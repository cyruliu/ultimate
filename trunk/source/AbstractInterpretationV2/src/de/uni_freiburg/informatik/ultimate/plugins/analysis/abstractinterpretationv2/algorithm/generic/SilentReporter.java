package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.generic;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.AbstractCounterexample;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.IResultReporter;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractState;

/**
 * This {@link IResultReporter} does not generate any results.
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public final class SilentReporter<STATE extends IAbstractState<STATE, ACTION, VARDECL>, ACTION, VARDECL, LOCATION>
		implements IResultReporter<STATE, ACTION, VARDECL, LOCATION> {

	@Override
	public void reportSafe(ACTION elem) {

	}

	@Override
	public void reportSafe(ACTION elem, String msg) {

	}

	@Override
	public void reportPossibleError(AbstractCounterexample<STATE, ACTION, ?, LOCATION> cex) {

	}
}