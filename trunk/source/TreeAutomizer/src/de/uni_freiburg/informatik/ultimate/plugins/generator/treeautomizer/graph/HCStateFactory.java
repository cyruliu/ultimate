/*
 * Copyright (C) 2016 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2016 Mostafa M.A. (mostafa.amin93@gmail.com)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE TreeAutomizer Plugin.
 *
 * The ULTIMATE TreeAutomizer Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TreeAutomizer Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TreeAutomizer Plugin. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TreeAutomizer Plugin, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TreeAutomizer Plugin grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.treeautomizer.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.statefactory.IIntersectionStateFactory;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.IMergeStateFactory;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.ISemanticReducerFactory;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.ISinkStateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.lib.chc.HcPredicateSymbol;
import de.uni_freiburg.informatik.ultimate.lib.chc.HornClause;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.simplification.SimplifyDDA;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker.Validity;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singletracecheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.util.CombinatoricsUtils;
import de.uni_freiburg.informatik.ultimate.util.scc.DefaultStronglyConnectedComponentFactory;
import de.uni_freiburg.informatik.ultimate.util.scc.SccComputation;
import de.uni_freiburg.informatik.ultimate.util.scc.SccComputation.ISuccessorProvider;
import de.uni_freiburg.informatik.ultimate.util.scc.StronglyConnectedComponent;

/**
 * HornClause state factory.
 *
 * @author Mostafa M.A. (mostafa.amin93@gmail.com)
 * @author Alexander Nutz (nutz@informatik.uni-freiburg.de)
 *
 */
