package de.uni_freiburg.informatik.ultimate.heapseparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermTransformer;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transformations.ReplacementVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVarOrConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalStore;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.vp.EqNode;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.vp.VPDomain;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.irsdependencies.rcfg.visitors.SimpleRCFGVisitor;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation3;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;

public class HeapSepRcfgVisitor extends SimpleRCFGVisitor {

	/**
	 * arrayId before separation --> pointerId --> arrayId after separation
	 */
	NestedMap2<IProgramVarOrConst, EqNode, ReplacementVar> moldArrayToPointerToNewArray;
	/**
	 * arrayId before separation --> arrayId after separation--> Set of pointerIds
	 */
//	HashRelation3<IProgramVarOrConst, ReplacementVar, EqNode> marrayToPartitions;
//	HashRelation3<IProgramVarOrConst, ReplacementVar, EqNode> mArrayIdToNewArrayIdToPointers;
	ManagedScript mScript;
	private VPDomain mVpDomain;
	private NewArrayIdProvider mNewArrayIdProvider;

	public HeapSepRcfgVisitor(final ILogger logger,
//			final NestedMap2<IProgramVarOrConst, EqNode, ReplacementVar> oldArrayToPointerToNewArray,
			NewArrayIdProvider naip,
			final ManagedScript script,
			final VPDomain vpDomain) {
		super(logger);
//		moldArrayToPointerToNewArray = oldArrayToPointerToNewArray;
//		marrayToPartitions = null; // TODO: do we need reverseInnerMap, still??
									// //reverseInnerMap(moldArrayToPointerToNewArray);
		mNewArrayIdProvider = naip;
		mVpDomain = vpDomain;
		mScript = script;
	}

	// private HashMap<IProgramVar, HashMap<IProgramVar, HashSet<IProgramVar>>> reverseInnerMap(
	// final NestedMap2<IProgramVar,IProgramVar,IProgramVar> moldArrayToPointerToNewArray2) {
	// final HashMap<IProgramVar, HashMap<IProgramVar, HashSet<IProgramVar>>> result = new HashMap<IProgramVar,
	// HashMap<IProgramVar,HashSet<IProgramVar>>>();
	//
	// for (final Triple<IProgramVar, IProgramVar, IProgramVar> en1 : moldArrayToPointerToNewArray2.entrySet()) {
	// result.put(en1.getKey(), new HashMap<>());
	// for (final Entry<IProgramVar, IProgramVar> en2 : en1.getValue().entrySet()) {
	// HashSet<IProgramVar> pointers = result.get(en1.getKey()).get(en2.getValue());
	// if (pointers == null) {
	// pointers = new HashSet<IProgramVar>();
	// result.get(en1.getKey()).put(en2.getValue(), pointers);
	// }
	// pointers.add(en2.getKey());
	// }
	// }
	// return result;
	// }

	@Override
	public boolean performedChanges() {
		// TODO make smarter?
		return true;
	}

	@Override
	public boolean abortCurrentBranch() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abortAll() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void level(final IcfgEdge edge) {
		if (!(edge instanceof CodeBlock)) {
			return;
		}
		final UnmodifiableTransFormula tf = ((CodeBlock) edge).getTransitionFormula();

		final UnmodifiableTransFormula newTf = splitArraysInTransFormula(tf);

		((CodeBlock) edge).setTransitionFormula(newTf);

		super.level(edge);
	}

	public static TermVariable getSplitTermVariable(final String arrayName, final int splitIndex, final Sort sort,
			final Script script) {
		return script.variable(String.format("{}_split_{}", arrayName, splitIndex), sort);
	}

	public static IProgramVar getBoogieVarFromTermVar(final TermVariable tv, final Map<IProgramVar, TermVariable> map1,
			final Map<IProgramVar, TermVariable> map2) {
		for (final Entry<IProgramVar, TermVariable> en : map1.entrySet()) {
			if (en.getValue().equals(tv)) {
				return en.getKey();
			}
		}
		for (final Entry<IProgramVar, TermVariable> en : map2.entrySet()) {
			if (en.getValue().equals(tv)) {
				return en.getKey();
			}
		}
		assert false : "did not find " + tv + " in the given maps";
		return null;
	}

