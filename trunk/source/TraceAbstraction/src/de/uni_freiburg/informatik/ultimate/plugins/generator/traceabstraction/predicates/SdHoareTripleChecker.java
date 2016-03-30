/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates;

import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.ICallAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IInternalAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IReturnAction;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.HoareTripleCheckerStatisticsGenerator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.IPredicateCoverageChecker;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;

/**
 * Hoare triple checker that uses only simple dataflow analysis to check 
 * triples. If this simple analysis is not able to determine the result
 * UNKNOWN is returned.
 * @author Matthias Heizmann
 *
 */
public class SdHoareTripleChecker implements IHoareTripleChecker {
	
	private final SdHoareTripleCheckerHelper m_SdHoareTripleChecker;
	private final IPredicateCoverageChecker m_PredicateCoverageChecker;
	private final IPredicate m_TruePredicate;
	private final IPredicate m_FalsePredicate;
	private final static boolean m_LazyChecks = false;
	private final InternalCheckHelper m_InternalCheckHelper = new InternalCheckHelper();
	private final CallCheckHelper m_CallCheckHelper = new CallCheckHelper();
	private final ReturnCheckHelper m_ReturnCheckHelper = new ReturnCheckHelper();

	
	public SdHoareTripleChecker(ModifiableGlobalVariableManager modGlobVarManager, 
			PredicateUnifier predicateUnifier, 
			HoareTripleCheckerStatisticsGenerator edgeCheckerBenchmarkGenerator) {
		m_PredicateCoverageChecker = predicateUnifier.getCoverageRelation();
		m_TruePredicate = predicateUnifier.getTruePredicate();
		m_FalsePredicate = predicateUnifier.getFalsePredicate();
		m_SdHoareTripleChecker = new SdHoareTripleCheckerHelper(modGlobVarManager, 
				m_PredicateCoverageChecker, edgeCheckerBenchmarkGenerator);
		
	}
	
	
	@Override
	public Validity checkInternal(IPredicate pre, IInternalAction act, IPredicate succ) {
		return m_InternalCheckHelper.check(pre, null, act, succ);

	}

	@Override
	public Validity checkCall(IPredicate pre, ICallAction act, IPredicate succ) {
		return m_CallCheckHelper.check(pre, null, act, succ);
	}

	@Override
	public Validity checkReturn(IPredicate preLin, IPredicate preHier, IReturnAction act, IPredicate succ) {
		return m_ReturnCheckHelper.check(preLin, preHier, act, succ);
	}

	@Override
	public HoareTripleCheckerStatisticsGenerator getEdgeCheckerBenchmark() {
		return m_SdHoareTripleChecker.getEdgeCheckerBenchmark();
	}
	
	
	
	
	/**
	 * Abstract class for data-flow based Hoare triple checks.
	 * Subclasses are checks for internal, call, and return. 
	 * Because we can only override methods with the same signature (in Java) 
	 * we use the 3-parameter-signature for return (with hierarchical state) 
	 * and use null as hierarchical state for call and internal.
	 */
	public abstract class CheckHelper {

