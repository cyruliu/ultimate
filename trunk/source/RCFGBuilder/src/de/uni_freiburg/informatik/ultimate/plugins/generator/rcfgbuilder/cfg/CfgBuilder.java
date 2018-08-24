/*
 * Copyright (C) 2013-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2010-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2018 Lars Nitzke (lars.nitzke@outlook.com)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE RCFGBuilder plug-in.
 *
 * The ULTIMATE RCFGBuilder plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE RCFGBuilder plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE RCFGBuilder plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE RCFGBuilder plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE RCFGBuilder plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Body;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.EnsuresSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ForkStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.GotoStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IfStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.JoinStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Label;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.boogie.ast.RequiresSpecification;
import de.uni_freiburg.informatik.ultimate.boogie.ast.ReturnStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.UnaryExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Unit;
import de.uni_freiburg.informatik.ultimate.boogie.ast.WhileStatement;
import de.uni_freiburg.informatik.ultimate.boogie.type.BoogieType;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.Check;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.LoopEntryAnnotation;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.LoopEntryAnnotation.LoopEntryType;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.LoopExitAnnotation;
import de.uni_freiburg.informatik.ultimate.core.lib.models.annotation.Overapprox;
import de.uni_freiburg.informatik.ultimate.core.model.models.IElement;
import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.core.model.models.ModelUtils;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieDeclarations;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Statements2TransFormula.TranslationResult;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.ConcurrencyInformation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgElement;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgForkTransitionCurrentThread;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgForkTransitionOtherThread;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgJoinTransitionCurrentThread;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgJoinTransitionOtherThread;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.DebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.LoopEntryDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.OrdinaryDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureEntryDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureErrorDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureErrorDebugIdentifier.ProcedureErrorType;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureErrorWithCheckDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureExitDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.ProcedureFinalDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.debugidentifiers.StringDebugIdentifier;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SolverBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SolverBuilder.SolverMode;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SolverBuilder.SolverSettings;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.RCFGBacktranslator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.WeakestPrecondition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence.Origin;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.RcfgPreferenceInitializer.CodeBlockSize;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.TransFormulaAdder;

/**
 * This class generates a recursive control flow graph (in the style of POPL'10 - Heizmann, Hoenicke, Podelski - Nested
 * Interpolants) from an Boogie AST which contains only unstructured Code (i.e., no while (and others??) statements).
 * The input for this classs has to be unstructured Boogie Code (e.g., no while loops) for example the output of the
 * BoogiePreprocessor.
 *
 * @author heizmann@informatik.uni-freiburg.de
 */

// TODO How to give every location the right line number
public class CfgBuilder {

	private static final String ULTIMATE_START = "ULTIMATE.start";

	/**
	 * ILogger for this plugin.
	 */
	private final ILogger mLogger;

	/**
	 * Root Node of this Ultimate model. I use this to store information that should be passed to the next plugin. The
	 * Successors of this node are exactly the entry nodes of procedures.
	 */
	private final BoogieIcfgContainer mIcfg;

	private final Boogie2SMT mBoogie2smt;
	private final BoogieDeclarations mBoogieDeclarations;
	TransFormulaAdder mTransFormulaAdder;

	Collection<Summary> mImplementationSummarys = new ArrayList<>();

	Collection<ForkCurrentThread> mImplementationForkCurrentThreads = new ArrayList<>();

	Collection<JoinCurrentThread> mJoinCurrentThreads = new ArrayList<>();

	Collection<String> mForkedProcedureNames = new ArrayList<>();

	Map<String, BoogieNonOldVar> mProcedureNameThreadIdMap = new HashMap<String, BoogieNonOldVar>();
	Map<String, BoogieNonOldVar> mProcedureNameToThreadInUseMap = new HashMap<String, BoogieNonOldVar>();

	private final RCFGBacktranslator mBacktranslator;

	private CodeBlockSize mCodeBlockSize;

	private final IUltimateServiceProvider mServices;

	private final boolean mAddAssumeForEachAssert;

	private final CodeBlockFactory mCbf;

	private final SimplificationTechnique mSimplificationTechnique = SimplificationTechnique.SIMPLIFY_DDA;
	private final XnfConversionTechnique mXnfConversionTechnique =
			XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION;

	public CfgBuilder(final Unit unit, final RCFGBacktranslator backtranslator, final IUltimateServiceProvider services,
			final IToolchainStorage storage) throws IOException {
		mServices = services;
		mLogger = services.getLoggingService().getLogger(Activator.PLUGIN_ID);
		mBacktranslator = backtranslator;
		final IPreferenceProvider prefs = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
		mAddAssumeForEachAssert = prefs.getBoolean(RcfgPreferenceInitializer.LABEL_ASSUME_FOR_ASSERT);

		mCodeBlockSize = prefs.getEnum(RcfgPreferenceInitializer.LABEL_CODE_BLOCK_SIZE, CodeBlockSize.class);

		final String pathAndFilename = ILocation.getAnnotation(unit).getFileName();
		final String filename = new File(pathAndFilename).getName();
		final Script script = constructAndInitializeSolver(services, storage, filename);
		final ManagedScript mgdScript = new ManagedScript(mServices, script);

		mBoogieDeclarations = new BoogieDeclarations(unit, mLogger);
		final boolean bitvectorInsteadInt = prefs.getBoolean(RcfgPreferenceInitializer.LABEL_BITVECTOR_WORKAROUND);
		final boolean simplePartialSkolemization =
				prefs.getBoolean(RcfgPreferenceInitializer.LABEL_SIMPLE_PARTIAL_SKOLEMIZATION);
		final List<ForkStatement> forkStatements = extractForkStatements(mBoogieDeclarations);
		mBoogie2smt = new Boogie2SMT(mgdScript, mBoogieDeclarations, bitvectorInsteadInt, mServices,
				simplePartialSkolemization, forkStatements);
		final ConcurrencyInformation concurInfo = mBoogie2smt.getConcurrencyInformation();
		if (concurInfo != null) {
			mProcedureNameToThreadInUseMap = concurInfo.getThreadInUseVars();
			mProcedureNameThreadIdMap = concurInfo.getProcedureNameThreadIdMap();
		}

		mIcfg = new BoogieIcfgContainer(mServices, mBoogieDeclarations, mBoogie2smt, concurInfo);
		mCbf = mIcfg.getCodeBlockFactory();
		mCbf.storeFactory(storage);
	}


	/**
	 * Returns list of all {@link ForkStatement}s from all declarations. Expects
	 * that the input has been "unstructured", i.e., all {@link WhileStatement}s and
	 * {@link IfStatement}s have been removed.
	 */
	private static List<ForkStatement> extractForkStatements(final BoogieDeclarations boogieDeclarations) {
		final List<ForkStatement> result = new ArrayList<>();
		for (final Entry<String, Procedure> entry : boogieDeclarations.getProcImplementation().entrySet()) {
			final Procedure proc = entry.getValue();
			final Body body = proc.getBody();
			for (final Statement st : body.getBlock()) {
				if ((st instanceof ForkStatement)) {
					result.add((ForkStatement) st);
				} else if ((st instanceof AssignmentStatement) || (st instanceof AssumeStatement)
						|| (st instanceof HavocStatement) || (st instanceof GotoStatement) || (st instanceof Label)
						|| (st instanceof JoinStatement) || (st instanceof CallStatement)
						|| (st instanceof ReturnStatement)|| (st instanceof AssertStatement)) {
					// do nothing
				} else {
					throw new UnsupportedOperationException(
							"Did not expect statement of type " + st.getClass().getSimpleName());
				}
			}

		}
		return result;
	}

	/**
	 * @param services
	 * @param storage
	 * @param filename
	 */
	private Script constructAndInitializeSolver(final IUltimateServiceProvider services,
			final IToolchainStorage storage, final String filename) {

		final IPreferenceProvider prefs = mServices.getPreferenceProvider(Activator.PLUGIN_ID);

		final SolverMode solverMode = prefs.getEnum(RcfgPreferenceInitializer.LABEL_SOLVER, SolverMode.class);

		final boolean fakeNonIncrementalScript =
				prefs.getBoolean(RcfgPreferenceInitializer.LABEL_FAKE_NON_INCREMENTAL_SCRIPT);

		final boolean dumpSmtScriptToFile = prefs.getBoolean(RcfgPreferenceInitializer.LABEL_DUMP_TO_FILE);
		final String pathOfDumpedScript = prefs.getString(RcfgPreferenceInitializer.LABEL_DUMP_PATH);

		final String commandExternalSolver = prefs.getString(RcfgPreferenceInitializer.LABEL_EXT_SOLVER_COMMAND);

		final boolean dumpUsatCoreTrackBenchmark =
				prefs.getBoolean(RcfgPreferenceInitializer.LABEL_DUMP_UNSAT_CORE_BENCHMARK);

		final boolean dumpMainTrackBenchmark =
				prefs.getBoolean(RcfgPreferenceInitializer.LABEL_DUMP_MAIN_TRACK_BENCHMARK);

		final String logicForExternalSolver = prefs.getString(RcfgPreferenceInitializer.LABEL_EXT_SOLVER_LOGIC);
		final SolverSettings solverSettings = SolverBuilder.constructSolverSettings(filename, solverMode,
				fakeNonIncrementalScript, commandExternalSolver, dumpSmtScriptToFile, pathOfDumpedScript);

		return SolverBuilder.buildAndInitializeSolver(services, storage, solverMode, solverSettings,
				dumpUsatCoreTrackBenchmark, dumpMainTrackBenchmark, logicForExternalSolver, "CfgBuilderScript");
	}

