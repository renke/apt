/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2014  Uli Schlachter
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uniol.apt.TestNetCollection;
import uniol.apt.TestTSCollection;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.coverability.CoverabilityGraph;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.analysis.isomorphism.IsomorphismLogic;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uniol.apt.adt.matcher.Matchers.flowThatConnects;
import static uniol.apt.adt.matcher.Matchers.nodeWithID;
import static uniol.apt.analysis.synthesize.Matchers.*;

/** @author Uli Schlachter */
@Test
@SuppressWarnings("unchecked") // I hate generics
public class SynthesizePNTest {
	static private Region mockRegion(RegionUtility utility, int normalMarking,
			List<Integer> backwardWeights, List<Integer> forwardWeights) {
		assert backwardWeights.size() == forwardWeights.size();

		Region result = mock(Region.class);
		when(result.getRegionUtility()).thenReturn(utility);
		when(result.getNormalRegionMarking()).thenReturn(normalMarking);

		List<String> eventList = utility.getEventList();
		for (int i = 0; i < backwardWeights.size(); i++) {
			when(result.getBackwardWeight(i)).thenReturn(backwardWeights.get(i));
			when(result.getBackwardWeight(eventList.get(i))).thenReturn(backwardWeights.get(i));
			when(result.getForwardWeight(i)).thenReturn(forwardWeights.get(i));
			when(result.getForwardWeight(eventList.get(i))).thenReturn(forwardWeights.get(i));
		}

		return result;
	}

	@Test
	public void testSynthesizePetriNetEmpty() {
		TransitionSystem ts = TestTSCollection.getSingleStateTS();
		RegionUtility utility = new RegionUtility(ts);
		PetriNet pn = SynthesizePN.synthesizePetriNet(utility, Collections.<Region>emptySet());

		assertThat(pn.getPlaces(), is(empty()));
		assertThat(pn.getTransitions(), is(empty()));
		assertThat(pn.getEdges(), is(empty()));
	}

	@Test
	public void testSynthesizeSimplePetriNet() {
		List<String> eventList = Arrays.asList("a", "b");
		RegionUtility utility = mock(RegionUtility.class);
		when(utility.getEventList()).thenReturn(eventList);

		Set<Region> regions = new HashSet<>();
		regions.add(mockRegion(utility, 1, Arrays.asList(1, 1), Arrays.asList(0, 0)));

		PetriNet pn = SynthesizePN.synthesizePetriNet(utility, regions);

		assertThat(pn.getPlaces(), hasSize(1));
		assertThat(pn.getTransitions(), containsInAnyOrder(nodeWithID("a"), nodeWithID("b")));
		assertThat(pn.getEdges(), containsInAnyOrder(
					flowThatConnects(anything(), nodeWithID("a")),
					flowThatConnects(anything(), nodeWithID("b"))));
	}

	@Test
	public void testSingleStateTSWithLoop() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getSingleStateTSWithLoop();
		RegionUtility utility = new RegionUtility(ts);
		PetriNet pn = SynthesizePN.synthesizePetriNet(utility, Collections.<Region>emptySet());