		public Validity check(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			if (act instanceof IInternalAction) {
				if (((IInternalAction) act).getTransformula().isInfeasible() == Infeasibility.INFEASIBLE) {
					return Validity.VALID;
				}
			}
			
			boolean unknownCoverage = false;
			// check if preLin is equivalent to false
			switch (m_PredicateCoverageChecker.isCovered(preLin, m_FalsePredicate)) {
			case INVALID:
				break;
			case NOT_CHECKED:
				throw new AssertionError("unchecked predicate");
			case UNKNOWN:
				unknownCoverage = true;
				break;
			case VALID:
				return Validity.VALID;
			default:
				throw new AssertionError("unknown case");
			}
			
			// check if preHier is equivalent to false
			if (preHier != null) {
				switch (m_PredicateCoverageChecker.isCovered(preHier, m_FalsePredicate)) {
				case INVALID:
					break;
				case NOT_CHECKED:
					throw new AssertionError("unchecked predicate");
				case UNKNOWN:
					unknownCoverage = true;
					break;
				case VALID:
					return Validity.VALID;
				default:
					throw new AssertionError("unknown case");
				}
			}
			
			// check if succ is equivalent to true
			switch (m_PredicateCoverageChecker.isCovered(m_TruePredicate, succ)) {
			case INVALID:
				break;
			case NOT_CHECKED:
				throw new AssertionError("unchecked predicate");
			case UNKNOWN:
				unknownCoverage = true;
				break;
			case VALID:
				return Validity.VALID;
			default:
				throw new AssertionError("unknown case");
			}
			if (unknownCoverage) {
				return Validity.UNKNOWN;
			}
			boolean isInductiveSelfloop = this.isInductiveSefloop(preLin, preHier, act, succ);
			if (isInductiveSelfloop) {
				return Validity.VALID;
			}
			if (SmtUtils.isFalse(succ.getFormula())) {
				Validity toFalse = this.sdecToFalse(preLin, preHier, act);
				if (toFalse == null) {
					// do nothing an continue with other checks
				} else {
					switch (toFalse) {
					case INVALID:
						return Validity.INVALID;
					case NOT_CHECKED:
						throw new AssertionError("unchecked predicate");
					case UNKNOWN:
						throw new AssertionError("this case should have been filtered out before");
					case VALID:
						throw new AssertionError("this case should have been filtered out before");
					default:
						throw new AssertionError("unknown case");
					}
				}
			}
			final Validity general;
			if (m_LazyChecks) {
				general = sdLazyEc(preLin, preHier, act, succ);
			} else {			
				general = sdec(preLin, preHier, act, succ);
			}
			if (general != null) {
				switch (general) {
				case INVALID:
					return Validity.INVALID;
				case NOT_CHECKED:
					throw new AssertionError("unchecked predicate");
				case UNKNOWN:
					throw new AssertionError("this case should have been filtered out before");
				case VALID:
					return Validity.VALID;
				default:
					throw new AssertionError("unknown case");
				}
			}
			return Validity.UNKNOWN;
		}

		public abstract Validity sdecToFalse(IPredicate preLin, IPredicate preHier, IAction act);

		public abstract boolean isInductiveSefloop(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ);

		public abstract Validity sdec(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ);

		public abstract Validity sdLazyEc(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ);

	}

	protected class InternalCheckHelper extends CheckHelper {

		@Override
		public Validity sdecToFalse(IPredicate preLin, IPredicate preHier, IAction act) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdecInternalToFalse(preLin, (IInternalAction) act);
		}

		@Override
		public boolean isInductiveSefloop(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			if ((preLin == succ) && (m_SdHoareTripleChecker.sdecInternalSelfloop(preLin, (IInternalAction) act) == Validity.VALID)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Validity sdec(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdecInteral(preLin, (IInternalAction) act, succ);
		}

		@Override
		public Validity sdLazyEc(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdLazyEcInteral(preLin, (IInternalAction) act, succ);
		}
	}

	protected class CallCheckHelper extends CheckHelper {

		@Override
		public Validity sdecToFalse(IPredicate preLin, IPredicate preHier, IAction act) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdecCallToFalse(preLin, (ICallAction) act);
		}

		@Override
		public boolean isInductiveSefloop(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			if ((preLin == succ) && (m_SdHoareTripleChecker.sdecCallSelfloop(preLin, (ICallAction) act) == Validity.VALID)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Validity sdec(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdecCall(preLin, (ICallAction) act, succ);
		}

		@Override
		public Validity sdLazyEc(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			assert preHier == null;
			return m_SdHoareTripleChecker.sdLazyEcCall(preLin, (Call) act, succ);
		}

	}

	public class ReturnCheckHelper extends CheckHelper {

		@Override
		public Validity sdecToFalse(IPredicate preLin, IPredicate preHier, IAction act) {
			// sat if (not only if!) preLin and preHier are independent,
			// hence we can use the "normal" sdec method
			return m_SdHoareTripleChecker.sdecReturn(preLin, preHier, (IReturnAction) act, m_FalsePredicate);
		}

		@Override
		public boolean isInductiveSefloop(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			if ((preLin == succ) && (m_SdHoareTripleChecker.sdecReturnSelfloopPre(preLin, (IReturnAction) act) == Validity.VALID)) {
				return true;
			} else if ((preHier == succ)
					&& (m_SdHoareTripleChecker.sdecReturnSelfloopHier(preHier, (IReturnAction) act) == Validity.VALID)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Validity sdec(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			return m_SdHoareTripleChecker.sdecReturn(preLin, preHier, (IReturnAction) act, succ);
		}

		@Override
		public Validity sdLazyEc(IPredicate preLin, IPredicate preHier, IAction act, IPredicate succ) {
			return m_SdHoareTripleChecker.sdLazyEcReturn(preLin, preHier, (IReturnAction) act, succ);
		}

	}

	@Override
	public void releaseLock() {
		// do nothing, since objects of this class do not lock the solver
	}

}