	private UnmodifiableTransFormula splitArraysInTransFormula(final UnmodifiableTransFormula tf) {

		// new plan: we don't need a term transformer, here (at least not for the simple cases?..)
		// --> just rename the BoogieVars in the maps
		// insight:
		// that won't work
		// example:
		// "(= x' (+ (select a p) (select a q)))" where p and q don't alias
		// --> we'll need to duplicate a in outvars in this case..
		// other thing:
		// "(store (store a q i) p j)" where p and q don't alias
		// --> is this a problem? Is this even possible?

		// cases where arrays may occur, in the SMT theory of arrays:
		// - store
		// - select
		// - equals/distinct
		// example
		// a' in "(= a' (store a p i))"
		//

		//		final ArraySplitter as = new ArraySplitter(mScript.getScript(),
		//				 tf.getInVars(), tf.getOutVars());
		//		final TransFormulaBuilder tfb = new TransFormulaBuilder(as.getUpdatedInVars(), as.getUpdatedOutVars(), false,
		//				tf.getNonTheoryConsts(), false, tf.getBranchEncoders(), false);
		//		final Term newFormula = as.transform(tf.getFormula());
		//		tfb.setFormula(newFormula);
		//		tfb.setInfeasibility(tf.isInfeasible());
		//		tfb.addAuxVarsButRenameToFreshCopies(tf.getAuxVars(), mScript);
		//		return tfb.finishConstruction(mScript);


		/*
		 * need:
		 *  - a substitution that also updates the invars/outvars
		 * plan:
		 *  use 
		 *   -- ArrayEquality
		 *   -- ArrayUpdate
		 *   -- MultiDimensionalSelect
		 *   -- MultiDimensionalStore
		 *    --> the extract-Methods of each
		 *  build a substitution
		 */

		Map<IProgramVarOrConst, ReplacementVar> substitutionMap = new HashMap<>();

		List<MultiDimensionalStore> mdStores = MultiDimensionalStore.extractArrayStoresShallow(tf.getFormula());

		List<MultiDimensionalSelect> mdSelects = MultiDimensionalSelect.extractSelectShallow(tf.getFormula(), false); //TODO allowArrayValues??

		for (MultiDimensionalSelect mds : mdSelects) {
			IProgramVarOrConst oldArray = mVpDomain.getPreAnalysis().getIProgramVarOrConst(mds.getArray());
			List<EqNode> pointers = mds.getIndex().stream()
					.map(indexTerm -> mVpDomain.getPreAnalysis().getTermToEqNodeMap().get(indexTerm))
					.collect(Collectors.toList());
			ReplacementVar newArray = mNewArrayIdProvider.getNewArrayId(oldArray, pointers);
			substitutionMap.put( oldArray, newArray);
		}

		return null;
	}