	/**
	 * Build a recursive control flow graph for an unstructured boogie program.
	 *
	 * @param Unit
	 *            that encodes a program.
	 * @return RootNode of a recursive control flow graph.
	 */
	public BoogieIcfgContainer createIcfg(final Unit unit) {

		mTransFormulaAdder = new TransFormulaAdder(mBoogie2smt, mServices);

		// Build entry, final and exit node for all procedures that have an
		// implementation
		for (final String procName : mBoogieDeclarations.getProcImplementation().keySet()) {
			final Body body = mBoogieDeclarations.getProcImplementation().get(procName).getBody();
			final Statement firstStatement = body.getBlock()[0];
			final BoogieIcfgLocation entryNode = new BoogieIcfgLocation(new ProcedureEntryDebugIdentifier(procName),
					procName, false, firstStatement);
			// We have to use some ASTNode for final and exit node. Let's take
			// the procedure implementation.
			final Procedure impl = mBoogieDeclarations.getProcImplementation().get(procName);
			mIcfg.getProcedureEntryNodes().put(procName, entryNode);
			final BoogieIcfgLocation finalNode =
					new BoogieIcfgLocation(new ProcedureFinalDebugIdentifier(procName), procName, false, impl);
			mIcfg.mFinalNode.put(procName, finalNode);
			final BoogieIcfgLocation exitNode =
					new BoogieIcfgLocation(new ProcedureExitDebugIdentifier(procName), procName, false, impl);
			mIcfg.getProcedureExitNodes().put(procName, exitNode);

			// new RootEdge(mGraphroot, mRootAnnot.mentryNode.get(procName));
		}

		// Build a control flow graph for each procedure
		final ProcedureCfgBuilder procCfgBuilder = new ProcedureCfgBuilder();
		for (final String procName : mBoogieDeclarations.getProcSpecification().keySet()) {
			if (mBoogieDeclarations.getProcImplementation().containsKey(procName)) {
				procCfgBuilder.buildProcedureCfgFromImplementation(procName);
			} else {
				// procCfgBuilder.buildProcedureCfgWithoutImplementation(procName);
			}
		}

		// Transform CFGs to a recursive CFG
		for (final Summary se : mImplementationSummarys) {
			addCallTransitionAndReturnTransition(se, mSimplificationTechnique);
		}

		// Add all transitions to the forked procedure entry locations.
		for (final ForkCurrentThread fct : mImplementationForkCurrentThreads ) {
			final BoogieIcfgLocation errorNode = procCfgBuilder.addErrorNode(fct.getPrecedingProcedure(), fct.getForkStatement());
			addForkOtherThreadTransition(fct, mSimplificationTechnique, errorNode);
		}

		// For each implemented procedure, add a JoinOtherThreadTransition from the exit location of the procedure
		// to all target locations of each JoinCurrentThreadEdge
		for (final String procName : mForkedProcedureNames) {
			if (mBoogieDeclarations.getProcImplementation().containsKey(procName)) {
				for (final JoinCurrentThread jot : mJoinCurrentThreads) {
					addJoinOtherThreadTransition(jot, procName, mSimplificationTechnique);
				}
			}
		}

		// mRootAnnot.mModifiableGlobalVariableManager = new ModifiableGlobalVariableManager(
		// mBoogieDeclarations.getModifiedVars(), mBoogie2smt);
		mCodeBlockSize = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
				.getEnum(RcfgPreferenceInitializer.LABEL_CODE_BLOCK_SIZE, CodeBlockSize.class);
		if (mCodeBlockSize == CodeBlockSize.LoopFreeBlock) {
			new LargeBlockEncoding();
		}

		final Set<BoogieIcfgLocation> initialNodes = mIcfg.getProcedureEntryNodes().entrySet().stream()
				.filter(a -> a.getKey().equals(ULTIMATE_START)).map(a -> a.getValue()).collect(Collectors.toSet());
		if (initialNodes.isEmpty()) {
			mLogger.info("Using library mode");
			mIcfg.getInitialNodes().addAll(mIcfg.getProcedureEntryNodes().values());
		} else {
			mLogger.info("Using the " + initialNodes.size() + " location(s) as analysis (start of procedure "
					+ ULTIMATE_START + ")");
			mIcfg.getInitialNodes().addAll(initialNodes);
		}

		return mIcfg;
	}

	private static Expression getNegation(final Expression expr) {
		if (expr == null) {
			return null;
		}
		return new UnaryExpression(expr.getLocation(), BoogieType.TYPE_BOOL, UnaryExpression.Operator.LOGICNEG, expr);
	}

	/**
	 * Add CallEdge from SummaryEdge source to the entry location of the called procedure. Add ReturnEdge from the
	 * called procedures exit node to the summary edges target.
	 *
	 * @param simplificationTechnique
	 *
	 * @param SummaryEdge
	 *            that summarizes execution of an implemented procedure.
	 */
	private void addCallTransitionAndReturnTransition(final Summary edge,
			final SimplificationTechnique simplificationTechnique) {
		final CallStatement st = edge.getCallStatement();
		final String callee = st.getMethodName();
		assert mIcfg.getProcedureEntryNodes().containsKey(callee) : "Source code contains" + " call of " + callee
		+ " but no such procedure.";

		// Add call transition from callerNode to procedures entry node
		final BoogieIcfgLocation callerNode = (BoogieIcfgLocation) edge.getSource();
		final BoogieIcfgLocation calleeEntryLoc = mIcfg.getProcedureEntryNodes().get(callee);

		final String caller = callerNode.getProcedure();

		final TranslationResult arguments2InParams =
				mIcfg.getBoogie2SMT().getStatements2TransFormula().inParamAssignment(st, simplificationTechnique);
		final TranslationResult outParams2CallerVars = mIcfg.getBoogie2SMT().getStatements2TransFormula()
				.resultAssignment(st, caller, simplificationTechnique);
		final Map<String, ILocation> overapproximations = new HashMap<>();
		overapproximations.putAll(arguments2InParams.getOverapproximations());
		overapproximations.putAll(outParams2CallerVars.getOverapproximations());
		if (!overapproximations.isEmpty()) {
			new Overapprox(overapproximations).annotate(edge);
		}

		final Call call = mCbf.constructCall(callerNode, calleeEntryLoc, st);
		call.setTransitionFormula(arguments2InParams.getTransFormula());

		final BoogieIcfgLocation returnNode = (BoogieIcfgLocation) edge.getTarget();
		final BoogieIcfgLocation calleeExitLoc = mIcfg.getProcedureExitNodes().get(callee);
		final Return returnAnnot = mCbf.constructReturn(calleeExitLoc, returnNode, call);
		returnAnnot.setTransitionFormula(outParams2CallerVars.getTransFormula());
	}




