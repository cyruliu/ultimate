/*
 * Copyright (C) 2020 Jonas Werner (wernerj@informatik.uni-freiburg.de)
 * Copyright (C) 2020 University of Freiburg
 *
 * This file is part of the ULTIMATE accelerated interpolation library .
 *
 * The ULTIMATE framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE accelerated interpolation library . If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE PDR library , or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE accelerated interpolation library grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution;
import de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation.loopaccelerator.Accelerator;
import de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation.loopaccelerator.Accelerator.AccelerationMethod;
import de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation.loopdetector.Loopdetector;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.interpolant.IInterpolatingTraceCheck;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.interpolant.InterpolantComputationStatus;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.interpolant.InterpolantComputationStatus.ItpErrorStatus;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicateUnifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.PredicateTransformer;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.TermDomainOperationProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.tracecheck.ITraceCheckPreferences;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.tracecheck.TraceCheckReasonUnknown;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;

/**
 *
 * @author Jonas Werner (wernerj@informatik.uni-freiburg.de)
 *
 * @param <LETTER>
 */
public class AcceleratedInterpolation<LETTER extends IIcfgTransition<?>> implements IInterpolatingTraceCheck<LETTER> {

	private final ILogger mLogger;
	private final ManagedScript mScript;
	private final IUltimateServiceProvider mServices;
	private final List<LETTER> mCounterexample;
	private final IPredicateUnifier mPredicateUnifier;
	private final PredicateTransformer<Term, IPredicate, TransFormula> mPredTransformer;
	private final ITraceCheckPreferences mPrefs;
	private final IIcfg<? extends IcfgLocation> mIcfg;
	private LBool mIsTraceCorrect;
	private IPredicate[] mInterpolants;
	private IProgramExecution<IIcfgTransition<IcfgLocation>, Term> mFeasibleProgramExecution;
	private final TraceCheckReasonUnknown mReasonUnknown;
	private final boolean mTraceCheckFinishedNormally;

	private final Map<IcfgLocation, Set<List<LETTER>>> mLoops;
	private final Map<IcfgLocation, Set<UnmodifiableTransFormula>> mAccelerations;
	private final Accelerator mAccelerator;
	private final Loopdetector<LETTER> mLoopdetector;

	public AcceleratedInterpolation(final ILogger logger, final ITraceCheckPreferences prefs,
			final ManagedScript script, final IPredicateUnifier predicateUnifier, final List<LETTER> counterexample) {
		mLogger = logger;
		mScript = script;
		mServices = prefs.getUltimateServices();
		mCounterexample = counterexample;
		mPredicateUnifier = predicateUnifier;
		mPrefs = prefs;
		mIcfg = mPrefs.getIcfgContainer();
		mAccelerations = new HashMap<>();
		mLogger.debug("Accelerated Interpolation engaged!");
		mInterpolants = new IPredicate[mCounterexample.size()];
		mInterpolants[0] = mPredicateUnifier.getTruePredicate();
		mInterpolants[mCounterexample.size() - 1] = mPredicateUnifier.getFalsePredicate();

		mAccelerator = new Accelerator<>(mLogger, mScript, mServices);
		mLoopdetector = new Loopdetector<>(mCounterexample, mLogger);
		mPredTransformer = new PredicateTransformer<>(mScript, new TermDomainOperationProvider(mServices, mScript));

		// TODO give a better reason
		mReasonUnknown = null;
		mTraceCheckFinishedNormally = true;

		mLoops = mLoopdetector.getLoops();
		if (mLoops.isEmpty()) {
			mLogger.debug("No loops found in this trace.");
			mIsTraceCorrect = checkFeasibility(mCounterexample);
			if (mIsTraceCorrect == LBool.UNSAT) {
				mInterpolants = generateInterpolants();
				return;
			}
		}
		mLogger.debug("Found loops, starting acceleration");
		for (final Entry<IcfgLocation, Set<List<LETTER>>> loophead : mLoops.entrySet()) {
			final Set<UnmodifiableTransFormula> accelerations = new HashSet<>();
			for (final List<LETTER> loop : loophead.getValue()) {
				final UnmodifiableTransFormula loopRelation = traceToTf(loop);
				final UnmodifiableTransFormula acceleratedLoopRelation =
						mAccelerator.accelerateLoop(loopRelation, AccelerationMethod.FAST_UPR);
				accelerations.add(acceleratedLoopRelation);
			}
			mAccelerations.put(loophead.getKey(), accelerations);
		}
		mIsTraceCorrect = checkFeasibility(mCounterexample);
		if (mIsTraceCorrect == LBool.UNSAT) {
			mInterpolants = generateInterpolants();
		}
	}