	static UnmodifiableTransFormula substituteInTranformula(TransFormula originalFormula,
				Map<IProgramVarOrConst, IProgramVarOrConst> substitutionMap) {

		return null;
	}
	
	


//	class TransFormulaSubstitution {
//		
//		TransFormulaSubstitution(TransFormula originalFormula, 
//				Map<IProgramVarOrConst, IProgramVarOrConst> substitutionMap) {
//			
//		}
//	}
	

//	/**
//	 * Input: maps that say how arrays should be split a term to split arrays in inVar and outVar mappings 
//	 * Output: the term where arrays are split 
//	 * SideEffect: inVar and outVar mappings are updated according to the splitting
//	 */
//	class ArraySplitter extends TermTransformer {
//		Script mscript;
//		Map<IProgramVar, TermVariable> minVars;
//		Map<IProgramVar, TermVariable> moutVars;
//
//		Set<IProgramVar> minVarsToRemove = new HashSet<IProgramVar>();
//		Map<IProgramVar, TermVariable> minVarsToAdd = new HashMap<>();
//		Set<IProgramVar> moutVarsToRemove = new HashSet<IProgramVar>();
//		Map<IProgramVar, TermVariable> moutVarsToAdd = new HashMap<>();
//
//		boolean mequalsMode = false;
//		// BoogieVar maOld;
//		TermVariable maOld;
//		// BoogieVar maNew;
//		TermVariable maNew;
//
//		/**
//		 * arrayId (old array) X pointerId -> arrayId (split version)
//		 */
////		NestedMap2<IProgramVarOrConst, EqNode, ReplacementVar> marrayToPointerToPartition;
//		/**
//		 * arrayId (old array) X arrayId (split version) -> Set(pointerIds)
//		 */
////		HashRelation3<IProgramVarOrConst, ReplacementVar, EqNode> mArrayIdToNewArrayIdToPointers;
//		
//
//		public ArraySplitter(final Script script,
////				final NestedMap2<IProgramVarOrConst, EqNode, ReplacementVar> moldArrayToPointerToNewArray,
////				final HashRelation3<IProgramVarOrConst, ReplacementVar, EqNode> arrayToPartitions,
//				final Map<IProgramVar, TermVariable> inVars, 
//				final Map<IProgramVar, TermVariable> outVars) {
////			marrayToPointerToPartition = moldArrayToPointerToNewArray;
////			mArrayIdToNewArrayIdToPointers = arrayToPartitions;
//			mscript = script;
//			minVars = inVars;
//			moutVars = outVars;
//		}
//
//		public ArraySplitter(final Script script,
////				final NestedMap2<IProgramVarOrConst, EqNode, ReplacementVar> arrayToPointerToPartition,
////				final HashRelation3<IProgramVarOrConst, ReplacementVar, EqNode> arrayToPartitions,
//				final Map<IProgramVar, TermVariable> inVars, 
//				final Map<IProgramVar, TermVariable> outVars,
//				final TermVariable aOld, final TermVariable aNew) {
//			this(script, inVars, outVars);
//			mequalsMode = true;
//			maOld = aOld;
//			maNew = aNew;
//		}
//
//		@Override
//		protected void convert(final Term term) {
//			if (mequalsMode) {
//				// TODO: not sure how robust this is..
//				if (term.equals(maOld)) {
//					setResult(maNew);
//					mequalsMode = false;
//					return;
//				}
//			}
//			if (term instanceof ApplicationTerm) {
//				final ApplicationTerm appTerm = (ApplicationTerm) term;
//				if (appTerm.getFunction().getName().equals("select")
//						|| appTerm.getFunction().getName().equals("store")) {
//
//					if (mequalsMode && appTerm.getFunction().getName().equals("store")
//							&& appTerm.getParameters()[0] instanceof TermVariable) {
//						// TODO: not sure how robust this is..
//						super.convert(appTerm);
//						return;
//
//					} else if (appTerm.getParameters()[0] instanceof TermVariable
//							&& appTerm.getParameters()[1] instanceof TermVariable) {
//						assert appTerm.getParameters()[0].getSort().isArraySort();
//
//						final IProgramVar oldArrayVar =
//								getBoogieVarFromTermVar(((TermVariable) appTerm.getParameters()[0]), minVars, moutVars);
//
//						final Map<IProgramVar, IProgramVar> im = marrayToPartitions.get(oldArrayVar);
//						if (im != null) {
//							final IProgramVar ptrName = getBoogieVarFromTermVar(
//									((TermVariable) appTerm.getParameters()[1]), minVars, moutVars);
//
//							final IProgramVar newArrayVar = im.get(ptrName);
//
//							final TermVariable newVar = newArrayVar.getTermVariable(); // FIXME probably replace
//																						// getTermVariable, here
//
//							final Term newTerm = appTerm.getFunction().getName().equals("store")
//									? mscript.term(appTerm.getFunction().getName(), newVar, appTerm.getParameters()[1],
//											appTerm.getParameters()[2])
//									: mscript.term(appTerm.getFunction().getName(), newVar, appTerm.getParameters()[1]);
//
//							// TODO: do we also need outVars here, sometimes?
//							minVarsToRemove.add(oldArrayVar);
//							minVarsToAdd.put(newArrayVar, newVar);
//
//							setResult(newTerm);
//							return;
//						}
//					}
//				} else if (appTerm.getFunction().getName().equals("=")) {
//					if (appTerm.getParameters()[0].getSort().isArraySort()
//							&& appTerm.getParameters()[1].getSort().isArraySort()) {
//
//						final Term p0 = appTerm.getParameters()[0];
//						final Term p1 = appTerm.getParameters()[1];
//
//						// final ArrayFinder af0 = new ArrayFinder();
//						// af0.transform(p0);
//						final TermVariable a0Tv = new ArrayFinder(p0).getResult();
//						final IProgramVar a0Id = getBoogieVarFromTermVar(a0Tv, minVars, moutVars);
//
//						// final ArrayFinder af1 = new ArrayFinder();
//						// af1.transform(p1);
//						final TermVariable a1Tv = new ArrayFinder(p1).getResult();
//						final IProgramVar a1Id = getBoogieVarFromTermVar(a1Tv, minVars, moutVars);
//
//						/*
//						 * sanity check: if anywhere in the program, the two arrays a and b are equated, their
//						 * partitionings must be compatible i.e., no partition of a may overlap two partitions of b and
//						 * vice versa
//						 */
//						// assert partitionsAreCompatible(findArrayId(p0), findArrayId(p1)); TODO: write those methods
//						// for the assert..
//
//						final ArrayList<Term> equationConjunction = new ArrayList<Term>();
//
//						/*
//						 * for each partition p of a, which has an intersecting partition q of b: equate e1[a_p] =
//						 * e2[b_q] (where e1, e2 may be stores or just array identifiers (something else?).
//						 */
//						for (final Entry<IProgramVar, HashSet<IProgramVar>> a0SplitArrayToPointers : mArrayIdToNewArrayIdToPointers
//								.get(a0Id).entrySet()) {
//							for (final Entry<IProgramVar, HashSet<IProgramVar>> a1SplitArrayToPointers : mArrayIdToNewArrayIdToPointers
//									.get(a1Id).entrySet()) {
//
//								final HashSet<IProgramVar> intersection =
//										new HashSet<IProgramVar>(a0SplitArrayToPointers.getValue());
//								intersection.retainAll(a1SplitArrayToPointers.getValue());
//
//								if (!intersection.isEmpty()) {
//									final IProgramVar a0New = a0SplitArrayToPointers.getKey();
//									final IProgramVar a1New = a1SplitArrayToPointers.getKey();
//									final TermVariable a0NewTv = a0New.getTermVariable(); // TODO replace
//																							// getTermVariable through a
//																							// unique version
//									final TermVariable a1NewTv = a1New.getTermVariable(); // TODO replace
//																							// getTermVariable through a
//																							// unique version
//									equationConjunction.add(mscript.term("=",
//											new ArraySplitter(mscript, marrayToPointerToPartition, mArrayIdToNewArrayIdToPointers,
//													minVars, moutVars, a0Tv, a0NewTv)
//															.transform(appTerm.getParameters()[0]),
//											new ArraySplitter(mscript, marrayToPointerToPartition, mArrayIdToNewArrayIdToPointers,
//													minVars, moutVars, a1Tv, a1NewTv)
//															.transform(appTerm.getParameters()[1])));
//
//									if (minVars.containsKey(a0Id)) {
//										minVarsToRemove.add(a0Id);
//										minVarsToAdd.put(a0New, a0NewTv);
//									} else if (moutVars.containsKey(a0Id)) {
//										moutVarsToRemove.add(a0Id);
//										moutVarsToAdd.put(a0New, a0NewTv);
//									} else {
//										assert false;
//									}
//
//									if (minVars.containsKey(a1Id)) {
//										minVarsToRemove.add(a1Id);
//										minVarsToAdd.put(a1Id, a1NewTv);
//									} else if (moutVars.containsKey(a1Id)) {
//										moutVarsToRemove.add(a1Id);
//										moutVarsToAdd.put(a1Id, a1NewTv);
//									} else {
//										assert false;
//									}
//
//								}
//							}
//						}
//						setResult(
//								mscript.term("and", equationConjunction.toArray(new Term[equationConjunction.size()])));
//						return;
//					}
//				}
//			}
//
//			super.convert(term);
//		}
//
//		public HashMap<IProgramVar, TermVariable> getUpdatedInVars() {
//			final HashMap<IProgramVar, TermVariable> result = new HashMap<IProgramVar, TermVariable>(minVars);
//			for (final IProgramVar bv : minVarsToRemove) {
//				result.remove(bv);
//			}
//			for (final Entry<IProgramVar, TermVariable> en : minVarsToAdd.entrySet()) {
//				result.put(en.getKey(), en.getValue());
//			}
//			return result;
//		}
//
//		public HashMap<IProgramVar, TermVariable> getUpdatedOutVars() {
//			final HashMap<IProgramVar, TermVariable> result = new HashMap<IProgramVar, TermVariable>(moutVars);
//			for (final IProgramVar bv : moutVarsToRemove) {
//				result.remove(bv);
//			}
//			for (final Entry<IProgramVar, TermVariable> en : moutVarsToAdd.entrySet()) {
//				result.put(en.getKey(), en.getValue());
//			}
//			return result;
//		}
//
//	}
}