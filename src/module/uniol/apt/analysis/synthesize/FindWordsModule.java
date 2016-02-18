/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2014-2016  Uli Schlachter
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import uniol.apt.module.AbstractModule;
import uniol.apt.module.AptModule;
import uniol.apt.module.Category;
import uniol.apt.module.Module;
import uniol.apt.module.ModuleInput;
import uniol.apt.module.ModuleInputSpec;
import uniol.apt.module.ModuleOutput;
import uniol.apt.module.ModuleOutputSpec;
import uniol.apt.module.exception.ModuleException;
import uniol.apt.analysis.synthesize.SynthesizeUtils;

/**
 * Find all words that can be generated by a Petri net from a given class.
 * @author Uli Schlachter
 */
@AptModule
public class FindWordsModule extends AbstractModule implements Module {
	static private enum Operation {
		UNSOLVABLE(true, true, false),
		SOLVABLE(true, false, true),
		QUIET(false, false, false);

		private final boolean status;
		private final boolean unsolvable;
		private final boolean solvable;

		private Operation(boolean status, boolean unsolvable, boolean solvable) {
			this.status = status;
			this.unsolvable = unsolvable;
			this.solvable = solvable;
		}

		private boolean printStatus() {
			return status;
		}

		private boolean printUnsolvable() {
			return unsolvable;
		}

		private boolean printSolvable() {
			return solvable;
		}
	}

	@Override
	public String getShortDescription() {
		return "Print either minimal unsolvable or all solvable words of some class";
	}

	@Override
	public String getLongDescription() {
		return getShortDescription() + ".\n\n"
			+ "This module only prints a subset of all words. For this, an equivalence relation on words "
			+ "is used were two words are equivalent if one can be created from the other by replacing "
			+ "letters with other letters. For example, 'abc' and 'abd' are equivalent in this sense, "
			+ "since c->d turns one into the other.\n"
			+ "More concretely, words are generated so that the last letter of the word is the first "
			+ "letter of the alphabet. Then, the next new letter from the end is the second letter of the "
			+ "alphabet, and so on.\n"
			+ "\nExample calls:\n\n"
			+ " apt " + getName() + " safe solvable abc: Print all words solvable by safe Petri nets over "
			+ "the alphabet {a,b,c}\n"
			+ " apt " + getName() + " none unsolvable ab: Print all minimally unsolvable words over the "
			+ "alphabet {a,b}\n";
	}

	@Override
	public String getName() {
		return "find_words";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("options", String.class, "options");
		inputSpec.addParameter("operation", String.class,
				"Choose between printing all 'minimal_unsolvable' words or all 'solvable' words");
		inputSpec.addParameter("alphabet", String.class, "Letters that should be part of the alphabet");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		// This module prints to System.out.
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		String optionsStr = input.getParameter("options", String.class);
		String alphabetLetter = input.getParameter("alphabet", String.class);
		String operation = input.getParameter("operation", String.class);

		PNProperties properties = SynthesizeModule.Options.parseProperties(optionsStr).properties;
		SortedSet<Character> alphabet = new TreeSet<>(FindWords.toList(alphabetLetter));

		switch (operation) {
			case "minimal_unsolvable":
				generateList(properties, alphabet, Operation.UNSOLVABLE);
				break;
			case "solvable":
				generateList(properties, alphabet, Operation.SOLVABLE);
				break;
			default:
				throw new ModuleException("Unknown operation '" + operation
						+ "', valid options are 'minimal_unsolvable' and 'solvable'");
		}
	}

	static private void generateList(PNProperties properties, SortedSet<Character> alphabet, Operation operation) {
		final boolean printSolvable = operation.printSolvable();
		final boolean printUnsolvable = operation.printUnsolvable();
		if (operation.printStatus()) {
			String print;
			if (printSolvable && printUnsolvable)
				print = "solvable and minimal unsolvable";
			else if (printSolvable)
				print = "solvable";
			else if (printUnsolvable)
				print = "minimal unsolvable";
			else
				print = "no";
			System.out.println("Looking for " + print + " words from class " + properties.toString()
					+ " over the alphabet " + alphabet);
		}

		final int[] counters = new int[2];
		// Indices into the above array
		final int solvable = 0;
		final int unsolvable = 1;
		FindWords.WordCallback wordCallback = new FindWords.WordCallback() {
			@Override
			public void call(List<Character> wordAsList, String wordAsString, SynthesizePN synthesize) {
				if (synthesize.wasSuccessfullySeparated()) {
					counters[solvable]++;
					if (printSolvable)
						System.out.println(wordAsString);
				} else {
					counters[unsolvable]++;
					if (printUnsolvable)
						System.out.println(SynthesizeUtils.formatESSPFailure(
									FindWords.toStringList(wordAsList),
									synthesize.getFailedEventStateSeparationProblems(),
									true));
				}
			}
		};
		FindWords.LengthDoneCallback lengthDoneCallback = new FindWords.LengthDoneCallback() {
			@Override
			public void call(int length) {
				System.out.println("Done with length " + length + ". There were " + counters[unsolvable]
						+ " unsolvable words and " + counters[solvable] + " solvable words.");
				counters[solvable] = 0;
				counters[unsolvable] = 0;
			}
		};
		FindWords.generateList(properties, alphabet, !printUnsolvable, wordCallback, lengthDoneCallback);
	}

	@Override
	public Category[] getCategories() {
		return new Category[]{Category.LTS};
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
