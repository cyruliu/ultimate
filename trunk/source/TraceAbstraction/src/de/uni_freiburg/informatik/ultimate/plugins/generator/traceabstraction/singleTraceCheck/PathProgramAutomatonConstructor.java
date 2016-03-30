/*
 * Copyright (C) 2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 * 
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.TermVarsProc;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PredicateFactoryForInterpolantAutomata;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;


/**
 * @author musab@informatik.uni-freiburg.de
 *
 */
public class PathProgramAutomatonConstructor {
	
	/**
	 *  A mapping from positions of given path to states of the constructed automaton.
	 */
	private List<IPredicate> m_PositionsToStates;

	
	public List<IPredicate> getPositionsToStates() {
		return m_PositionsToStates;
	}
	
	/**
	 * Construct a path automaton (finite) from the given path.
	 */
	public INestedWordAutomaton<CodeBlock, IPredicate> constructAutomatonFromGivenPath(NestedWord<CodeBlock> path, IUltimateServiceProvider services, SmtManager smtManager,
			TAPreferences taPrefs) {
		// Set the alphabet
		Set<CodeBlock> internalAlphabet = new HashSet<CodeBlock>();
		Set<CodeBlock> callAlphabet = new HashSet<CodeBlock>();
		Set<CodeBlock> returnAlphabet = new HashSet<CodeBlock>();
		for (int i = 0; i < path.length(); i++) {
			if (path.isInternalPosition(i)) {
				internalAlphabet.add(path.getSymbol(i));
			} else if (path.isCallPosition(i)) {
				callAlphabet.add(path.getSymbol(i));
			} else if (path.isReturnPosition(i)) {
				returnAlphabet.add(path.getSymbol(i));
			} else {
				throw new UnsupportedOperationException("Symbol at position " + i + " is neither internal, call, nor return symbol!");
			}
		}
		
		StateFactory<IPredicate> predicateFactory = new PredicateFactoryForInterpolantAutomata(smtManager, taPrefs);
		// Create the automaton
		NestedWordAutomaton<CodeBlock, IPredicate> pathPA = new NestedWordAutomaton<CodeBlock, IPredicate>(new AutomataLibraryServices(services), internalAlphabet, callAlphabet, returnAlphabet, predicateFactory);
		
		// We need this list to create the transitions of the automaton.
		m_PositionsToStates = new ArrayList<IPredicate>(path.length() + 1);
		
		ProgramPoint[] arrOfProgPoints = new ProgramPoint[path.length()];
		// We use this map to keep track of the predicates we've created so far. We don't want to create a new predicate
		// for the same program point, therefore we use the map to get the predicate we've constructed before.
		Map<ProgramPoint, SPredicate> programPointToState = new HashMap<>();
		
		ProgramPoint tempProgramPoint = null;
		tempProgramPoint = (ProgramPoint) path.getSymbol(0).getSource();
		
		// Add the initial state
		TermVarsProc trueTvp = smtManager.getPredicateFactory().constructTrue();
		SPredicate initialState = smtManager.getPredicateFactory().newSPredicate(tempProgramPoint, trueTvp);
		pathPA.addState(true, false, initialState);
		programPointToState.put(tempProgramPoint, initialState);
		m_PositionsToStates.add(0, initialState);
		
		// Add the other states
		for (int i = 0; i < path.length(); i++) {
			tempProgramPoint = (ProgramPoint) path.getSymbol(i).getTarget();
			if (!programPointToState.containsKey(tempProgramPoint)) {
				SPredicate newState = smtManager.getPredicateFactory().newSPredicate(tempProgramPoint, trueTvp);
				programPointToState.put(tempProgramPoint, newState);
				arrOfProgPoints[i] = (ProgramPoint) path.getSymbol(i).getTarget();
				if (i + 1 == path.length()) {
					// this is the last (accepting) state (the error location)
					pathPA.addState(false, true, newState);
				} else {
					pathPA.addState(false, false, newState);
				}
			}
			m_PositionsToStates.add(i+1, programPointToState.get(tempProgramPoint));
		}
		
		// Add the transitions
		for (int i = 0; i < path.length(); i++) {
			IPredicate pred = m_PositionsToStates.get(i);
			IPredicate succ = m_PositionsToStates.get(i+1);
			if (path.isInternalPosition(i)) {
				pathPA.addInternalTransition(pred, path.getSymbol(i), succ);
			} else if (path.isCallPosition(i)) {
				pathPA.addCallTransition(pred, path.getSymbol(i), succ);
			} else if (path.isReturnPosition(i)) {
				IPredicate hier = m_PositionsToStates.get(path.getCallPosition(i));
				pathPA.addReturnTransition(pred, hier, path.getSymbol(i), succ);
			}
		}
		return pathPA;
	}

}
