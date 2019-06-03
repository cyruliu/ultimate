/*
 * Copyright (C) 2019 Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE Library-SymbolicInterpretation plug-in.
 *
 * The ULTIMATE Library-SymbolicInterpretation plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Library-SymbolicInterpretation plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-SymbolicInterpretation plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-SymbolicInterpretation plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-SymbolicInterpretation plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.summarizers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.lib.pathexpressions.regex.IRegex;
import de.uni_freiburg.informatik.ultimate.lib.pathexpressions.regex.Star;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.DagInterpreter;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.ProcedureResources.OverlaySuccessors;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.StarDagCache;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.domain.IDomain;
import de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.regexdag.RegexDag;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

public class FixpointLoopSummarizer implements ILoopSummarizer {

	private final ILogger mLogger;
	private final IDomain mDomain;
	private final DagInterpreter mDagInterpreter;
	private final StarDagCache mStarDagCache = new StarDagCache();
	private final Map<Pair<Star<IIcfgTransition<IcfgLocation>>, IPredicate>, IPredicate> mCache;

	public FixpointLoopSummarizer(final ILogger logger, final IDomain domain, final DagInterpreter dagInterpreter) {
		mLogger = Objects.requireNonNull(logger);
		mDomain = Objects.requireNonNull(domain);
		mDagInterpreter = Objects.requireNonNull(dagInterpreter);
		mCache = new HashMap<>();
	}

	@Override
	public IPredicate summarize(final Star<IIcfgTransition<IcfgLocation>> regex, final IPredicate input) {
		final Pair<Star<IIcfgTransition<IcfgLocation>>, IPredicate> key = new Pair<>(regex, input);
		// TODO consider using cache more, for instance if loop is the same but
		// - input is a subset of a known input
		// - input is a superset of a known input, but a subset of any input from the iteration sequence.
		return mCache.computeIfAbsent(key, this::summarizeInternal);
	}

	private IPredicate summarizeInternal(final Pair<Star<IIcfgTransition<IcfgLocation>>, IPredicate> starAndInput) {
		final IRegex<IIcfgTransition<IcfgLocation>> starredRegex = starAndInput.getFirst().getInner();
		final RegexDag<IIcfgTransition<IcfgLocation>> dag = mStarDagCache.dagOf(starredRegex);
		IPredicate preState = starAndInput.getSecond();
		IPredicate postState = null;
		while (true) {
			// TODO make overlay to match everything. Even better: Create Overlay Interface with subclasses
			final OverlaySuccessors overlaySuccessors = null;
			// TODO get source node's post state.
			mDagInterpreter.interpret(dag, overlaySuccessors, preState);
			if (mDomain.isSubsetEq(preState, postState)) {
				break;
			}
			preState = mDomain.widen(preState, postState);
		}
		return postState;
	}

}