	/**
	 * Add ForkOtherThreadEdge from the ForkCurrentThreadEdge source to the entry location of the forked procedure.
	 *
	 * @param edge that points to the next step in the current thread.
	 * @param simplificationTechnique
	 */
	private void addForkOtherThreadTransition(final ForkCurrentThread forkCurrentEdge, final SimplificationTechnique simplificationTechnique,
			final BoogieIcfgLocation errorNode) {
		// FIXME Matthias 2018-08-17: check method, especially for terminology and overapproximation flags

		final ForkStatement st = forkCurrentEdge.getForkStatement();
		final String callee = st.getMethodName();
		assert mIcfg.getProcedureEntryNodes().containsKey(callee) : "Source code contains" + " fork of " + callee +  " but no such procedure.";

		// Add fork transition from callerNode to procedures entry node.
		final BoogieIcfgLocation callerNode = (BoogieIcfgLocation) forkCurrentEdge.getSource();
		final BoogieIcfgLocation calleeEntryLoc = mIcfg.getProcedureEntryNodes().get(callee);

		final TranslationResult arguments2InParams = mIcfg.getBoogie2SMT().getStatements2TransFormula()
				.inParamAssignment(st, simplificationTechnique);

		final Map<String, ILocation> overapproximations = new HashMap<>();
		overapproximations.putAll(arguments2InParams.getOverapproximations());
		if (!overapproximations.isEmpty()) {
			new Overapprox(overapproximations).annotate(forkCurrentEdge);
		}

		final ForkOtherThread fork = mCbf.constructForkOtherThread(callerNode, calleeEntryLoc, st, forkCurrentEdge);
		final UnmodifiableTransFormula parameterAssignment = arguments2InParams.getTransFormula();
		final String nameOfForkingProcedure = forkCurrentEdge.getPrecedingProcedure();

		final BoogieNonOldVar threadIdVar = mProcedureNameThreadIdMap.get(st.getMethodName());
		final UnmodifiableTransFormula forkIdAssignment = constructForkIdAssignment(threadIdVar, st.getForkID(),
				nameOfForkingProcedure, simplificationTechnique);
		final BoogieNonOldVar threadInUseVar = mProcedureNameToThreadInUseMap.get(st.getMethodName());
		final UnmodifiableTransFormula threadInUseAssignment = constructForkInUseAssignment(threadInUseVar,
				mIcfg.getCfgSmtToolkit().getManagedScript());
		final UnmodifiableTransFormula forkTransformula = TransFormulaUtils.sequentialComposition(mLogger, mServices,
				mIcfg.getCfgSmtToolkit().getManagedScript(), false, false, false,
				XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION,
				SimplificationTechnique.NONE,
				Arrays.asList(new UnmodifiableTransFormula[] {parameterAssignment, forkIdAssignment, threadInUseAssignment}));
		fork.setTransitionFormula(forkTransformula);


		// Add the assume statement for the error location and construct the
		final ILocation forkLocation = st.getLocation();
		final UnmodifiableTransFormula forkErrorTransFormula = constructThreadInUseViolationAssumption(threadInUseVar,
				mIcfg.getCfgSmtToolkit().getManagedScript());
		final Expression formula = mIcfg.getBoogie2SMT().getTerm2Expression().translate(forkErrorTransFormula.getFormula());
		final AssumeStatement assumeError = new AssumeStatement(forkLocation, formula);
		final StatementSequence errorStatement = mCbf.constructStatementSequence(callerNode, errorNode, assumeError);
		errorStatement.setTransitionFormula(forkErrorTransFormula);
	}



	/**
	 * Add JoinOtherThreadEdge from
	 *
	 * @param edge
	 * @param simplificationTechnique
	 */
	private void addJoinOtherThreadTransition(final JoinCurrentThread joinCurrentEdge, final String procName,
			final SimplificationTechnique simplificationTechnique) {
		// FIXME Matthias 2018-08-17: check method, especially for terminology and overapproximation flags
		final JoinStatement st = joinCurrentEdge.getJoinStatement();
		final BoogieIcfgLocation exitNode = mIcfg.getProcedureExitNodes().get(procName);
		final BoogieIcfgLocation callerNode = (BoogieIcfgLocation) joinCurrentEdge.getTarget();



		final BoogieNonOldVar threadInUse = mProcedureNameToThreadInUseMap.get(procName);
		final BoogieNonOldVar threadId = mProcedureNameThreadIdMap.get(procName);

		if (st.getForkID().getType() != mIcfg.getBoogie2SMT().getTypeSortTranslator().getType(threadId.getSort())) {
			return;
		}

		final String caller = callerNode.getProcedure();

		final TranslationResult outParams2CallerVars = mIcfg.getBoogie2SMT().getStatements2TransFormula()
				.resultAssignment(st, caller, procName, simplificationTechnique);
		final Map<String, ILocation> overapproximations = new HashMap<>();
		overapproximations.putAll(outParams2CallerVars.getOverapproximations());
		if (!overapproximations.isEmpty()) {
			new Overapprox(overapproximations).annotate(joinCurrentEdge);
		}

		// Create JoinOtherThread object and add TransFormulas.
		final JoinOtherThread joinOtherThread = mCbf.constructJoinOtherThread(exitNode, callerNode, st, joinCurrentEdge);

		final UnmodifiableTransFormula threadIdAssumption = constructJoinMatchingThreadIdAssumption(threadId,
				st.getForkID(), caller, simplificationTechnique);
		final UnmodifiableTransFormula threadInUseAssignment = constructThreadNotInUseAssingment(threadInUse,
				mIcfg.getCfgSmtToolkit().getManagedScript());
		final UnmodifiableTransFormula parameterAssignment = outParams2CallerVars.getTransFormula();
		final UnmodifiableTransFormula joinTransformula = TransFormulaUtils.sequentialComposition(mLogger, mServices,
				mIcfg.getCfgSmtToolkit().getManagedScript(), false, false, false,
				XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION, SimplificationTechnique.NONE,
				Arrays.asList(new UnmodifiableTransFormula[] { parameterAssignment, threadIdAssumption,
						threadInUseAssignment }));
		joinOtherThread.setTransitionFormula(joinTransformula);
	}



	/**
	 * TODO Concurrent Boogie:
	 * @param mgdScript
	 */
	private UnmodifiableTransFormula constructForkInUseAssignment(final BoogieNonOldVar threadInUseVar,
			final ManagedScript mgdScript) {
		final Map<IProgramVar, TermVariable> outVars = Collections.singletonMap(threadInUseVar,
				threadInUseVar.getTermVariable());
		final TransFormulaBuilder tfb = new TransFormulaBuilder(Collections.emptyMap(), outVars, true,
				Collections.emptySet(), true, Collections.emptySet(), true);
		tfb.setFormula(threadInUseVar.getTermVariable());
		tfb.setInfeasibility(Infeasibility.UNPROVEABLE);
		return tfb.finishConstruction(mgdScript);
	}

	/**
	 * TODO Concurrent Boogie:
	 * @param simplificationTechnique
	 */
	private UnmodifiableTransFormula constructForkIdAssignment(final BoogieNonOldVar threadIdVar,
			final Expression forkIdExpression, final String forkingProcedureId,
			final SimplificationTechnique simplificationTechnique) {
		// FIXME Matthias 2018-08-17: take care of overapproximations
		final TranslationResult test = mIcfg.getBoogie2SMT().getStatements2TransFormula()
				.forkThreadIdAssignment(threadIdVar, forkingProcedureId, forkIdExpression, simplificationTechnique);
		return test.getTransFormula();
	}

	/**
	 * TODO Concurrent Boogie:
	 * @param mgdScript
	 * @return A {@link TransFormula} that represents the assume statement {@code var == true}.
	 */
	private UnmodifiableTransFormula constructThreadInUseViolationAssumption(final BoogieNonOldVar threadInUseVar,
			final ManagedScript mgdScript) {
		final Map<IProgramVar, TermVariable> inVars = Collections.singletonMap(threadInUseVar,
				threadInUseVar.getTermVariable());
		final Map<IProgramVar, TermVariable> outVars = inVars;
		final TransFormulaBuilder tfb = new TransFormulaBuilder(inVars, outVars, true,
				Collections.emptySet(), true, Collections.emptySet(), true);
		tfb.setFormula(threadInUseVar.getTermVariable());
		tfb.setInfeasibility(Infeasibility.UNPROVEABLE);
		return tfb.finishConstruction(mgdScript);
	}

	/**
	 * TODO Concurrent Boogie:
	 * @param joiningThreadProcedureId
	 * @param simplificationTechnique
	 */
	private UnmodifiableTransFormula constructJoinMatchingThreadIdAssumption(final BoogieNonOldVar threadIdVar,
			final Expression joinedThreadIdExpression, final String joiningThreadProcedureId,
			final SimplificationTechnique simplificationTechnique) {
		// FIXME Matthias 2018-08-17: take care of overapproximations
		final TranslationResult test = mIcfg.getBoogie2SMT().getStatements2TransFormula().joinThreadIdAssumption(
				threadIdVar, joiningThreadProcedureId, joinedThreadIdExpression, simplificationTechnique);
		return test.getTransFormula();
	}

	/**
	 * TODO Concurrent Boogie:
	 * @param mgdScript
	 * @return A {@link TransFormula} that represents the assignment statement
	 *         {@code var := false}.
	 */
	private UnmodifiableTransFormula constructThreadNotInUseAssingment(final BoogieNonOldVar threadInUseVar,
			final ManagedScript mgdScript) {
		final Map<IProgramVar, TermVariable> outVars = Collections.singletonMap(threadInUseVar,
				threadInUseVar.getTermVariable());
		final TransFormulaBuilder tfb = new TransFormulaBuilder(Collections.emptyMap(), outVars, true,
				Collections.emptySet(), true, Collections.emptySet(), true);
		tfb.setFormula(SmtUtils.not(mgdScript.getScript(), threadInUseVar.getTermVariable()));
		tfb.setInfeasibility(Infeasibility.UNPROVEABLE);
		return tfb.finishConstruction(mgdScript);
	}

	/**
	 * Build control flow graph of single procedures.
	 *
	 * @author heizmann@informatik.uni-freiburg.de
	 */
	private final class ProcedureCfgBuilder {

		/**
		 * Maps a position identifier to the LocNode that represents this position in the CFG.
		 */
		private Map<DebugIdentifier, BoogieIcfgLocation> mProcLocNodes;

		/**
		 * Maps a Label identifier to the LocNode that represents this Label in the CFG.
		 */
		private Map<DebugIdentifier, BoogieIcfgLocation> mLabel2LocNodes;

		/**
		 * Set of all labels that occurred in the procedure. If an element is inserted twice this is an error.
		 */
		private Set<String> mLabels;

