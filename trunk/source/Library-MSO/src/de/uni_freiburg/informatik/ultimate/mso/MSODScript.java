/*
 * Copyright (C) 2018 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2019 Nico Hauff (hauffn@informatik.uni-freiburg.de)
 * Copyright (C) 2019 Elisabeth Henkel (henkele@informatik.uni-freiburg.de)
 * Copyright (C) 2018 University of Freiburg
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.Format;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.ConstantFinder;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.NoopScript;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * Solver for Monadic Second Order Difference Logic Formulas
 *
 * TODO Check inputs.
 *
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * @author Elisabeth Henkel (henkele@informatik.uni-freiburg.de)
 * @author Nico Hauff (hauffn@informatik.uni-freiburg.de)
 */
public class MSODScript extends NoopScript {
	private final AutomataLibraryServices mAutomataLibrarayServices;
	private final MSODOperations mMSODOperations;
	public final ILogger mLogger;
	private Term mAssertionTerm;
	private Map<Term, Term> mModel;

	public enum MSODLogic {
		MSODInt, MSODNat, MSODIntWeak, MSODNatWeak,
	}

	public MSODScript(final IUltimateServiceProvider services, final ILogger logger, final MSODLogic logic) {
		mAutomataLibrarayServices = new AutomataLibraryServices(services);
		mLogger = logger;

		if (logic == MSODLogic.MSODNatWeak) {
			mMSODOperations = new MSODOperations(services, this, logger, new MSODFormulaOperationsNat(),
					new MSODAutomataOperationsWeak());
		} else if (logic == MSODLogic.MSODIntWeak) {
			mMSODOperations = new MSODOperations(services, this, logger, new MSODFormulaOperationsInt(),
					new MSODAutomataOperationsWeak());
		} else if (logic == MSODLogic.MSODNat) {
			mMSODOperations = new MSODOperations(services, this, logger, new MSODFormulaOperationsNat(),
					new MSODAutomataOperationsBuchi());
		} else if (logic == MSODLogic.MSODInt) {
			mMSODOperations = new MSODOperations(services, this, logger, new MSODFormulaOperationsInt(),
					new MSODAutomataOperationsBuchi());
		} else {
			throw new AssertionError("Unknown value: " + logic);
		}
	}

	@Override
	public void setLogic(final String logic) throws UnsupportedOperationException, SMTLIBException {
		super.setLogic(logic);
	}

	@Override
	public void setLogic(final Logics logic) throws UnsupportedOperationException, SMTLIBException {
		super.setLogic(logic);
	}

	@Override
	public LBool assertTerm(final Term term) throws SMTLIBException {
		// mAssertionTerm = mAssertionTerm == null ? term : term("and", new Term[] { mAssertionTerm, term });
		return null;
	}

	@Override
	public LBool checkSat() throws SMTLIBException {
		mLogger.info("INPUT: " + mAssertionTerm);
		LBool result = LBool.UNKNOWN;

		try {

			final INestedWordAutomaton<MSODAlphabetSymbol, String> automaton =
					mMSODOperations.traversePostOrder(mAssertionTerm);

			mLogger.info(automatonToString(automaton, Format.ATS));
			mModel = mMSODOperations.getResult(this, mAutomataLibrarayServices, automaton);

			if (mModel == null) {
				mLogger.info("RESULT: UNSAT");
				return LBool.UNSAT;
			}

			if (mModel.keySet().toString().contains("emptyWord")) {
				// TODO Deal with empty word
				mLogger.info("Model: EMPTYWORD");
				final ConstantFinder cf = new ConstantFinder();
				final Set<ApplicationTerm> terms = cf.findConstants(mAssertionTerm, true);
				mModel.clear();
				for (final Term t : terms) {
					mModel.put(t, mAssertionTerm.getTheory().mFalse);
				}
			}

			result = LBool.SAT;
			mLogger.info("RESULT: SAT");
			mLogger.info("MODEL: " + mModel);

		} catch (final Exception e) {
			final StringWriter stringWriter = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			mLogger.info(stringWriter);
		}

		return result;
	}

	@Override
	public Map<Term, Term> getValue(final Term[] terms) throws SMTLIBException {
		final Map<Term, Term> values = new HashMap<>();

		if (mModel == null) {
			return values;
		}

		for (final Term term : terms) {
			Term value = mModel.get(term);

			if (value == null) {
				if (SmtSortUtils.isIntSort(term.getSort())) {
					value = SmtUtils.constructIntValue(this, BigInteger.ZERO);
				}

				if (MSODUtils.isSetOfIntSort(term.getSort())) {
					value = MSODUtils.constructSetOfIntValue(this, new HashSet<BigInteger>());
				}
			}
			values.put(term, value);
		}

		return values;
	}

	@Override
	public Model getModel() throws SMTLIBException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a string representation of the given automaton. (only for debugging)
	 */
	private String automatonToString(final IAutomaton<?, ?> automaton, final Format format) {
		AutomatonDefinitionPrinter<?, ?> printer;
		printer = new AutomatonDefinitionPrinter<>(mAutomataLibrarayServices, "", Format.ATS, automaton);

		return printer.getDefinitionAsString();
	}
}
