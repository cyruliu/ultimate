/*
 * Copyright (C) 2019 Nico Hauff (hauffn@informatik.uni-freiburg.de)
 * Copyright (C) 2019 Elisabeth Henkel (henkele@informatik.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE MSO Library package.
 *
 * The ULTIMATE MSO Library package library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE MSO Library package library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE MSO Library package. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE MSO Library package, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE MSO Library package library grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.mso;

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.buchi.BuchiComplementFKV;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.buchi.BuchiIntersect;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * This class provides methods to manipulate büchi automata used to describe MSOD-Formulas.
 *
 * @author Elisabeth Henkel (henkele@informatik.uni-freiburg.de)
 * @author Nico Hauff (hauffn@informatik.uni-freiburg.de)
 */
public class MSODAutomataOperationsBuchi extends MSODAutomataOperations {

	/**
	 * @throws AutomataLibraryException
	 *             if construction of {@link BuchiComplementFKV} fails.
	 */
	@Override
	public INestedWordAutomaton<MSODAlphabetSymbol, String> complement(final AutomataLibraryServices services,
			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton) throws AutomataLibraryException {

		INestedWordAutomaton<MSODAlphabetSymbol, String> result =
				new BuchiComplementFKV<>(services, new MSODStringFactory(), automaton).getResult();

		result = minimize(services, result);

		if (result.getAlphabet().isEmpty()) {
			return result;
		}

		// Find all Int variables contained in the alphabet.
		final Set<Term> intVars = result.getAlphabet().iterator().next().containsSort(SmtSortUtils.INT_SORT);

		// Intersect with an automaton that ensures that each Int variable is matched to exactly one value.
		for (final Term intVar : intVars) {
			INestedWordAutomaton<MSODAlphabetSymbol, String> varAutomaton;
			varAutomaton = intVariableAutomaton(services, intVar);
			varAutomaton = reduceOrExtend(services, varAutomaton, result.getAlphabet(), true);

			result = intersect(services, result, varAutomaton);
		}

		return result;
	}

	/**
	 * Returns an {@link INestedWordAutomaton} that represents the intersection of the two given automata.
	 *
	 * @throws AutomataLibraryException
	 *             if construction of {@link BuchiIntersect} fails
	 */
	@Override
	public INestedWordAutomaton<MSODAlphabetSymbol, String> intersect(final AutomataLibraryServices services,
			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton1,
			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton2) throws AutomataLibraryException {

		INestedWordAutomaton<MSODAlphabetSymbol, String> result =
				new BuchiIntersect<>(services, new MSODStringFactory(), automaton1, automaton2).getResult();

		result = minimize(services, result);

		return result;
	}
}