		/**
		 * Name of that last Label for which we constructed a LocNode
		 */
		private DebugIdentifier mLastLabelName;

		/**
		 * Distance to the last LocNode that was constructed as representative of a label.
		 */
		// int mlocSuffix;

		/**
		 * Element at which we continue building the CFG. This should be a - LocNode if the last processed Statement was
		 * a Label or a CallStatement - TransEdge if the last processed Statement was Assume, Assignment, Havoc or
		 * Assert. - null if the last processed Statement was Goto or Return.
		 */
		IElement mCurrent;

		/**
		 * True only if the current code is deadcode. E.g., if there was a goto or return but not yet a label.
		 */
		boolean mDeadcode;

		/**
		 * List of auxiliary edges, which represent Gotos and get removed later.
		 */
		List<GotoEdge> mGotoEdges;

		/**
		 * Name of the procedure for which the CFG is build (at the moment)
		 */
		String mCurrentProcedureName;

		/**
		 * The last processed Statement. This is only used in assertions to
		 */
		Statement mLastStmt = new Label(null, null);

		/**
		 * The non goto edges of this procedure.
		 */
		Set<CodeBlock> mEdges;

		Map<Integer, Integer> mNameCache;

		/**
		 * Builds the control flow graph of a single procedure according to a given implementation.
		 *
		 * @param Identifier
		 *            of the procedure for which the CFG will be build.
		 */
		private void buildProcedureCfgFromImplementation(final String procName) {
			mCurrentProcedureName = procName;
			mEdges = new HashSet<>();
			mGotoEdges = new LinkedList<>();
			mLabels = new HashSet<>();
			mNameCache = new HashMap<>();

			final Statement[] statements =
					mBoogieDeclarations.getProcImplementation().get(procName).getBody().getBlock();
			if (statements.length == 0) {
				throw new UnsupportedOperationException("Procedure contains no statement");
			}

			mLabel2LocNodes = new HashMap<>();

			// initialize the Map from labels to LocNodes for this procedure
			mProcLocNodes = new HashMap<>();
			mIcfg.getProgramPoints().put(procName, mProcLocNodes);

			mLogger.debug("Start construction of the CFG for" + procName);
			{
				// first LocNode is the entry node of the procedure
				final BoogieIcfgLocation locNode = mIcfg.getProcedureEntryNodes().get(procName);
				mLastLabelName = locNode.getDebugIdentifier();
				// mlocSuffix = 0;
				mProcLocNodes.put(mLastLabelName, locNode);
				mCurrent = locNode;
			}
			assumeRequires(false);

			for (final Statement st : statements) {

				if (!mServices.getProgressMonitorService().continueProcessing()) {
					mLogger.warn("Timeout while constructing control flow graph");
					throw new ToolchainCanceledException(this.getClass(),
							"constructing CFG for procedure with " + statements.length + "statements");
				}

				final ILocation loc = st.getLocation();
				assert loc != null : "location of the following statement is null " + st;

				if (st instanceof Label) {
					if (mCurrent instanceof BoogieIcfgLocation) {
						assert mCurrent == mIcfg.getProcedureEntryNodes().get(procName)
								|| mLastStmt instanceof Label : "If st is Label"
								+ " and mcurrent is LocNode lastSt is Label";
						mLogger.debug("Two Labels in a row: " + mCurrent + " and " + ((Label) st).getName() + "."
								+ " I am expecting that at least one was" + " introduced by the user (or vcc). In the"
								+ " CFG only the first label of those two (or" + " more) will be used");
					}
					if (mCurrent instanceof CodeBlock) {
						assert mLastStmt instanceof AssumeStatement || mLastStmt instanceof AssignmentStatement
						|| mLastStmt instanceof HavocStatement || mLastStmt instanceof AssertStatement
						|| mLastStmt instanceof CallStatement : "If st"
						+ " is a Label and the last constructed node"
						+ " was a TransEdge, then the last"
						+ " Statement must not be a Label, Return or" + " Goto";
					mLogger.warn("Label in the middle of a codeblock.");
					}

					processLabel((Label) st);
				}

				else if (st instanceof AssumeStatement || st instanceof AssignmentStatement
						|| st instanceof HavocStatement) {
					if (mCurrent instanceof CodeBlock) {
						assert mLastStmt instanceof AssumeStatement || mLastStmt instanceof AssignmentStatement
						|| mLastStmt instanceof HavocStatement || mLastStmt instanceof AssertStatement
						|| mLastStmt instanceof CallStatement : "If the"
						+ " last constructed node is a TransEdge, then"
						+ " the last Statement must not be a Label,"
						+ " Return or Goto. (i.e. this is not the first" + " Statemnt of the block)";
					}
					processAssuAssiHavoStatement(st, Origin.IMPLEMENTATION);
				}

				else if (st instanceof AssertStatement) {
					if (mCurrent instanceof CodeBlock) {
						assert mLastStmt instanceof AssumeStatement || mLastStmt instanceof AssignmentStatement
						|| mLastStmt instanceof HavocStatement || mLastStmt instanceof AssertStatement
						|| mLastStmt instanceof CallStatement : "If the"
						+ " last constructed node is a TransEdge, then"
						+ " the last Statement must not be a Label,"
						+ " Return or Goto. (i.e. this is not the first" + " Statement of the block)";
					}
					processAssertStatement((AssertStatement) st);
				}

				else if (st instanceof GotoStatement) {
					// assert (! (mLastSt instanceof GotoStatement)) :
					// "Two Gotos in a row";
					if (mLastStmt instanceof GotoStatement) {
						mLogger.warn("Two Gotos in a row! There was dead code");
					} else {
						processGotoStatement((GotoStatement) st);
					}
				}

				else if (st instanceof CallStatement) {
					if (mCurrent instanceof CodeBlock) {
						assert mLastStmt instanceof AssumeStatement || mLastStmt instanceof AssignmentStatement
						|| mLastStmt instanceof HavocStatement || mLastStmt instanceof AssertStatement
						|| mLastStmt instanceof CallStatement : "If mcurrent is a TransEdge, then lastSt"
						+ " must not be a Label, Return or Goto."
						+ " (i.e. this is not the first Statemnt" + " of the block)";
					}
					if (mCurrent instanceof BoogieIcfgLocation) {
						assert mLastStmt instanceof Label
						|| mLastStmt instanceof CallStatement : "If mcurrent is LocNode, then st is"
						+ " first statement of a block or fist" + " statement after a call";
					}
					processCallStatement((CallStatement) st);
				}

				else if (st instanceof ReturnStatement) {
					processReturnStatement();
				}
				else if (st instanceof ForkStatement) {
					processForkStatement((ForkStatement) st);
				}
				else if (st instanceof JoinStatement) {
					processJoinStatement((JoinStatement) st);
				}
				else {
					throw new UnsupportedOperationException("At the moment"
							+ " only Labels, Assert, Assume, Assignment, Havoc" + " and Goto statements are supported");
				}
				mLastStmt = st;
			}

			// If there is no ReturnStatement at the end of the procedure act
			// like there would have been one.
			if (!(mLastStmt instanceof ReturnStatement)) {
				processReturnStatement();
			}

			// Assume that the procedures final node may be reachable
			mDeadcode = false;

			assertAndAssumeEnsures();

			// Remove auxiliary GotoTransitions
			final boolean removeGotoEdges = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
					.getBoolean(RcfgPreferenceInitializer.LABEL_REMOVE_GOTO_EDGES);
			if (removeGotoEdges) {
				mLogger.debug("Starting removal of auxiliaryGotoTransitions");
				while (!mGotoEdges.isEmpty()) {
					final GotoEdge gotoEdge = mGotoEdges.remove(0);
					final boolean wasRemoved = removeAuxiliaryGoto(gotoEdge, true);
					assert wasRemoved : "goto not removed";
				}
			} else {
				for (final GotoEdge gotoEdge : mGotoEdges) {
					final boolean wasRemoved = removeAuxiliaryGoto(gotoEdge, false);
					if (!wasRemoved) {
						mEdges.add(gotoEdge);
					}
				}
			}

			for (final CodeBlock transEdge : mEdges) {
				mTransFormulaAdder.addTransitionFormulas(transEdge, procName, mXnfConversionTechnique,
						mSimplificationTechnique);
			}
			// mBoogie2smt.removeLocals(proc);
		}

