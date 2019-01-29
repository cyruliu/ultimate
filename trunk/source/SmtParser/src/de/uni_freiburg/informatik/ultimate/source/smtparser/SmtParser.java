/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE SmtParser plug-in.
 *
 * The ULTIMATE SmtParser plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE SmtParser plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE SmtParser plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE SmtParser plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE SmtParser plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.source.smtparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.core.model.ISource;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelType;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.NoopScript;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.mso.MSODIntScript;
import de.uni_freiburg.informatik.ultimate.mso.MSODNatScript;
import de.uni_freiburg.informatik.ultimate.smtinterpol.LogProxy;
import de.uni_freiburg.informatik.ultimate.smtinterpol.option.OptionMap;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.ParseEnvironment;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.SmtInterpolLogProxyWrapper;
import de.uni_freiburg.informatik.ultimate.source.smtparser.SmtParserPreferenceInitializer.MsoLogic;
import de.uni_freiburg.informatik.ultimate.source.smtparser.chc.HCGBuilderHelper;
import de.uni_freiburg.informatik.ultimate.source.smtparser.chc.HCGBuilderHelper.ConstructAndInitializeBackendSmtSolver;
import de.uni_freiburg.informatik.ultimate.source.smtparser.chc.HornClauseParserScript;

/**
 * There are currently two basic modes in which SMTParser can work:
 * <li>Directly run an SMTSolver on the input smtlib-script (which one is run depends on a setting) This means it works
 * with an empty toolchain and does not return an IElement for further processing.</li>
 * <li>Read an smtlib-script for further processing in TreeAutomizer. This assumes that the logic is set to HORN and the
 * script only contains Horn clauses as defined in
 * <a href="http://github.com/sosy-lab/sv-benchmarks/tree/master/clauses"> github.com/sosy-lab/sv-benchmarks/tree/master
 * /clauses </a> Then a set of HornClauses is extracted and returned as an IElement (with a HornAnnot) for processing in
 * TreeAutomizer.</li>
 *
 *
 * @author Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * @author Daniel Dietsch
 * @author Matthias Heizmann
 */
public class SmtParser implements ISource {
	protected String[] mFileTypes;
	protected ILogger mLogger;
	protected List<String> mFileNames;
	protected Unit mPreludeUnit;
	private IUltimateServiceProvider mServices;
	private IToolchainStorage mStorage;
	/**
	 * Used for the HORN-case to return the HornAnnot generated by HornClauseParserScript
	 */
	private IElement mOutput;

	public SmtParser() {
		mFileTypes = new String[] { "smt2" };
		mFileNames = new ArrayList<>();
	}

	@Override
	public String getPluginID() {
		return getClass().getPackage().getName();
	}

	@Override
	public void init() {
		mFileNames = new ArrayList<>();
	}

	@Override
	public String getPluginName() {
		return "SmtParser";
	}

	public String[] getTokens() {
		return null;
	}

	@Override
	public IElement parseAST(final File[] files) throws Exception {
		if (files.length == 1) {
			return parseAST(files[0]);
		}
		throw new UnsupportedOperationException("Cannot parse more than one file");
	}

	private IElement parseAST(final File file) throws IOException {
		processFile(file);
		return mOutput;
	}

	@Override
	public File[] parseable(final File[] files) {
		final List<File> rtrList = Arrays.stream(files).filter(this::parseable).collect(Collectors.toList());
		return rtrList.toArray(new File[rtrList.size()]);
	}