public class HCStateFactory implements IMergeStateFactory<IPredicate>, IIntersectionStateFactory<IPredicate>,
		ISinkStateFactory<IPredicate>, ISemanticReducerFactory<IPredicate, HornClause> {

	private final HCPredicate mSinkState;

	private final ManagedScript mBackendSmtSolverScript;
	private final SimplifyDDA mSimplifier;

	private final HCPredicateFactory mPredicateFactory;
	private final PredicateUnifier mPredicateUnifier;

	private final HCHoareTripleChecker mHoareTripleChecker;

	private int mSer;

	private final ILogger mLogger;

	private final boolean mDummySemanticReduction;

	/***
	 * HornClause State factory constructor.
	 *
	 * @param backendSmtSolverScript
	 * @param predicateFactory
	 * @param predicateUnifier
	 * @param symbolTable
	 */
	public HCStateFactory(final ManagedScript backendSmtSolverScript, final HCPredicateFactory predicateFactory,
			final ILogger logger, final PredicateUnifier predicateUnifier, final HCHoareTripleChecker hoareChecker) {
		this(backendSmtSolverScript, predicateFactory, logger, predicateUnifier, hoareChecker, false);
	}

	/***
	 * HornClause State factory constructor.
	 *
	 * @param backendSmtSolverScript
	 * @param predicateFactory
	 * @param predicateUnifier
	 * @param symbolTable
	 */
	public HCStateFactory(final ManagedScript backendSmtSolverScript, final HCPredicateFactory predicateFactory,
			final ILogger logger, final PredicateUnifier predicateUnifier, final HCHoareTripleChecker hoareChecker,
			final boolean dummySemanticReduction) {
		mBackendSmtSolverScript = backendSmtSolverScript;

		mLogger = logger;
		mSinkState = predicateFactory.getDontCareLocationPredicate();
		mSimplifier = new SimplifyDDA(mBackendSmtSolverScript.getScript());
		mPredicateFactory = predicateFactory;
		mPredicateUnifier = predicateUnifier;
		mHoareTripleChecker = hoareChecker;
		mDummySemanticReduction = dummySemanticReduction;
		mSer = 0;
	}

	protected int constructFreshSerialNumber() {
		return ++mSer;
	}

	@Override
	public IPredicate createSinkStateContent() {
		return mSinkState;
	}

	@Override
	public IPredicate intersection(final IPredicate state1, final IPredicate state2) {
		final Set<HcPredicateSymbol> state1PredSymbols = new HashSet<>();
		state1PredSymbols.addAll(((HCPredicate) state1).getHcPredicateSymbols());

		final Term conjoinedFormula = mSimplifier.getSimplifiedTerm(
				SmtUtils.and(mBackendSmtSolverScript.getScript(), state1.getFormula(), state2.getFormula()));

		final Set<IPredicate> ps = new HashSet<>();
		ps.add(state1);
		ps.add(state2);

		return mPredicateFactory.newPredicate(state1PredSymbols, //constructFreshSerialNumber(),
				conjoinedFormula,
				Arrays.asList(state1.getFormula().getFreeVars()));
	}

	@Override
	public IPredicate merge(final Collection<IPredicate> states) {
		/*
		 * stricly speaking, we would have to have something like "multi-location-HCPredicate" in order to treat merging
		 * several HCPredicates (with different locations) correctly. (TODO) For now we just treat everything as a
		 * generic IPredicate..
		 */

		final Set<HcPredicateSymbol> mergedLocations = new HashSet<>();
		Term mergedFormula = mBackendSmtSolverScript.getScript().term("false");

		List<TermVariable> varsForHcPred = null;

		for (final IPredicate pred : states) {
			if (pred instanceof HCPredicate) {
				mergedLocations.addAll(((HCPredicate) pred).getHcPredicateSymbols());
				assert varsForHcPred == null || varsForHcPred.equals(((HCPredicate) pred).getSignature()) : "merging "
						+ "predicates with a different signature. Does that make sense??";
				varsForHcPred = ((HCPredicate) pred).getSignature();
			}
			mergedFormula = mSimplifier.getSimplifiedTerm(
					SmtUtils.or(mBackendSmtSolverScript.getScript(), mergedFormula, pred.getFormula()));
		}
		if (mergedLocations.isEmpty()) {
			return mPredicateFactory.newPredicate(mergedFormula);
		} else {
			return mPredicateFactory.newPredicate(mergedLocations, //constructFreshSerialNumber(),
					mergedFormula,
					varsForHcPred);
		}
	}

	@Override
	public IPredicate createEmptyStackState() {
		return createSinkStateContent();
	}

	private Term implicationStatement(final IPredicate predA, final IPredicate predB) {

		return SmtUtils.and(mBackendSmtSolverScript.getScript(), predA.getClosedFormula(),
				SmtUtils.not(mBackendSmtSolverScript.getScript(), predB.getClosedFormula()));

	}

	private boolean implies(final IPredicate predA, final IPredicate predB) {

		mBackendSmtSolverScript.lock(this);
		mBackendSmtSolverScript.push(this, 1);

		final Term statement = implicationStatement(predA, predB);

		mBackendSmtSolverScript.assertTerm(this, statement);
		final LBool res = mBackendSmtSolverScript.checkSat(this);

		mBackendSmtSolverScript.pop(this, 1);
		mBackendSmtSolverScript.unlock(this);
		return res == LBool.SAT;
	}

	private boolean implies2(final IPredicate predA, final IPredicate predB) {
		return mPredicateUnifier.getCoverageRelation().getCoveredPredicates(predB).contains(predA);
	}

	private Map<IPredicate, Set<IPredicate>> constructBaseGraph(final Iterable<IPredicate> states) {

		final IPredicate[] preds = CombinatoricsUtils.iterateAll(states.iterator()).toArray(new IPredicate[] {});
		final Map<IPredicate, Set<IPredicate>> implication = new HashMap<>();
		for (int i = 0; i < preds.length; ++i) {
			if (!implication.containsKey(preds[i])) {
				implication.put(preds[i], new HashSet<>());
			}
			for (int j = 0; j < preds.length; ++j) {
				if (i != j && implies(preds[i], preds[j])) {
					implication.get(preds[i]).add(preds[j]);
				}
			}
		}
		return implication;
	}

	public SccComputation<IPredicate, StronglyConnectedComponent<IPredicate>>
			getImplicationGraph(final Iterable<IPredicate> states) {
		return getImplicationGraph(constructBaseGraph(states));
	}

	public SccComputation<IPredicate, StronglyConnectedComponent<IPredicate>>
			getImplicationGraph(final Map<IPredicate, Set<IPredicate>> implication) {

		final ISuccessorProvider<IPredicate> successors = new ISuccessorProvider<IPredicate>() {
			@Override
			public Iterator<IPredicate> getSuccessors(final IPredicate node) {
				return implication.get(node).iterator();
			}
		};

		final SccComputation<IPredicate, StronglyConnectedComponent<IPredicate>> sccComputer =
				new SccComputation<>(mLogger, successors, new DefaultStronglyConnectedComponentFactory<>(),
						implication.size(), implication.keySet());

		return sccComputer;
	}

	public Iterable<IPredicate> filterUnconditonally(final Iterable<IPredicate> states) {
		final SccComputation<IPredicate, StronglyConnectedComponent<IPredicate>> sccComputer =
				getImplicationGraph(states);
		final Set<IPredicate> res = new HashSet<>();
		for (final StronglyConnectedComponent<IPredicate> leaf : sccComputer.getLeafComponents()) {
			res.add(leaf.getRootNode());
		}
		return res;
	}

	@Override
	public Iterable<IPredicate> filter(final Iterable<IPredicate> states) {
		if (mDummySemanticReduction) {
			return states;
		}
		return filterUnconditonally(states);
	}

	@Override
	public Iterable<IPredicate> getOptimalDestination(final Iterable<IPredicate> states, final List<IPredicate> src,
			final HornClause letter, final Iterable<IPredicate> dest) {

		final Set<IPredicate> potential = new HashSet<>();
		for (final IPredicate state : states) {
			// preCond ^ state ==> state
			if (mHoareTripleChecker.check(src, letter, state) == Validity.VALID) {
				potential.add(state);
			}
		}

		/*
		 * assert that all destination states are in the set of states for (final IPredicate state : dest) { // preCond
		 * ^ state ==> dest if (mHoareTripleChecker.check(src, letter, state) == Validity.VALID) { potential.add(state);
		 * } }
		 */

		return filterUnconditonally(potential);
	}

}