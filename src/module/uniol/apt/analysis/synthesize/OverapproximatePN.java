/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2016  Uli Schlachter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.apt.analysis.synthesize;

import static uniol.apt.util.DebugUtil.debug;
import static uniol.apt.util.DebugUtil.debugFormat;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.exception.ArcExistsException;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.Event;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

/**
 * Calculate the minimal Petri Net over-approximation of a transition system.
 * @author Uli Schlachter
 */
public class OverapproximatePN {
	private OverapproximatePN() {
	}

	/**
	 * Calculate the minimal Petri Net over-approximation of a transition system.
	 * @param ts The transition system to over-approximate.
	 * @param properties The properties that the resulting Petri net should satisfy.
	 * @return A Petri net being a minimal over-approximation of the input.
	 * @throws MissingLocationException If some states of the input have a location and others do not.
	 */
	static public PetriNet overapproximate(TransitionSystem ts, PNProperties properties)
			throws MissingLocationException {
		// Make a copy which we can modify
		ts = new TransitionSystem(ts);
		SynthesizePN synthesize = null;
		int iterations = 0;

		do {
			iterations++;
			debug();
			debugFormat("Beginning iteration %d", iterations);

			SynthesizePN.Builder builder = SynthesizePN.Builder
				.createForIsomorphicBehaviour(ts)
				.setProperties(properties);

			// Add already-calculated regions so that they do not have to be calculated again
			if (synthesize != null)
				copyExistingRegions(builder, synthesize);

			synthesize = builder.build();
			ts = handleSeparationFailures(ts, synthesize.getFailedStateSeparationProblems(),
					synthesize.getFailedEventStateSeparationProblems());
		} while (!synthesize.wasSuccessfullySeparated());

		return synthesize.synthesizePetriNet();
	}

	static private void copyExistingRegions(SynthesizePN.Builder builder, SynthesizePN synthesize) {
		RegionUtility utility = builder.getRegionUtility();
		for (Region region : synthesize.getSeparatingRegions()) {
			Region newRegion = Region.Builder.copyRegionToUtility(utility, region);
			// The only difference should be in the used RegionUtility, which should not influence
			// the result of toString().
			assert region.toString().equals(newRegion.toString())
				: region.toString() + " = " + newRegion.toString();
			try {
				builder.addRegion(newRegion);
			} catch (InvalidRegionException e) {
				// This cannot happen; it was a region previously and we only changed
				// things in a way that this should still be a region.
				throw new AssertionError(e);
			}
		}
	}

	/**
	 * Create a new transition system from the given system that will not have the given separation failures.
	 * @param ts The transition system whose synthesis was attempted.
	 * @param failedSSP A collection of sets of unseparable states.
	 * @param failedESSP A description of event/state separation failures.
	 * @return A transition system without the given separation failures.
	 */
	static public TransitionSystem handleSeparationFailures(TransitionSystem ts, Collection<Set<State>> failedSSP,
			Map<String, Set<State>> failedESSP) {
		debugFormat("Creating new TS to handle SSP failures %s and ESSP failures %s", failedSSP, failedESSP);
		TransitionSystem result = ts;
		Map<State, State> oldToNewStateMap = null;

		if (!failedSSP.isEmpty()) {
			result = new TransitionSystem();
			oldToNewStateMap = new HashMap<>();

			// First mark the states that we have to modify as "reserved"
			for (Set<State> equivalenceClass : failedSSP)
				for (State state : equivalenceClass)
					oldToNewStateMap.put(state, null);

			// Then copy over all states which we do not modify
			for (State state : ts.getNodes()) {
				if (oldToNewStateMap.containsKey(state))
					continue;
				State newState = result.createState(state);
				oldToNewStateMap.put(state, newState);
			}

			// Then handle states which have to be merged
			for (Set<State> equivalenceClass : failedSSP) {
				State newState = result.createState();
				for (State state : equivalenceClass)
					oldToNewStateMap.put(state, newState);
			}

			result.setInitialState(oldToNewStateMap.get(ts.getInitialState()));

			// Now that all states are created, handle arcs
			for (Arc arc : ts.getEdges()) {
				State source = oldToNewStateMap.get(arc.getSource());
				State target = oldToNewStateMap.get(arc.getTarget());
				String label = arc.getLabel();
				try {
					result.createArc(source, target, label);
				} catch (ArcExistsException e) {
					// This can happen when the state-mapping merges two states together which both
					// have an outgoing edge with the same label.
				}
			}

			// Finally, copy locations (and other extensions on events)
			for (Event event : result.getAlphabetEvents())
				event.copyExtensions(ts.getEvent(event.getLabel()));
		}

		if (!failedESSP.isEmpty()) {
			// For each failed ESSP instance (s,e), add an arc (s,e,s') where s' is a newly created state
			for (Map.Entry<String, Set<State>> entry : failedESSP.entrySet()) {
				String label = entry.getKey();
				for (State state : entry.getValue()) {
					State stateToModify;
					if (oldToNewStateMap == null)
						stateToModify = state;
					else
						stateToModify = oldToNewStateMap.get(state);
					if (stateToModify.getPostsetNodesByLabel(label).isEmpty())
						result.createArc(stateToModify, result.createState(), label);
					else
						// This can happen if the state also had an SSP failure and was merged
						// with some state which already has this event enabled.
						assert !failedSSP.isEmpty();
				}
			}
		}

		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120