		/**
		 * construct error location BoogieASTNode in procedure procName add constructed location to mprocLocNodes and
		 * mErrorNodes.
		 *
		 * @return
		 */
		private BoogieIcfgLocation addErrorNode(final String procName, final BoogieASTNode boogieASTNode) {
			Set<BoogieIcfgLocation> errorNodes = mIcfg.getProcedureErrorNodes().get(procName);
			final int locNodeNumber;
			if (errorNodes == null) {
				errorNodes = new HashSet<>();
				mIcfg.getProcedureErrorNodes().put(procName, errorNodes);
				locNodeNumber = 0;
			} else {
				locNodeNumber = errorNodes.size();
			}

			final ProcedureErrorType type;
			if (boogieASTNode instanceof AssertStatement) {
				type = ProcedureErrorType.ASSERT_VIOLATION;
			} else if (boogieASTNode instanceof EnsuresSpecification) {
				type = ProcedureErrorType.ENSURES_VIOLATION;
			} else if (boogieASTNode instanceof CallStatement) {
				type = ProcedureErrorType.REQUIRES_VIOLATION;
			} else if (boogieASTNode instanceof ForkStatement) {
				type = ProcedureErrorType.INUSE_VIOLATION;
			} else {
				throw new IllegalArgumentException();
			}

			final ProcedureErrorDebugIdentifier errorLocLabel;
			final Check check = Check.getAnnotation(boogieASTNode);
			if (check != null) {
				errorLocLabel = new ProcedureErrorWithCheckDebugIdentifier(procName, locNodeNumber, type, check);
			} else {
				errorLocLabel = new ProcedureErrorDebugIdentifier(procName, locNodeNumber, type);
			}
			final BoogieIcfgLocation errorLocNode =
					new BoogieIcfgLocation(errorLocLabel, procName, true, boogieASTNode);
			if (check != null) {
				check.annotate(errorLocNode);
			}
			mProcLocNodes.put(errorLocLabel, errorLocNode);
			errorNodes.add(errorLocNode);
			return errorLocNode;
		}

		/**
		 * @return List of {@code EnsuresSpecification}s that contains only one {@code EnsuresSpecification} which is
		 *         true.
		 */
		private List<EnsuresSpecification> getDummyEnsuresSpecifications(final ILocation loc) {
			final Expression dummyExpr = new BooleanLiteral(loc, BoogieType.TYPE_BOOL, true);
			final EnsuresSpecification dummySpec = new EnsuresSpecification(loc, false, dummyExpr);
			final ArrayList<EnsuresSpecification> dummySpecs = new ArrayList<>(1);
			dummySpecs.add(dummySpec);
			return dummySpecs;
		}

		/**
		 * @return List of {@code RequiresSpecification}s that contains only one {@code RequiresSpecification} which is
		 *         true.
		 */
		private List<RequiresSpecification> getDummyRequiresSpecifications() {
			final Expression dummyExpr = new BooleanLiteral(null, BoogieType.TYPE_BOOL, true);
			final RequiresSpecification dummySpec = new RequiresSpecification(null, false, dummyExpr);
			final ArrayList<RequiresSpecification> dummySpecs = new ArrayList<>(1);
			dummySpecs.add(dummySpec);
			return dummySpecs;
		}

		/**
		 * Remove GotoEdge from a CFG. If allowMultiplicationOfEdges is false, we try to remove the goto by only merging
		 * locations. This is not always possible hence there is no guarantee that the goto is removed. If
		 * allowMultiplicationOfEdges is true, we guarantee that the goto is removed but in some cases will not only
		 * merge locations but also multiply existing edges.
		 *
		 * @return true iff we removed the gotoEdge.
		 */
		private boolean removeAuxiliaryGoto(final GotoEdge gotoEdge, final boolean allowMultiplicationOfEdges) {
			final BoogieIcfgLocation mother = (BoogieIcfgLocation) gotoEdge.getSource();
			final BoogieIcfgLocation child = (BoogieIcfgLocation) gotoEdge.getTarget();

			// Target of a goto should never be an error location.
			// If this assertion will fail some day. A fix might be that
			// mother has to become an error location.
			assert !child.isErrorLocation();

			for (final IcfgEdge grandchild : child.getOutgoingEdges()) {
				if (grandchild instanceof Call) {
					mLogger.warn("Will not remove gotoEdge" + gotoEdge + "since this would involve adding/removing call"
							+ "and return edges and bring my naive goto"
							+ " replacing algorithm into terrible trouble");
					return false;
				}
			}

			mLogger.debug("Removed GotoEdge from" + mother + " to " + child);
			if (mother == child) {
				mother.removeOutgoing(gotoEdge);
				gotoEdge.setSource(null);
				gotoEdge.setTarget(null);
				child.removeIncoming(gotoEdge);
				mLogger.debug("GotoEdge was selfloop");
				return true;
			}
			assert !child.getIncomingEdges().isEmpty() : "there should be at least the goto that might be removed";
			assert !mother.getOutgoingEdges().isEmpty() : "there should be at least the goto that might be removed";
			if (child.getIncomingEdges().size() == 1 || mother.getOutgoingEdges().size() == 1) {
				mother.removeOutgoing(gotoEdge);
				gotoEdge.setSource(null);
				gotoEdge.setTarget(null);
				child.removeIncoming(gotoEdge);

				// transfer goto-loop annotations to the outgoing edges of child
				for (final IcfgEdge out : child.getOutgoingEdges()) {
					ModelUtils.copyAnnotations(gotoEdge, out, LoopEntryAnnotation.class);
					// TODO: Where to put the LoopExitAnnotation
					ModelUtils.copyAnnotations(gotoEdge, out, LoopExitAnnotation.class);
				}

				mLogger.debug(mother + " has no sucessors any more or " + child + "has no predecessors any more.");
				mLogger.debug(child + " gets absorbed by " + mother);
				mergeLocNodes(child, mother);
				return true;
			}
			if (allowMultiplicationOfEdges) {
				mother.removeOutgoing(gotoEdge);
				gotoEdge.setSource(null);
				gotoEdge.setTarget(null);
				child.removeIncoming(gotoEdge);
				// Not allowed to merge mother and child in this case
				mLogger.debug(child + " has " + child.getIncomingEdges().size() + " predecessors," + " namely "
						+ child.getIncomingNodes());
				mLogger.debug(mother + " has " + mother.getIncomingEdges().size() + " successors" + ", namely "
						+ mother.getOutgoingNodes());
				mLogger.debug("Adding for every successor transition of " + child
						+ " a copy of that transition as successor of " + mother);
				for (final IcfgEdge grandchild : child.getOutgoingEdges()) {
					final BoogieIcfgLocation target = (BoogieIcfgLocation) grandchild.getTarget();
					final CodeBlock edge = mCbf.copyCodeBlock((CodeBlock) grandchild, mother, target);
					// transfer goto-loop annotations to the duplicated edges
					ModelUtils.copyAnnotations(gotoEdge, edge, LoopEntryAnnotation.class);
					ModelUtils.copyAnnotations(gotoEdge, edge, LoopExitAnnotation.class);
					if (edge instanceof GotoEdge) {
						mGotoEdges.add((GotoEdge) edge);
					} else {
						mEdges.add(edge);
					}
				}
				return true;
			}
			return false;
		}

		/**
		 * Assert the ensures clause. For each ensures clause expr
		 * <ul>
		 * <li>append {@code assume (expr)} between the finalNode and the exitNode of the procedure</li> add an edge
		 * labeled with {@code assume (not expr)} from the final Node to the errorNode
		 */
		private void assertAndAssumeEnsures() {
			// Assume the ensures specification at the end of the procedure.
			List<EnsuresSpecification> ensures = mBoogieDeclarations.getEnsures().get(mCurrentProcedureName);
			if (ensures == null || ensures.isEmpty()) {
				final Procedure proc = mBoogieDeclarations.getProcSpecification().get(mCurrentProcedureName);
				ensures = getDummyEnsuresSpecifications(proc.getLocation());
			}
			final BoogieIcfgLocation finalNode = mIcfg.mFinalNode.get(mCurrentProcedureName);
			mLastLabelName = finalNode.getDebugIdentifier();
			mProcLocNodes.put(mLastLabelName, finalNode);
			// mlocSuffix = 0;
			mCurrent = finalNode;

			for (final EnsuresSpecification spec : ensures) {
				final AssumeStatement st = new AssumeStatement(spec.getLocation(), spec.getFormula());
				ModelUtils.copyAnnotations(spec, st);
				mBacktranslator.putAux(st, new BoogieASTNode[] { spec });
				processAssuAssiHavoStatement(st, Origin.ENSURES);
				mLastStmt = st;
			}
			final BoogieIcfgLocation exitNode = mIcfg.getProcedureExitNodes().get(mCurrentProcedureName);
			mLastLabelName = exitNode.getDebugIdentifier();
			mProcLocNodes.put(mLastLabelName, exitNode);
			((CodeBlock) mCurrent).connectTarget(exitNode);

			// Violations against the ensures part of the procedure
			// specification
			final List<EnsuresSpecification> ensuresNonFree =
					mBoogieDeclarations.getEnsuresNonFree().get(mCurrentProcedureName);
			if (ensuresNonFree != null && !ensuresNonFree.isEmpty()) {
				for (final EnsuresSpecification spec : ensuresNonFree) {
					final Expression specExpr = spec.getFormula();
					AssumeStatement assumeSt;
					assumeSt = new AssumeStatement(spec.getLocation(), getNegation(specExpr));
					final Statement st = assumeSt;
					ModelUtils.copyAnnotations(spec, st);
					mBacktranslator.putAux(assumeSt, new BoogieASTNode[] { spec });
					final BoogieIcfgLocation errorLocNode = addErrorNode(mCurrentProcedureName, spec);
					final CodeBlock assumeEdge =
							mCbf.constructStatementSequence(finalNode, errorLocNode, assumeSt, Origin.ENSURES);
					ModelUtils.copyAnnotations(spec, assumeEdge);
					ModelUtils.copyAnnotations(spec, errorLocNode);
					mEdges.add(assumeEdge);
				}
			}
		}