		assertThat(pn.getPlaces(), is(empty()));
		assertThat(pn.getTransitions(), contains(nodeWithID("a")));
		assertThat(pn.getEdges(), is(empty()));
	}

	@Test
	public void testNonDeterministicTS() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getNonDeterministicTS();
		SynthesizePN synth = new SynthesizePN(ts);

		assertThat(synth.wasSuccessfullySeparated(), is(false));
		assertThat(synth.getSeparatingRegions(), contains(
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("a", -1))));
		assertThat(synth.getFailedStateSeparationProblems(),
				contains(containsInAnyOrder(nodeWithID("s1"), nodeWithID("s2"))));
		assertThat(synth.getFailedEventStateSeparationProblems().entrySet(), empty());
	}

	@Test
	public void testNonDeterministicTSNoSSP() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getNonDeterministicTS();
		SynthesizePN synth = new SynthesizePN(new RegionUtility(ts), new PNProperties(), true);

		assertThat(synth.wasSuccessfullySeparated(), is(true));
		assertThat(synth.getSeparatingRegions(), contains(
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("a", -1))));
		assertThat(synth.getFailedStateSeparationProblems(), empty());
		assertThat(synth.getFailedEventStateSeparationProblems().entrySet(), empty());
	}

	@Test
	public void testACBCCLoopTS() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getACBCCLoopTS();
		SynthesizePN synth = new SynthesizePN(new RegionUtility(ts), new PNProperties(PNProperties.OUTPUT_NONBRANCHING));

		assertThat(synth.wasSuccessfullySeparated(), is(true));
		// We know that there is a solution with three regions. Test that this really found an ON feasible set.
		assertThat(synth.getSeparatingRegions(), containsInAnyOrder(
					allOf(pureRegionWithWeightThat("b", greaterThanOrEqualTo(0)), pureRegionWithWeightThat("c", greaterThanOrEqualTo(0))),
					allOf(pureRegionWithWeightThat("a", greaterThanOrEqualTo(0)), pureRegionWithWeightThat("c", greaterThanOrEqualTo(0))),
					allOf(pureRegionWithWeightThat("a", greaterThanOrEqualTo(0)), pureRegionWithWeightThat("b", greaterThanOrEqualTo(0)))));
		assertThat(synth.getFailedStateSeparationProblems(), empty());
		assertThat(synth.getFailedEventStateSeparationProblems().entrySet(), empty());
	}

	@Test
	public void testPathTSPure() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getPathTS();
		RegionUtility utility = new RegionUtility(ts);
		SynthesizePN synth = new SynthesizePN(utility, new PNProperties(PNProperties.PURE));

		assertThat(synth.wasSuccessfullySeparated(), is(false));
		// Can't really be more specific, way too many possibilities
		assertThat(synth.getSeparatingRegions(), not(empty()));
		assertThat(synth.getFailedStateSeparationProblems(),
				contains(containsInAnyOrder(nodeWithID("t"), nodeWithID("u"))));
		assertThat(synth.getFailedEventStateSeparationProblems().toString(),
				synth.getFailedEventStateSeparationProblems().size(), is(2));
		assertThat(synth.getFailedEventStateSeparationProblems(), allOf(
					hasEntry(is("b"), containsInAnyOrder(ts.getNode("v"), ts.getNode("u"), ts.getNode("s"))),
					hasEntry(is("c"), contains(ts.getNode("t")))));
	}

	@Test
	public void testPathTSImpure() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getPathTS();
		RegionUtility utility = new RegionUtility(ts);
		SynthesizePN synth = new SynthesizePN(utility);

		assertThat(synth.wasSuccessfullySeparated(), is(false));
		// Can't really be more specific, way too many possibilities
		assertThat(synth.getSeparatingRegions(), not(empty()));
		assertThat(synth.getFailedStateSeparationProblems(),
				contains(containsInAnyOrder(nodeWithID("t"), nodeWithID("u"))));
		assertThat(synth.getFailedEventStateSeparationProblems().toString(),
				synth.getFailedEventStateSeparationProblems().size(), is(2));
		assertThat(synth.getFailedEventStateSeparationProblems(), allOf(
					hasEntry(is("b"), contains(ts.getNode("u"))),
					hasEntry(is("c"), contains(ts.getNode("t")))));
	}

	@Test
	public void testPureSynthesizablePathTS() throws MissingLocationException, UnboundedException {
		TransitionSystem ts = TestTSCollection.getPureSynthesizablePathTS();
		RegionUtility utility = new RegionUtility(ts);
		SynthesizePN synth = new SynthesizePN(utility);

		assertThat(synth.wasSuccessfullySeparated(), is(true));
		// Can't really be more specific, way too many possibilities
		assertThat(synth.getSeparatingRegions(), hasSize(greaterThanOrEqualTo(3)));
		assertThat(synth.getFailedStateSeparationProblems(), empty());
		assertThat(synth.getFailedEventStateSeparationProblems().entrySet(), empty());

		TransitionSystem ts2 = CoverabilityGraph.get(synth.synthesizePetriNet()).toReachabilityLTS();
		assertThat(new IsomorphismLogic(ts, ts2, true).isIsomorphic(), is(true));
	}

	@Test
	public void testStateSeparationSatisfiesProperties() throws MissingLocationException {
		// This transition system leads to a region basis with a:1 and b:2. This region is not plain, but the
		// code would add it for state separation anyway! Since this is the only entry in the basis and all
		// regions must be a linear combination of the basis, there are no plain regions at all for this TS.
		TransitionSystem ts = TestTSCollection.getTwoBThreeATS();
		State s = ts.getNode("s");
		State t = ts.getNode("t");
		State u = ts.getNode("u");
		State v = ts.getNode("v");
		SynthesizePN synth = new SynthesizePN(ts, new PNProperties(PNProperties.PLAIN));

		assertThat(synth.getSeparatingRegions(), everyItem(plainRegion()));
		assertThat(synth.getFailedEventStateSeparationProblems().toString(),
				synth.getFailedEventStateSeparationProblems().size(), is(2));
		assertThat(synth.getFailedEventStateSeparationProblems(), allOf(
					hasEntry(is("a"), contains(v)),
					hasEntry(is("b"), containsInAnyOrder(u, v))));
		assertThat(synth.getFailedStateSeparationProblems(), contains(containsInAnyOrder(s, t, u, v)));
	}

	@Test
	public void testWordB2AB5AB6AB6None() throws MissingLocationException, UnboundedException {
		TransitionSystem ts = SynthesizeWordModule.makeTS(Arrays.asList("b", "b", "a", "b", "b", "b", "b", "b",
					"a", "b", "b", "b", "b", "b", "b", "a", "b", "b", "b", "b", "b", "b"));
		SynthesizePN synth = new SynthesizePN(ts, new PNProperties());

		assertThat(synth.wasSuccessfullySeparated(), is(true));

		// Bypass the assertions in synthesizePetriNet() which already check for isomorphism
		PetriNet pn = SynthesizePN.synthesizePetriNet(synth.getUtility(), synth.getSeparatingRegions());
		TransitionSystem ts2 = CoverabilityGraph.get(pn).toReachabilityLTS();

		assertThat(new IsomorphismLogic(ts, ts2, true).isIsomorphic(), is(true));
	}

	@Test
	public void testWordB2AB5AB6AB6Pure() throws MissingLocationException, UnboundedException {
		TransitionSystem ts = SynthesizeWordModule.makeTS(Arrays.asList("b", "b", "a", "b", "b", "b", "b", "b",
					"a", "b", "b", "b", "b", "b", "b", "a", "b", "b", "b", "b", "b", "b"));
		SynthesizePN synth = new SynthesizePN(ts, new PNProperties(PNProperties.PURE));

		assertThat(synth.wasSuccessfullySeparated(), is(true));

		// Bypass the assertions in synthesizePetriNet() which already check for isomorphism
		PetriNet pn = SynthesizePN.synthesizePetriNet(synth.getUtility(), synth.getSeparatingRegions());
		TransitionSystem ts2 = CoverabilityGraph.get(pn).toReachabilityLTS();

		assertThat(new IsomorphismLogic(ts, ts2, true).isIsomorphic(), is(true));
	}

	@Test
	public void testABandB() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getABandB();
		SynthesizePN synth = new SynthesizePN(ts);

		assertThat(synth.wasSuccessfullySeparated(), is(false));
		assertThat(synth.getSeparatingRegions(), contains(
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("b", -1), impureRegionWithWeight("a", 1, 1))));
		assertThat(synth.getFailedStateSeparationProblems(),
				contains(containsInAnyOrder(nodeWithID("s"), nodeWithID("t"))));
		assertThat(synth.getFailedEventStateSeparationProblems().toString(),
				synth.getFailedEventStateSeparationProblems().size(), is(1));
		assertThat(synth.getFailedEventStateSeparationProblems(),
				hasEntry(equalTo("a"), contains(nodeWithID("t"))));
	}

	@Test
	public void testABandBNoSSP() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getABandB();
		SynthesizePN synth = new SynthesizePN(new RegionUtility(ts), new PNProperties(), true);

		assertThat(synth.wasSuccessfullySeparated(), is(false));
		assertThat(synth.getSeparatingRegions(), contains(
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("b", -1), impureRegionWithWeight("a", 1, 1))));
		assertThat(synth.getFailedStateSeparationProblems(), empty());
		assertThat(synth.getFailedEventStateSeparationProblems().toString(),
				synth.getFailedEventStateSeparationProblems().size(), is(1));
		assertThat(synth.getFailedEventStateSeparationProblems(),
				hasEntry(equalTo("a"), contains(nodeWithID("t"))));
	}

	@Test
	public void testABandBUnfolded() throws MissingLocationException {
		TransitionSystem ts = TestTSCollection.getABandBUnfolded();
		SynthesizePN synth = new SynthesizePN(ts);

		assertThat(synth.wasSuccessfullySeparated(), is(true));
		assertThat(synth.getSeparatingRegions(), containsInAnyOrder(
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("b", -1), impureRegionWithWeight("a", 1, 1)),
					allOf(regionWithInitialMarking(1), pureRegionWithWeight("b", 0), pureRegionWithWeight("a", -1))));
		assertThat(synth.getFailedStateSeparationProblems(), empty());
		assertThat(synth.getFailedEventStateSeparationProblems().entrySet(), empty());
	}

	@Test
	static public class MinimizeRegions {
		private TransitionSystem ts;
		private RegionUtility utility;

		@BeforeClass
		public void setup() {
			ts = SynthesizeWordModule.makeTS(Arrays.asList("a", "b"));
			// Add an unreachable state, just because we can
			ts.createState();
			utility = new RegionUtility(ts);
		}

		@Test
		public void testEmpty() {
			Set<Region> regions = new HashSet<>();

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, empty());
		}

		@Test
		public void testEmptyESSP() {
			Set<Region> regions = new HashSet<>();

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, empty());
		}

		@Test
		public void testSingleRegion() {
			Region region = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0));
			Set<Region> regions = new HashSet<>(Arrays.asList(region));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, containsInAnyOrder(region));
		}

		@Test
		public void testSingleRegionESSP() {
			Region region = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0));
			Set<Region> regions = new HashSet<>(Arrays.asList(region));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, containsInAnyOrder(region));
		}

		@Test
		public void testUselessRegion() {
			Region region = new Region(utility, Arrays.asList(1, 1), Arrays.asList(1, 1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, empty());
		}

		@Test
		public void testUselessRegionESSP() {
			Region region = new Region(utility, Arrays.asList(1, 1), Arrays.asList(1, 1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, empty());
		}

		@Test
		public void testNoUselessRegion() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, containsInAnyOrder(region1, region2));
		}

		@Test
		public void testNoUselessRegionESSP() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, containsInAnyOrder(region1, region2));
		}

		@Test
		public void testUselessRegionForSSP() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Region region3 = Region.createPureRegionFromVector(utility, Arrays.asList(1, 1));
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2, region3));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, containsInAnyOrder(region1, region2));
		}

		@Test
		public void testUselessRegionForSSPESSP() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Region region3 = Region.createPureRegionFromVector(utility, Arrays.asList(1, 1));
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2, region3));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, containsInAnyOrder(region1, region2));
		}

		@Test
		public void testDuplicateRegion() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(-2, 0)).withInitialMarking(2);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, anyOf(contains(region1), contains(region2)));
		}

		@Test
		public void testDuplicateRegionESSP() {
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, 0)).withInitialMarking(1);
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(-2, 0)).withInitialMarking(2);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, anyOf(contains(region1), contains(region2)));
		}

		@Test
		public void testLessUsefulRegion() {
			// There are three SSP instances and two ESSP instances. This region solves all of them except
			// for one ESSP instance (disabling a after the first a).
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, -1)).withInitialMarking(2);
			// This region solves only two SSP and one ESSP instance (less than the above and the above
			// solves all these problems, too)
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, false);
			assertThat(regions, contains(region1));
		}

		@Test
		public void testLessUsefulRegionESSP() {
			// There are three SSP instances and two ESSP instances. This region solves all of them except
			// for one ESSP instance (disabling a after the first a).
			Region region1 = Region.createPureRegionFromVector(utility, Arrays.asList(-1, -1)).withInitialMarking(2);
			// This region solves only two SSP and one ESSP instance (less than the above and the above
			// solves all these problems, too)
			Region region2 = Region.createPureRegionFromVector(utility, Arrays.asList(0, -1)).withInitialMarking(1);
			Set<Region> regions = new HashSet<>(Arrays.asList(region1, region2));

			SynthesizePN.minimizeRegions(utility, regions, true);
			assertThat(regions, contains(region1));
		}
	}

	@Test
	static public class DistributedImplementation {
		private TransitionSystem ts;
		private RegionUtility utility;

		@BeforeClass
		private void setup() {
			ts = TestTSCollection.getPersistentTS();
			ts.getArc("s0", "l", "a").putExtension("location", "a");
			ts.getArc("s0", "r", "b").putExtension("location", "b");
			ts.getArc("l", "s1", "b").putExtension("location", "b");
			ts.getArc("r", "s1", "a").putExtension("location", "a");

			utility = new RegionUtility(ts);
		}

		private PetriNet setupPN(PetriNet pn) {
			pn.getTransition("t1").setLabel("a");
			pn.getTransition("t2").setLabel("b");
			return pn;
		}

		@Test
		public void testConcurrentDiamond() {
			PetriNet pn = setupPN(TestNetCollection.getConcurrentDiamondNet());

			assertThat(SynthesizePN.isDistributedImplementation(utility, new PNProperties(), pn), is(true));
		}

		@Test
		public void testConcurrentDiamondWithCommonPostset() {
			PetriNet pn = setupPN(TestNetCollection.getConcurrentDiamondNet());

			// Create a new place which has both transitions in its post-set. This tests that the
			// implementation really checks the places' pre-set and ignores their post-sets.
			pn.createPlace("post");
			pn.createFlow("t1", "post");
			pn.createFlow("t2", "post");

			assertThat(SynthesizePN.isDistributedImplementation(utility, new PNProperties(), pn), is(true));
		}

		@Test
		public void testConflictingDiamond() {
			PetriNet pn = setupPN(TestNetCollection.getConflictingDiamondNet());

			assertThat(SynthesizePN.isDistributedImplementation(utility, new PNProperties(), pn), is(false));
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