	private boolean parseable(final File file) {
		for (final String s : getFileTypes()) {
			if (file.getName().endsWith(s)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] getFileTypes() {
		return mFileTypes;
	}

	@Override
	public ModelType getOutputDefinition() {
		return new ModelType(Activator.PLUGIN_ID, ModelType.Type.OTHER, mFileNames);
	}

	@Override
	public IPreferenceInitializer getPreferences() {
		return new SmtParserPreferenceInitializer();
	}

	@Override
	public void setToolchainStorage(final IToolchainStorage storage) {
		mStorage = storage;
	}

	@Override
	public void setServices(final IUltimateServiceProvider services) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
	}

	@Override
	public void finish() {
		// not necessary
	}

	private void processFile(final File file) throws IOException {

//		final boolean useExternalSolver = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
//				.getBoolean(SmtParserPreferenceInitializer.LABEL_UseExtSolver);
//		final String commandExternalSolver = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
//				.getString(SmtParserPreferenceInitializer.LABEL_ExtSolverCommand);
//		final boolean writeCommandsToFile = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
//				.getBoolean(SmtParserPreferenceInitializer.LABEL_WriteToFile);
//		final String filename =
//				mServices.getPreferenceProvider(Activator.PLUGIN_ID).getString(SmtParserPreferenceInitializer.LABEL_Filename);
		final String directory =
				mServices.getPreferenceProvider(Activator.PLUGIN_ID).getString(SmtParserPreferenceInitializer.LABEL_Directory);
		final boolean inHornSolverMode = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
				.getBoolean(SmtParserPreferenceInitializer.LABEL_HornSolverMode);
		final boolean inMsoSolverMode = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
				.getBoolean(SmtParserPreferenceInitializer.LABEL_MsoSolverMode);
		final MsoLogic msoLogic = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
				.getEnum(SmtParserPreferenceInitializer.LABEL_MsoLogic, MsoLogic.class);
		final boolean filterUnusedDeclarationsMode = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
				.getBoolean(SmtParserPreferenceInitializer.LABEL_FilterUnusedDeclarationsMode);

		final LogProxy logProxy = new SmtInterpolLogProxyWrapper(mLogger);

		if (filterUnusedDeclarationsMode) {
			runFilterUnusedDeclarationsMode(file, directory, logProxy);
			return;
		}

		final ConstructAndInitializeBackendSmtSolver caibss =
				new HCGBuilderHelper.ConstructAndInitializeBackendSmtSolver(mServices, mStorage,
						"smtParserBackendSolver");

		Script script;
		if (inHornSolverMode) {
			mLogger.info("Parsing .smt2 file as a set of Horn Clauses");
			script = new HornClauseParserScript(mServices, mLogger, file.getName(), caibss.getScript(),
//					"ALL", caibss.getSolverSettings());
					caibss.getLogicForExternalSolver(), caibss.getSolverSettings());
		} else if (inMsoSolverMode) {
			mLogger.info("Running our experimental MSO solver on input file");
			//script = new MoNatDiffScript(mServices, mLogger);
			script = new MSODIntScript(mServices, mLogger);
		} else {
			mLogger.info("Running solver on smt file");
//			if (useExternalSolver) {
//				mLogger.info("Starting external SMT solver with command " + commandExternalSolver);
//				script = new Scriptor(commandExternalSolver, mLogger, mServices, mStorage,
//						"external solver of SMT parser plugin");
//			} else {
//				mLogger.info("Starting SMTInterpol");
//				script = new SMTInterpol(logProxy);
//			}
			script = caibss.getScript().getScript();

//			if (writeCommandsToFile) {
//				final String abs = new File(filename).getAbsolutePath();
//				mLogger.info("Writing all SMT commands to " + abs);
//				script = new LoggingScript(script, filename, true);
//			}
		}

//		mLogger.info("Executing SMT file " + file.getAbsolutePath());


		final OptionMap optionMap = new OptionMap(logProxy, true);

		if (inHornSolverMode) {
			// crash in Horn solver mode if parsing fails
			optionMap.set(":continue-on-error", false);
			optionMap.set(":print-success", false);
		}

		final ParseEnvironment parseEnv = new ParseEnvironment(script, optionMap);
		try {
			parseEnv.parseScript(file.getAbsolutePath());
			mLogger.info("Succesfully executed SMT file " + file.getAbsolutePath());
			if (inHornSolverMode) {
				mOutput = ((HornClauseParserScript) script).getHornClauses();
			}
		} catch (final SMTLIBException exc) {
			mLogger.info("Failed while executing SMT file " + file.getAbsolutePath());
			mLogger.info("SMTLIBException " + exc.getMessage());
			parseEnv.printError(exc.getMessage());
		} finally {
			script.exit();
		}
	}

	private void runFilterUnusedDeclarationsMode(final File file, final String directory, final LogProxy logProxy)
			throws FileNotFoundException {
		final CollectNamesScript cns = new CollectNamesScript();

		final OptionMap optionMap = new OptionMap(logProxy, true);
		final ParseEnvironment parseEnv1 = new ParseEnvironment(cns, optionMap);
		try {
			parseEnv1.parseScript(file.getAbsolutePath());
			mLogger.info("Succesfully read SMT file " + file.getAbsolutePath());
		} catch (final SMTLIBException exc) {
			mLogger.info("Failed while reading SMT file " + file.getAbsolutePath());
			mLogger.info("SMTLIBException " + exc.getMessage());
			parseEnv1.printError(exc.getMessage());
		}

		final String outputFilename = directory + File.separator + file.getName();
		final ParseEnvironment parseEnv2 =
				new ParseEnvironment(new FilteredLoggingScript(outputFilename, true, cns.getNames()), optionMap);
		try {
			parseEnv2.parseScript(file.getAbsolutePath());
			mLogger.info("Succesfully wrote SMT file " + outputFilename);
		} catch (final SMTLIBException exc) {
			mLogger.info("Failed while writing SMT file " + outputFilename);
			mLogger.info("SMTLIBException " + exc.getMessage());
			parseEnv2.printError(exc.getMessage());
		}
	}

	private class CollectNamesScript extends NoopScript {

		Set<String> mNames = new HashSet<>();

		@Override
		public Term term(final String funcname, final BigInteger[] indices, final Sort returnSort, final Term... params)
				throws SMTLIBException {
			mNames.add(funcname);
			return super.term(funcname, indices, returnSort, params);
		}

		public Set<String> getNames() {
			return mNames;
		}
	}

	private class FilteredLoggingScript extends LoggingScript {

		private final Set<String> mAllowedNames;

		public FilteredLoggingScript(final String file, final boolean autoFlush, final Set<String> allowedNames)
				throws FileNotFoundException {
			super(file, autoFlush);
			mAllowedNames = allowedNames;
		}

		@Override
		public void declareFun(final String fun, final Sort[] paramSorts, final Sort resultSort)
				throws SMTLIBException {
			if (mAllowedNames.contains(fun)) {
				super.declareFun(fun, paramSorts, resultSort);
			} else {
				// do nothing
			}
		}

		@Override
		public void defineFun(final String fun, final TermVariable[] params, final Sort resultSort,
				final Term definition) throws SMTLIBException {
			if (mAllowedNames.contains(fun)) {
				super.defineFun(fun, params, resultSort, definition);
			} else {
				// do nothing
			}
		}

	}

}