		/**
		 * Assume the requires clause. If the requires clause is empty and dummyRequiresIfEmpty is true add an dummy
		 * requires specification.
		 */
		private void assumeRequires(final boolean dummyRequiresIfEmpty) {
			// Assume everything mentioned in the requires specification
			List<RequiresSpecification> requires = mBoogieDeclarations.getRequires().get(mCurrentProcedureName);
			if ((requires == null || requires.isEmpty()) && dummyRequiresIfEmpty) {
				requires = getDummyRequiresSpecifications();
			}
			if (requires != null && !requires.isEmpty()) {
				for (final RequiresSpecification spec : requires) {
					final AssumeStatement st = new AssumeStatement(spec.getLocation(), spec.getFormula());
					ModelUtils.copyAnnotations(spec, st);
					mBacktranslator.putAux(st, new BoogieASTNode[] { spec });
					processAssuAssiHavoStatement(st, Origin.REQUIRES);
					mLastStmt = st;
				}
			}
		}

		private DebugIdentifier constructLocDebugIdentifier(final Statement stmt) {
			final ILocation location = stmt.getLocation();
			final int startLine = location.getStartLine();
			Integer value = mNameCache.get(startLine);
			if (value == null) {
				value = 0;
			} else {
				value = value + 1;
			}
			mNameCache.put(startLine, value);
			final LoopEntryAnnotation lea = LoopEntryAnnotation.getAnnotation(stmt);
			if (lea != null && lea.getLoopEntryType() == LoopEntryType.WHILE) {
				return new LoopEntryDebugIdentifier(startLine, value.intValue());
			}
			return new OrdinaryDebugIdentifier(startLine, value.intValue());
		}

		/**
		 * Get the LocNode that represents a label. If there is already a LocNode that represents this Label return this
		 * representative. Otherwise construct a new LocNode that becomes the representative for this label.
		 *
		 * @param labelId
		 *            {@link DebugIdentifier} of the Label for which you want the corresponding LocNode.
		 * @param st
		 *            Statement whose (Ultimate) Location should be added to this LocNode. If this method is called
		 *            while processing a GotoStatement the Statement can be set to null, since the Location will be
		 *            overwritten, when this method is called with the correct Label as second parameter.
		 * @return LocNode that is the representative for labelName.
		 */
		private BoogieIcfgLocation getLocNodeForLabel(final DebugIdentifier labelId, final Statement st) {
			final ILocation loc = st.getLocation();
			final LoopEntryAnnotation lea = LoopEntryAnnotation.getAnnotation(st);
			BoogieIcfgLocation locNode = mLabel2LocNodes.get(labelId);
			if (locNode != null) {
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("LocNode for " + labelId + " already" + " constructed, namely: " + locNode);
				}
				if (st instanceof Label && locNode.getDebugIdentifier() == labelId) {

					loc.annotate(locNode);
					if (lea != null && lea.getLoopEntryType() == LoopEntryType.WHILE) {
						if (mLogger.isDebugEnabled()) {
							mLogger.debug("LocNode does not have to Location of the while loop" + st.getLocation());
						}
						mIcfg.getLoopLocations().add(locNode);
					}
				}
				ModelUtils.copyAnnotations(st, locNode);
				return locNode;
			}
			locNode = new BoogieIcfgLocation(labelId, mCurrentProcedureName, false, st);
			mLabel2LocNodes.put(labelId, locNode);
			mProcLocNodes.put(labelId, locNode);
			if (mLogger.isDebugEnabled()) {
				mLogger.debug("LocNode for " + labelId + " has not" + " existed yet. Constructed it");
			}
			if (lea != null && lea.getLoopEntryType() == LoopEntryType.WHILE) {
				mIcfg.getLoopLocations().add(locNode);
			}
			return locNode;
		}

		private void processLabel(final Label st) {
			final String labelName = st.getName();
			final boolean existsAlready = !mLabels.add(labelName);
			if (existsAlready) {
				throw new AssertionError("Label " + labelName + " occurred twice");
			}
			final StringDebugIdentifier tmpLabelIdentifier = new StringDebugIdentifier(labelName);
			if (mCurrent instanceof BoogieIcfgLocation) {
				// from now on this label is represented by mcurrent

				final BoogieIcfgLocation oldNodeForLabel = mLabel2LocNodes.get(tmpLabelIdentifier);
				if (oldNodeForLabel != null) {
					mergeLocNodes(oldNodeForLabel, (BoogieIcfgLocation) mCurrent);
				}
				mLabel2LocNodes.put(tmpLabelIdentifier, (BoogieIcfgLocation) mCurrent);
			} else {
				mLastLabelName = tmpLabelIdentifier;
				// mlocSuffix = 0;

				// is there already a LocNode that represents this
				// label? (This can be the case if this label was destination
				// of a goto statement) If not construct the LocNode.
				// If yes, add the Location Object to the existing LocNode.
				final BoogieIcfgLocation locNode = getLocNodeForLabel(tmpLabelIdentifier, st);

				if (mCurrent instanceof CodeBlock) {
					((IcfgEdge) mCurrent).setTarget(locNode);
					locNode.addIncoming((CodeBlock) mCurrent);
				}
				mCurrent = locNode;
			}
			mDeadcode = false;
		}

		private void processAssuAssiHavoStatement(final Statement st, final Origin origin) {
			if (mDeadcode) {
				return;
			}
			if (mCurrent instanceof BoogieIcfgLocation) {
				final StatementSequence codeBlock =
						mCbf.constructStatementSequence((BoogieIcfgLocation) mCurrent, null, st, origin);
				ModelUtils.copyAnnotations(st, codeBlock);
				mEdges.add(codeBlock);
				mCurrent = codeBlock;
			} else if (mCurrent instanceof CodeBlock) {
				if (mCodeBlockSize == CodeBlockSize.SequenceOfStatements
						|| mCodeBlockSize == CodeBlockSize.LoopFreeBlock) {
					final StatementSequence stSeq = (StatementSequence) mCurrent;
					stSeq.addStatement(st);
					ModelUtils.copyAnnotations(st, stSeq);
				} else {
					final DebugIdentifier locName = constructLocDebugIdentifier(st);
					final BoogieIcfgLocation locNode =
							new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
					((CodeBlock) mCurrent).connectTarget(locNode);
					mProcLocNodes.put(locName, locNode);
					final StatementSequence codeBlock = mCbf.constructStatementSequence(locNode, null, st, origin);
					ModelUtils.copyAnnotations(st, codeBlock);
					mEdges.add(codeBlock);
					mCurrent = codeBlock;
				}
			} else {
				// mcurrent must either be LocNode or TransEdge
				throw new IllegalArgumentException();
			}

		}

		private void processAssertStatement(final AssertStatement st) {
			if (mDeadcode) {
				return;
			}
			if (mCurrent instanceof CodeBlock) {
				final DebugIdentifier locName = constructLocDebugIdentifier(st);
				final BoogieIcfgLocation locNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
				((CodeBlock) mCurrent).connectTarget(locNode);
				mProcLocNodes.put(locName, locNode);
				mCurrent = locNode;
			}
			final BoogieIcfgLocation locNode = (BoogieIcfgLocation) mCurrent;
			final Expression assertion = st.getFormula();
			final AssumeStatement assumeError = new AssumeStatement(st.getLocation(), getNegation(assertion));
			ModelUtils.copyAnnotations(st, assumeError);
			mBacktranslator.putAux(assumeError, new BoogieASTNode[] { st });
			final BoogieIcfgLocation errorLocNode = addErrorNode(mCurrentProcedureName, st);
			final StatementSequence assumeErrorCB =
					mCbf.constructStatementSequence(locNode, errorLocNode, assumeError, Origin.ASSERT);
			ModelUtils.copyAnnotations(st, errorLocNode);
			ModelUtils.copyAnnotations(st, assumeErrorCB);
			mEdges.add(assumeErrorCB);
			AssumeStatement assumeSafe = new AssumeStatement(st.getLocation(), assertion);
			if (mAddAssumeForEachAssert) {
				assumeSafe = new AssumeStatement(st.getLocation(), assertion);
			} else {
				// we cannot omit this assume(true) because if the assert is
				// the last node of the procedure the final location will be
				// merged with the last location. In this case this would be
				// the predecessor of the assume(!expression).
				// Hence the error location would be erroneously reachable from
				// the final location.
				assumeSafe = new AssumeStatement(st.getLocation(),
						new BooleanLiteral(st.getLocation(), BoogieType.TYPE_BOOL, true));
			}
			final Statement st1 = assumeSafe;
			ModelUtils.copyAnnotations(st, st1);
			mBacktranslator.putAux(assumeSafe, new BoogieASTNode[] { st });
			final StatementSequence assumeSafeCB =
					mCbf.constructStatementSequence(locNode, null, assumeSafe, Origin.ASSERT);
			ModelUtils.copyAnnotations(st, assumeSafeCB);
			// add a new TransEdge labeled with st as successor of the
			// last constructed LocNode
			mEdges.add(assumeSafeCB);
			mCurrent = assumeSafeCB;
		}

