/*
 * Copyright (C) 2015-2016 Christian Schilling (schillic@informatik.uni-freiburg.de)
 * Copyright (C) 2015-2016 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automaton Delta Debugger.
 * 
 * The ULTIMATE Automaton Delta Debugger is free software: you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * The ULTIMATE Automaton Delta Debugger is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automaton Delta Debugger. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7: If you modify the
 * ULTIMATE Automaton Delta Debugger, or any covered work, by linking or
 * combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automaton Delta Debugger grant you additional
 * permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.shrinkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.automatondeltadebugger.utils.TypedTransition;

/**
 * Removes return transitions.
 * 
 * @author Christian Schilling (schillic@informatik.uni-freiburg.de)
 * @param <LETTER>
 *            letter type
 * @param <STATE>
 *            state type
 */
public class ReturnTransitionShrinker<LETTER, STATE>
		extends AbstractShrinker<TypedTransition<LETTER, STATE>, LETTER, STATE> {
	/**
	 * @param services
	 *            Ultimate services.
	 */
	public ReturnTransitionShrinker(final IUltimateServiceProvider services) {
		super(services);
	}

	@Override
	public INestedWordAutomaton<LETTER, STATE> createAutomaton(final List<TypedTransition<LETTER, STATE>> list) {
		// create fresh automaton
		final INestedWordAutomaton<LETTER, STATE> automaton = mFactory.create(mAutomaton);

		// add all states
		mFactory.addStates(automaton, mAutomaton.getStates());

		// add all internal transitions
		mFactory.addFilteredInternalTransitions(automaton, mAutomaton);

		// add all call transitions
		mFactory.addFilteredCallTransitions(automaton, mAutomaton);

		// add the complement of the passed return transitions
		final Set<TypedTransition<LETTER, STATE>> oldTransitions = mFactory.getReturnTransitions(mAutomaton);
		oldTransitions.removeAll(list);
		mFactory.addReturnTransitions(automaton, oldTransitions);

		return automaton;
	}

	@Override
	public List<TypedTransition<LETTER, STATE>> extractList() {
		return new ArrayList<>(mFactory.getReturnTransitions(mAutomaton));
	}
}