	private LBool checkFeasibility(final List<LETTER> trace) {
		final UnmodifiableTransFormula tf = trace.get(0).getTransformula();
		final List<UnmodifiableTransFormula> tfs = new ArrayList<>();
		tfs.add(tf);
		int i = 1;
		while (i < trace.size()) {
			final LETTER l = trace.get(i);
			if (mAccelerations.containsKey(l.getSource())) {
				tfs.addAll(mAccelerations.get(l.getSource()));
			}
			tfs.add(trace.get(i).getTransformula());
			i++;
		}
		final UnmodifiableTransFormula traceTf = TransFormulaUtils.sequentialComposition(mLogger, mServices, mScript,
				false, false, false, XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION,
				SimplificationTechnique.SIMPLIFY_DDA, tfs);
		final LBool result = SmtUtils.checkSatTerm(mScript.getScript(), traceTf.getFormula());
		return result;
	}

	private UnmodifiableTransFormula traceToTf(final List<LETTER> trace) {
		final List<UnmodifiableTransFormula> tfs = new ArrayList<>();
		for (final LETTER l : trace) {
			tfs.add(l.getTransformula());
		}
		return TransFormulaUtils.sequentialComposition(mLogger, mServices, mScript, true, true, true,
				XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION, SimplificationTechnique.SIMPLIFY_DDA, tfs);
	}

	private IPredicate[] generateInterpolants() {
		final IPredicate[] interpolants = new IPredicate[mCounterexample.size() + 1];
		interpolants[0] = mPredicateUnifier.getTruePredicate();
		interpolants[mCounterexample.size()] = mPredicateUnifier.getFalsePredicate();
		for (int i = 1; i <= mCounterexample.size(); i++) {
			interpolants[i] = mPredicateUnifier.getOrConstructPredicate(mPredTransformer
					.strongestPostcondition(interpolants[i - 1], mCounterexample.get(i - 1).getTransformula()));
		}
		final IPredicate[] actualInterpolants = Arrays.copyOfRange(interpolants, 1, mCounterexample.size());
		return actualInterpolants;
	}

	private IProgramExecution<IIcfgTransition<IcfgLocation>, Term> computeProgramExecution() {
		// TODO: construct a real IProgramExecution using
		// IcfgProgramExecutionBuilder (DD needs to refactor s.t. the
		// class becomes available here).
		if (mIsTraceCorrect == LBool.SAT) {
			return IProgramExecution.emptyExecution(Term.class, IcfgEdge.class);
		}
		return null;
	}

	@Override
	public LBool isCorrect() {
		return mIsTraceCorrect;
	}

	@Override
	public IPredicate getPrecondition() {
		return mPredicateUnifier.getTruePredicate();
	}

	@Override
	public IPredicate getPostcondition() {
		return mPredicateUnifier.getFalsePredicate();
	}

	@Override
	public Map<Integer, IPredicate> getPendingContexts() {
		return Collections.emptyMap();
	}

	@Override
	public boolean providesRcfgProgramExecution() {
		return mIsTraceCorrect != LBool.SAT;
	}

	@Override
	public IProgramExecution<IIcfgTransition<IcfgLocation>, Term> getRcfgProgramExecution() {
		if (mFeasibleProgramExecution == null) {
			mFeasibleProgramExecution = computeProgramExecution();
		}
		return mFeasibleProgramExecution;
	}

	@Override
	public TraceCheckReasonUnknown getTraceCheckReasonUnknown() {
		return mReasonUnknown;
	}

	@Override
	public boolean wasTracecheckFinishedNormally() {
		return mTraceCheckFinishedNormally;
	}

	@Override
	public List<LETTER> getTrace() {
		return mCounterexample;
	}

	@Override
	public IPredicate[] getInterpolants() {
		return mInterpolants;
	}

	@Override
	public IPredicateUnifier getPredicateUnifier() {
		return mPredicateUnifier;
	}

	@Override
	public boolean isPerfectSequence() {
		return isCorrect() == LBool.UNSAT;
	}

	@Override
	public InterpolantComputationStatus getInterpolantComputationStatus() {
		if (isCorrect() == LBool.UNSAT) {
			return new InterpolantComputationStatus();
		} else if (isCorrect() == LBool.SAT) {
			return new InterpolantComputationStatus(ItpErrorStatus.TRACE_FEASIBLE, null);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public IStatisticsDataProvider getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

}