		private void processGotoStatement(final GotoStatement st) {
			if (mDeadcode) {
				return;
			}
			final String[] targets = st.getLabels();
			assert targets.length != 0 : "Goto must have at least one target";
			mLogger.debug("Goto statement with " + targets.length + " targets.");
			BoogieIcfgLocation locNode;
			if (mCurrent instanceof CodeBlock) {
				final DebugIdentifier locName = constructLocDebugIdentifier(st);
				locNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
				((CodeBlock) mCurrent).connectTarget(locNode);
				mProcLocNodes.put(locName, locNode);
			} else if (mCurrent instanceof BoogieIcfgLocation) {
				locNode = (BoogieIcfgLocation) mCurrent;
			} else {
				// mcurrent must either LocNode or TransEdge
				throw new IllegalArgumentException();

			}
			for (final String label : targets) {
				// Add an auxiliary GotoEdge and a LocNode
				// for each target of the GotoStatement.
				final BoogieIcfgLocation targetLocNode = getLocNodeForLabel(new StringDebugIdentifier(label), st);
				final GotoEdge newGotoEdge = mCbf.constructGotoEdge(locNode, targetLocNode);
				ModelUtils.copyAnnotations(st, newGotoEdge);
				mGotoEdges.add(newGotoEdge);
			}
			// We have not constructed a new node that should be used in the
			// next iteration step, therefore setting mcurrent to null.
			mCurrent = null;
			mDeadcode = true;
		}



		private void processCallStatement(final CallStatement st) {
			if (mDeadcode) {
				return;
			}
			BoogieIcfgLocation locNode;
			if (mCurrent instanceof CodeBlock) {
				final DebugIdentifier locName = constructLocDebugIdentifier(st);
				locNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
				((CodeBlock) mCurrent).connectTarget(locNode);
				mProcLocNodes.put(locName, locNode);
			} else if (mCurrent instanceof BoogieIcfgLocation) {
				locNode = (BoogieIcfgLocation) mCurrent;
			} else {
				// mcurrent must be either LocNode or TransEdge
				throw new IllegalArgumentException();
			}
			final DebugIdentifier locName = constructLocDebugIdentifier(st);
			final BoogieIcfgLocation returnNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
			mProcLocNodes.put(locName, returnNode);
			// add summary edge
			final String callee = st.getMethodName();
			Summary summaryEdge;
			if (mBoogieDeclarations.getProcImplementation().containsKey(callee)) {
				summaryEdge = mCbf.constructSummary(locNode, returnNode, st, true);
				final IIcfgElement cb = summaryEdge;
				ModelUtils.copyAnnotations(st, cb);
				mImplementationSummarys.add(summaryEdge);
			} else {
				summaryEdge = mCbf.constructSummary(locNode, returnNode, st, false);
				final IIcfgElement cb = summaryEdge;
				ModelUtils.copyAnnotations(st, cb);
			}
			mEdges.add(summaryEdge);
			mCurrent = returnNode;

			// Violations against the requires part of the procedure
			// specification. Omit intruduction of these additional auxiliary
			// assert statements if current procedure is START_PROCEDURE.
			//

			// in fork throw unsuportedOperationException
			final List<RequiresSpecification> requiresNonFree = mBoogieDeclarations.getRequiresNonFree().get(callee);
			if (requiresNonFree != null && !requiresNonFree.isEmpty()) {
				for (final RequiresSpecification spec : requiresNonFree) {
					// use implementation if available and specification
					// otherwise. To use the implementation is important in
					// cases where signature of procedure and implementation are
					// different.
					Procedure proc;
					if (mBoogieDeclarations.getProcImplementation().containsKey(callee)) {
						proc = mBoogieDeclarations.getProcImplementation().get(callee);
					} else {
						proc = mBoogieDeclarations.getProcSpecification().get(callee);
					}
					final Expression violatedRequires =
							getNegation(new WeakestPrecondition(spec.getFormula(), st, proc).getResult());
					AssumeStatement assumeSt;
					assumeSt = new AssumeStatement(st.getLocation(), violatedRequires);
					final Statement st1 = assumeSt;
					ModelUtils.copyAnnotations(st, st1);
					mBacktranslator.putAux(assumeSt, new BoogieASTNode[] { st, spec });
					final BoogieIcfgLocation errorLocNode = addErrorNode(mCurrentProcedureName, st);
					final StatementSequence errorCB =
							mCbf.constructStatementSequence(locNode, errorLocNode, assumeSt, Origin.REQUIRES);
					ModelUtils.copyAnnotations(spec, errorCB);
					ModelUtils.copyAnnotations(spec, errorLocNode);
					mEdges.add(errorCB);
				}
			}
		}

