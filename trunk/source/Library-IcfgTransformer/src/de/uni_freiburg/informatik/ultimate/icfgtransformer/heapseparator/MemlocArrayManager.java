package de.uni_freiburg.informatik.ultimate.icfgtransformer.heapseparator;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.icfgtransformer.heapseparator.datastructures.EdgeInfo;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.heapseparator.datastructures.NoStoreInfo;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.heapseparator.datastructures.StoreInfo;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.absint.vpdomain.HeapSepProgramConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.ILocalProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVarOrConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSort;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap3;

public class MemlocArrayManager {

	private final boolean mFinalized;

	public static final String LOC_ARRAY_PREFIX = "#loc";
	public static final String LOC_SORT_PREFIX = "#locsort";
	public static final String INITLOCLIT_PREFIX = "#initloclit";

	private final ManagedScript mMgdScript;

	private final Map<Integer, Sort> mDimToLocSort = new HashMap<>();

	private final NestedMap3<EdgeInfo, Term, Integer, LocArrayInfo> mEdgeToArrayTermToDimToLocArray = new NestedMap3<>();

	/**
	 * used for internal caching
	 */
	private final NestedMap2<IProgramVarOrConst, Integer, IProgramVarOrConst> mArrayPvocToDimToLocArrayPvoc =
			new NestedMap2<>();

	private final Map<Sort, HeapSepProgramConst> mLocArraySortToInitLocLit = new HashMap<>();

	private final Set<IProgramNonOldVar> mGlobalLocArrays = new HashSet<>();

	private final Map<Term, HeapSepProgramConst> mInitLocLitTermToPvoc = new HashMap<>();

	private final Map<HeapSepProgramConst, NoStoreInfo> mInitLocPvocToNoStoreInfo = new HashMap<>();

	public MemlocArrayManager(final ManagedScript mgdScript) {
		mMgdScript = mgdScript;
		mFinalized = false;
	}

	/**
	 * We have different sorts for different dimensions. Note that it does not make
	 * sense to have different sorts for different arrays (perhaps for
	 * differentarray groups..)
	 *
	 * @param dim
	 * @return
	 */
	public Sort getMemlocSort(final int dim) {
		Sort result = mDimToLocSort.get(dim);
		if (result == null) {
			final String name = LOC_SORT_PREFIX + dim;
			mMgdScript.getScript().declareSort(name, 0);
			result = mMgdScript.getScript().sort(name);
			mDimToLocSort.put(dim, result);
		}
		return result;
	}

	public Set<HeapSepProgramConst> getInitLocLits() {
		if (!mFinalized) {
			throw new AssertionError();
		}
		return new HashSet<>(mLocArraySortToInitLocLit.values());
	}

	public LocArrayInfo getOrConstructLocArray(final EdgeInfo edgeInfo, final Term baseArrayTerm, final int dim) {
		LocArrayInfo result = mEdgeToArrayTermToDimToLocArray.get(edgeInfo, baseArrayTerm, dim);
		if (result == null) {
			assert !mFinalized;

			mMgdScript.lock(this);
			final Sort locArraySort = computeLocArraySort(baseArrayTerm.getSort());

			final IProgramVarOrConst pvoc;
			final Term term;
			{
				if (baseArrayTerm instanceof TermVariable) {
					final IProgramVar invar = edgeInfo.getInVar(baseArrayTerm);
					final IProgramVar outvar = edgeInfo.getOutVar(baseArrayTerm);
					final boolean isAuxVar = edgeInfo.getAuxVars().contains(baseArrayTerm);

					if (invar != null) {
						pvoc = getLocArrayPvocForArrayPvoc(invar, dim, locArraySort);
						term = mMgdScript.constructFreshTermVariable(
								sanitizeVarName(LOC_ARRAY_PREFIX + baseArrayTerm + "_" + dim),
								locArraySort);
					} else if (outvar != null) {
						pvoc = getLocArrayPvocForArrayPvoc(outvar, dim, locArraySort);
						term = mMgdScript.constructFreshTermVariable(
								sanitizeVarName(LOC_ARRAY_PREFIX + baseArrayTerm + "_" + dim),
								locArraySort);
					} else if (isAuxVar) {
						pvoc = null;
						term = mMgdScript.constructFreshTermVariable(
								sanitizeVarName(LOC_ARRAY_PREFIX + baseArrayTerm + "_" + dim),
								locArraySort);
					} else {
						throw new AssertionError();
					}
				} else {
					throw new UnsupportedOperationException("todo: deal with constants");
				}
			}
			final HeapSepProgramConst initLocLit = getOrConstructInitLocLitForLocArraySort(locArraySort, dim);
			result = new LocArrayInfo(edgeInfo, pvoc, term,
					computeInitConstantArrayForLocArray(initLocLit, locArraySort));

			mMgdScript.unlock(this);

			mEdgeToArrayTermToDimToLocArray.put(edgeInfo, baseArrayTerm, dim, result);
		}
		return result;
	}