		// FIXME problem if last statement is goto
		// fixed on 16.05.2011 - still needs to be tested
		private void processReturnStatement() {
			if (mDeadcode) {
				return;
			}
			// If mcurrent is a transition add as successor the final Node
			// of this procedure.
			// If mcurrent is a location replace it with the final Node of
			// this procedure.
			final BoogieIcfgLocation finalNode = mIcfg.mFinalNode.get(mCurrentProcedureName);
			if (mCurrent instanceof CodeBlock) {
				final CodeBlock transEdge = (CodeBlock) mCurrent;
				transEdge.connectTarget(finalNode);
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("Constructed TransEdge " + transEdge + "as predecessr of " + mIcfg.mFinalNode);
				}
			} else if (mCurrent instanceof BoogieIcfgLocation) {
				mergeLocNodes((BoogieIcfgLocation) mCurrent, finalNode);
				if (mLogger.isDebugEnabled()) {
					mLogger.debug("Replacing " + mCurrent + " by " + finalNode);
				}
			} else {
				// mcurrent must be either LocNode or TransEdge
				// s_Logger.warn("Last location of " + mcurrentProcedureName +
				// "not reachable");
				throw new IllegalArgumentException();
			}
			// No new nodes created, set mcurrent to null
			mCurrent = null;
			mDeadcode = true;
		}

		/**
		 * This function creates a new location in the cfg and connects it with a forkCurrentThread transition.
		 *
		 * @param st
		 */
		private void processForkStatement(final ForkStatement st) {
			if (mDeadcode) { return; }

			BoogieIcfgLocation locNode;
			if (mCurrent instanceof CodeBlock) {
				final DebugIdentifier locName = constructLocDebugIdentifier(st);
				locNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
				((CodeBlock) mCurrent).connectTarget(locNode);
				mProcLocNodes.put(locName, locNode);
			} else if (mCurrent instanceof BoogieIcfgLocation) {
				locNode = (BoogieIcfgLocation) mCurrent;
			} else {
				throw new IllegalArgumentException();
			}
			final DebugIdentifier locName = constructLocDebugIdentifier(st);

			final BoogieIcfgLocation forkCurrentNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
			mProcLocNodes.put(locName, forkCurrentNode);

			final String callee = st.getMethodName();
			ForkCurrentThread forkCurrentThreadEdge;
			if (mBoogieDeclarations.getProcImplementation().containsKey(callee)) {
				forkCurrentThreadEdge = mCbf.constructForkCurrentThread(locNode, forkCurrentNode, st, true);
				final IIcfgElement cb = forkCurrentThreadEdge;
				ModelUtils.copyAnnotations(st, cb);
				mImplementationForkCurrentThreads.add(forkCurrentThreadEdge);
				mForkedProcedureNames.add(st.getMethodName());
			} else {
				forkCurrentThreadEdge = mCbf.constructForkCurrentThread(locNode, forkCurrentNode, st, false);
				final IIcfgElement cb = forkCurrentThreadEdge;
				ModelUtils.copyAnnotations(st, cb);
			}
			mEdges.add(forkCurrentThreadEdge);
			mCurrent = forkCurrentNode;
		}

		/**
		 * This function creates a new location in the cfg and connects it with a joinCurrentThread transition.
		 *
		 * @param st
		 */
		private void processJoinStatement(final JoinStatement st) {
			if (mDeadcode) { return; }

			BoogieIcfgLocation locNode;
			if (mCurrent instanceof CodeBlock) {
				final DebugIdentifier locName = constructLocDebugIdentifier(st);
				locNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
				((CodeBlock) mCurrent).connectTarget(locNode);
				mProcLocNodes.put(locName, locNode);
			} else if (mCurrent instanceof BoogieIcfgLocation) {
				locNode = (BoogieIcfgLocation) mCurrent;
			} else {
				throw new IllegalArgumentException();
			}
			final DebugIdentifier locName = constructLocDebugIdentifier(st);

			final BoogieIcfgLocation joinCurrentNode = new BoogieIcfgLocation(locName, mCurrentProcedureName, false, st);
			mProcLocNodes.put(locName, joinCurrentNode);

			final JoinCurrentThread joinCurrentThreadEdge = mCbf.constructJoinCurrentThread(locNode, joinCurrentNode, st);
			final IIcfgElement cb = joinCurrentThreadEdge;
			ModelUtils.copyAnnotations(st, cb);
			mJoinCurrentThreads.add(joinCurrentThreadEdge);

			mEdges.add(joinCurrentThreadEdge);
			mCurrent = joinCurrentNode;
		}

		/**
		 * Merge one LocNode into another. The oldLocNode will be merged into the newLocNode. The newLocNode gets
		 * connected to all incoming/outgoing transitions of the oldLocNode. The oldLocNode looses connections to all
		 * incoming/outgoing transitions. If the oldLocNode was representative for a Label the new location will from
		 * now on be the representative of this Label.
		 *
		 * @param oldLocNode
		 *            LocNode that gets merged into the newLocNode. Must not represent an error location.
		 * @param newLocNode
		 *            LocNode that absorbes the oldLocNode.
		 */
		private void mergeLocNodes(final BoogieIcfgLocation oldLocNode, final BoogieIcfgLocation newLocNode) {
			// oldLocNode must not represent an error location
			assert !oldLocNode.isErrorLocation();
			if (oldLocNode == newLocNode) {
				return;
			}

			for (final IcfgEdge transEdge : oldLocNode.getIncomingEdges()) {
				transEdge.setTarget(newLocNode);
				newLocNode.addIncoming(transEdge);
			}
			oldLocNode.clearIncoming();
			for (final IcfgEdge transEdge : oldLocNode.getOutgoingEdges()) {
				transEdge.setSource(newLocNode);
				newLocNode.addOutgoing(transEdge);
			}
			oldLocNode.clearOutgoing();

			mProcLocNodes.remove(oldLocNode.getDebugIdentifier());

			// If the LocNode that should be replaced was constructed for a
			// label it is in mlocNodeOf and the representative for this label
			// should be updated accordingly.
			if (mLabel2LocNodes.containsKey(oldLocNode.getDebugIdentifier())) {
				mLabel2LocNodes.put(oldLocNode.getDebugIdentifier(), newLocNode);
			}

			assert oldLocNode != mIcfg.getProcedureExitNodes().get(mCurrentProcedureName);
			if (oldLocNode == mIcfg.getProcedureEntryNodes().get(mCurrentProcedureName)) {
				mIcfg.getProcedureEntryNodes().put(mCurrentProcedureName, newLocNode);
			}
			if (mIcfg.getLoopLocations().remove(oldLocNode)) {
				// if the old location was a loop location, the new one is also
				mIcfg.getLoopLocations().add(newLocNode);
			}
			ModelUtils.copyAnnotations(oldLocNode, newLocNode);
		}
	}

	private class LargeBlockEncoding {

		Set<BoogieIcfgLocation> mSequentialQueue = new HashSet<>();
		Map<BoogieIcfgLocation, List<CodeBlock>> mParallelQueue = new HashMap<>();
		final boolean mSimplifyCodeBlocks;

		public LargeBlockEncoding() {
			mSimplifyCodeBlocks = mServices.getPreferenceProvider(Activator.PLUGIN_ID)
					.getBoolean(RcfgPreferenceInitializer.LABEL_SIMPLIFY);

			for (final String proc : mIcfg.getProgramPoints().keySet()) {
				for (final DebugIdentifier position : mIcfg.getProgramPoints().get(proc).keySet()) {
					final BoogieIcfgLocation pp = mIcfg.getProgramPoints().get(proc).get(position);
					if (superfluousSequential(pp)) {
						mSequentialQueue.add(pp);
					} else {
						final List<CodeBlock> list = superfluousParallel(pp);
						if (list != null) {
							mParallelQueue.put(pp, list);
						}
					}
				}
			}
			while (!mSequentialQueue.isEmpty() || !mParallelQueue.isEmpty()) {
				if (!mSequentialQueue.isEmpty()) {
					final BoogieIcfgLocation superfluousPP = mSequentialQueue.iterator().next();
					mSequentialQueue.remove(superfluousPP);
					composeSequential(superfluousPP);
				} else {
					final Entry<BoogieIcfgLocation, List<CodeBlock>> superfluous =
							mParallelQueue.entrySet().iterator().next();
					final BoogieIcfgLocation pp = superfluous.getKey();
					final List<CodeBlock> outgoing = superfluous.getValue();
					mParallelQueue.remove(pp);
					composeParallel(pp, outgoing);
				}
			}
		}

		private void composeSequential(final BoogieIcfgLocation pp) {
			assert pp.getIncomingEdges().size() == 1;
			assert pp.getOutgoingEdges().size() == 1;
			final CodeBlock incoming = (CodeBlock) pp.getIncomingEdges().get(0);
			final CodeBlock outgoing = (CodeBlock) pp.getOutgoingEdges().get(0);
			final BoogieIcfgLocation predecessor = (BoogieIcfgLocation) incoming.getSource();
			final BoogieIcfgLocation successor = (BoogieIcfgLocation) outgoing.getTarget();
			final List<CodeBlock> sequence = new ArrayList<>(2);
			sequence.add(incoming);
			sequence.add(outgoing);
			mCbf.constructSequentialComposition(predecessor, successor, mSimplifyCodeBlocks, false, sequence,
					mXnfConversionTechnique, mSimplificationTechnique);
			if (!mSequentialQueue.contains(predecessor)) {
				final List<CodeBlock> outEdges = superfluousParallel(predecessor);
				if (outEdges != null) {
					mParallelQueue.put(predecessor, outEdges);
				}
			}
		}

		private void composeParallel(final BoogieIcfgLocation pp, final List<CodeBlock> outgoing) {
			final BoogieIcfgLocation successor = (BoogieIcfgLocation) outgoing.get(0).getTarget();
			mCbf.constructParallelComposition(pp, successor, Collections.unmodifiableList(outgoing),
					mXnfConversionTechnique, mSimplificationTechnique);
			if (superfluousSequential(pp)) {
				mSequentialQueue.add(pp);
			} else {
				final List<CodeBlock> list = superfluousParallel(pp);
				if (list != null) {
					mParallelQueue.put(pp, list);
				}
			}
			if (superfluousSequential(successor)) {
				mSequentialQueue.add(successor);
			}
		}

		private boolean superfluousSequential(final BoogieIcfgLocation pp) {
			if (pp.getIncomingEdges().size() != 1) {
				return false;
			}
			if (pp.getOutgoingEdges().size() != 1) {
				return false;
			}
			final IcfgEdge incoming = pp.getIncomingEdges().get(0);
			if (incoming instanceof RootEdge) {
				return false;
			}
			if (incoming instanceof Call) {
				return false;
			}
			assert incoming instanceof StatementSequence || incoming instanceof SequentialComposition
			|| incoming instanceof ParallelComposition || incoming instanceof Summary
			|| incoming instanceof GotoEdge;
			final IcfgEdge outgoing = pp.getOutgoingEdges().get(0);
			if (outgoing instanceof IIcfgForkTransitionCurrentThread
					|| outgoing instanceof IIcfgForkTransitionOtherThread
					|| outgoing instanceof IIcfgJoinTransitionCurrentThread
					|| outgoing instanceof IIcfgJoinTransitionOtherThread) {
				throw new IllegalStateException(
						"fork and join should never be part of a composition. Are you accidentally using a block encoding that is not suitable for concurrent programs?");
			}
			if (outgoing instanceof Return) {
				return false;
			}
			assert outgoing instanceof StatementSequence || outgoing instanceof SequentialComposition
			|| outgoing instanceof ParallelComposition || outgoing instanceof Summary
			|| outgoing instanceof GotoEdge;
			return true;
		}

		/**
		 * Check if ProgramPoint pp has several outgoing edges whose target is the same ProgramPoint.
		 *
		 * @return For some successor ProgramPoint the list of all outgoing edges whose target is this (successor)
		 *         ProgramPoint, if there can be such a list with more than one element. Otherwise (each outgoing edge
		 *         leads to a different ProgramPoint) return null.
		 */
		private List<CodeBlock> superfluousParallel(final BoogieIcfgLocation pp) {
			List<CodeBlock> result = null;
			final Map<BoogieIcfgLocation, List<CodeBlock>> succ2edge = new HashMap<>();
			for (final IcfgEdge edge : pp.getOutgoingEdges()) {
				if (!(edge instanceof Return) && !(edge instanceof Summary)) {
					final CodeBlock cb = (CodeBlock) edge;
					final BoogieIcfgLocation succ = (BoogieIcfgLocation) cb.getTarget();
					List<CodeBlock> edges = succ2edge.get(succ);
					if (edges == null) {
						edges = new ArrayList<>();
						succ2edge.put(succ, edges);
					}
					edges.add(cb);
					if (result == null && edges.size() > 1) {
						result = edges;
					}
				}
			}
			return result;
		}
	}
}