	public Term getInitConstArrayForGlobalLocArray(final IProgramNonOldVar pnov) {
		final Sort locArraySort = pnov.getSort();
		final int dim = new MultiDimensionalSort(locArraySort).getDimension();
		final HeapSepProgramConst initLocLit = getOrConstructInitLocLitForLocArraySort(locArraySort, dim);
		return computeInitConstantArrayForLocArray(initLocLit, locArraySort);
	}

	public Set<IProgramNonOldVar> getGlobalLocArrays() {
		if (!mFinalized) {
			throw new AssertionError();
		}
		return Collections.unmodifiableSet(mGlobalLocArrays);
	}

	private HeapSepProgramConst getOrConstructInitLocLitForLocArraySort(final Sort locArraySort, final int dim) {
		assert new MultiDimensionalSort(locArraySort).getDimension() == dim;
		HeapSepProgramConst result = mLocArraySortToInitLocLit.get(locArraySort);

		if (result == null) {
			assert mMgdScript.isLockOwner(this);
			final String litName = INITLOCLIT_PREFIX + dim;
			mMgdScript.declareFun(this, litName, new Sort[0], getMemlocSort(dim));
			final ApplicationTerm locLitTerm = (ApplicationTerm) mMgdScript.term(this, litName);
			result = new HeapSepProgramConst(locLitTerm);
			mInitLocLitTermToPvoc.put(locLitTerm, result);
			mLocArraySortToInitLocLit.put(locArraySort, result);
			mInitLocPvocToNoStoreInfo.put(result, new NoStoreInfo(dim));
		}
		return result;
	}

	private IProgramVarOrConst getLocArrayPvocForArrayPvoc(final IProgramVarOrConst pvoc, final int dim,
			final Sort locArraySort) {
		IProgramVarOrConst result = mArrayPvocToDimToLocArrayPvoc.get(pvoc, dim);
		if (result == null) {
			if (pvoc instanceof IProgramNonOldVar) {
				result = ProgramVarUtils.constructGlobalProgramVarPair(
						sanitizeVarName(LOC_ARRAY_PREFIX + "_" + pvoc + "_" + locArraySort), locArraySort, mMgdScript, this);
				mGlobalLocArrays.add((IProgramNonOldVar) result);
			} else if (pvoc instanceof ILocalProgramVar) {
				throw new UnsupportedOperationException("todo: deal local variables");
			} else if (pvoc instanceof IProgramConst) {
				throw new UnsupportedOperationException("todo: deal with constants");
			} else {
				throw new AssertionError("unforseen case");
			}
		}
		return result;
	}

	private String sanitizeVarName(final String string) {
		final String result = string.replaceAll("\\|", "");
		if (result.isEmpty()) {
			throw new AssertionError();
		}
		return result;
	}

	/**
	 * Replace the last entry in the given array sort by the loc array sort
	 *
	 * @param sort
	 * @return
	 */
	private Sort computeLocArraySort(final Sort sort) {
		final MultiDimensionalSort mds = new MultiDimensionalSort(sort);
		assert mds.getDimension() > 0;
		final Deque<Sort> sortDeque = new ArrayDeque<>(mds.getIndexSorts());
		Sort resultSort = getMemlocSort(mds.getDimension());
		while (!sortDeque.isEmpty()) {
			final Sort last = sortDeque.pollLast();
			resultSort = SmtSortUtils.getArraySort(mMgdScript.getScript(), last, resultSort);
		}
		return resultSort;
	}

	private Term computeInitConstantArrayForLocArray(final HeapSepProgramConst locLit, final Sort locArraySort) {
		return mMgdScript.term(this, "const", null, locArraySort, locLit.getTerm());
	}

	public LocArrayInfo getLocArray(final EdgeInfo edgeInfo, final Term array, final int dim) {
		if (!mFinalized) {
			throw new AssertionError();
		}
		final LocArrayInfo result = mEdgeToArrayTermToDimToLocArray.get(edgeInfo, array, dim);
		if (result == null) {
			throw new IllegalArgumentException();
		}
		return result;
	}

	public HeapSepProgramConst getInitLocLitPvocForLocLitTerm(final Term locLitTerm) {
		return mInitLocLitTermToPvoc.get(locLitTerm);
	}

	public boolean isInitLocPvoc(final HeapSepProgramConst hspc) {
		final boolean result = mInitLocPvocToNoStoreInfo.containsKey(hspc);
		assert result == mInitLocLitTermToPvoc.values().contains(hspc);
		return result;
	}

	public StoreInfo getNoStoreInfo(final HeapSepProgramConst hspc) {
		return mInitLocPvocToNoStoreInfo.get(hspc);
	}
}