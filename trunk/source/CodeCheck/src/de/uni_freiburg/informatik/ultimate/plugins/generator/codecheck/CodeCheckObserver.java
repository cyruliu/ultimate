package de.uni_freiburg.informatik.ultimate.plugins.generator.codecheck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;


import de.uni_freiburg.informatik.ultimate.access.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.access.WalkerOptions;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.core.api.UltimateServices;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootAnnot;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Summary;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.PreferenceValues.Solver;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.TraceChecker;

import org.apache.log4j.Logger;
//import org.eclipse.swt.program.Program;
import org.eclipse.core.commands.common.AbstractNamedHandleEvent;
import org.eclipse.ui.internal.handlers.ReuseEditorTester;


/**
 * Auto-Generated Stub for the plug-in's Observer
 */

enum Checker {
	ULTIMATE, IMPULSE
}

enum Result { CORRECT, TIMEOUT , MAXEDITERATIONS , UNKNOWN , INCORRECT }

public class CodeCheckObserver implements IUnmanagedObserver {

	private Checker checker;
	public static Logger s_Logger = UltimateServices.getInstance().getLogger(Activator.s_PLUGIN_ID);
	private CodeChecker codeChecker;
	
	public RootNode m_originalRoot;
	public TAPreferences m_taPrefs;
	
	public SmtManager m_smtManager;
	public IPredicate m_truePredicate;
	public IPredicate m_falsePredicate;
	
	public ImpRootNode m_graphRoot;

	private static final boolean DEBUG = false;
	
	private PrintWriter dumpInitialize() { // copied from Impulse, needed for trace checker
		File file = 
				new File(m_taPrefs.dumpPath() + "/" + ".txt");
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(file);
			return new PrintWriter(fileWriter);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return null;
	}
	/**
	 * Given a graph root, copy all the nodes and the corresponding connections.
	 * @param root
	 * @return
	 */
	public ImpRootNode copyGraph(ImpRootNode root) {
		HashMap <AnnotatedProgramPoint, AnnotatedProgramPoint> copy = new HashMap<AnnotatedProgramPoint, AnnotatedProgramPoint>();
		ImpRootNode newRoot = new ImpRootNode(root.getRootAnnot());
		copy.put(root, newRoot);
		Stack <AnnotatedProgramPoint> stack = new Stack<AnnotatedProgramPoint>();
		for (AnnotatedProgramPoint child : root.getOutgoingNodes()) {
			stack.add(child);
		}
		while (!stack.isEmpty()) {
			AnnotatedProgramPoint current = stack.pop();
			if (copy.containsKey(current))
				continue;
			copy.put(current, new AnnotatedProgramPoint(current));
			List <AnnotatedProgramPoint> nextNodes = current.getOutgoingNodes();
			for (AnnotatedProgramPoint nextNode : nextNodes) {
				if (!copy.containsKey(nextNode)) {
					stack.add(nextNode);
				}
			}
		}
		for (AnnotatedProgramPoint node : copy.keySet()) {
			AnnotatedProgramPoint newNode = copy.get(node);
			List <AnnotatedProgramPoint> nextNodes = node.getOutgoingNodes();
			for (AnnotatedProgramPoint nextNode : nextNodes) {
				AnnotatedProgramPoint nextNewNode = copy.get(nextNode);
				newNode.connectTo(nextNewNode, node.getOutgoingEdgeLabel(nextNode));
			}
		}
		for (AnnotatedProgramPoint node : copy.keySet()) {
			AnnotatedProgramPoint[] returnNodes = node.m_outgoingReturnAppToCallPreds.keySet().toArray(new AnnotatedProgramPoint[]{});
			for (AnnotatedProgramPoint returnNode : returnNodes) {
				for (AnnotatedProgramPoint callPred : node.getCallPredsOfOutgoingReturnTarget(returnNode)) {
					copy.get(node).addOutGoingReturnCallPred(copy.get(returnNode), copy.get(callPred));
				}
			}
		}
		return newRoot;
	}
	
	/**
	 * Initialize all the required objects in the implementation.
	 * @param root
	 * @return
	 */
	public boolean initialize(IElement root) {
		m_originalRoot = (RootNode) root;
		RootAnnot rootAnnot = m_originalRoot.getRootAnnot();
		m_taPrefs = rootAnnot.getTaPrefs();
		m_smtManager = new SmtManager(rootAnnot.getBoogie2Smt(),
				Solver.SMTInterpol, rootAnnot.getGlobalVars(),
				rootAnnot.getModGlobVarManager(), false, "");

		m_truePredicate = m_smtManager.newTruePredicate();
		m_falsePredicate = m_smtManager.newFalsePredicate();
		RCFG2AnnotatedRCFG r2ar = new RCFG2AnnotatedRCFG(m_smtManager);
		m_graphRoot = r2ar.convert(m_originalRoot, m_truePredicate);
		removeSummaryEdges();
		if (checker == Checker.IMPULSE) {
			codeChecker = new ImpulseChecker(root, m_smtManager, m_truePredicate, m_falsePredicate, m_taPrefs, m_originalRoot, m_graphRoot);
		} else {
			codeChecker = new UltimateChecker(root, m_smtManager, m_truePredicate, m_falsePredicate, m_taPrefs, m_originalRoot, m_graphRoot);
		}
		return false;
	}
	
	private void removeSummaryEdges() {
		Stack <AnnotatedProgramPoint> stack = new Stack<AnnotatedProgramPoint>();
		HashSet<AnnotatedProgramPoint> visited = new HashSet<AnnotatedProgramPoint>();
		visited.add(m_graphRoot);
		stack.add(m_graphRoot);
		while(!stack.isEmpty()) {
			AnnotatedProgramPoint node = stack.pop();
			AnnotatedProgramPoint[] successors = node.getOutgoingNodes().toArray(new AnnotatedProgramPoint[]{});
			for (AnnotatedProgramPoint successor : successors) {
				if (node.getOutgoingEdgeLabel(successor) instanceof Summary) {
					node.removeOutgoingNode(successor);
					successor.removeIncomingNode(node);
				}
				if(!visited.contains(successor)) {
					visited.add(successor);
					stack.add(successor);
				}
			}
		}
	}
	
	public boolean process(IElement root) {
		//FIXME
		checker = Checker.IMPULSE;
		initialize(root);
		
		final boolean loop_forever = true; // for DEBUG
		final int iterationsLimit = 0; // for DEBUG
		
		ImpRootNode originalGraphCopy = copyGraph(m_graphRoot);
		//m_graphRoot.addOutgoingNode(originalGraphCopy, new DummyCodeBlock());
		//originalGraphCopy.addIncomingNode(m_graphRoot);

		int noOfProcedures = m_graphRoot.getOutgoingNodes().size();
		
		for (int procID = 0; procID < noOfProcedures; ++procID) {
			AnnotatedProgramPoint procRoot = m_graphRoot.getOutgoingNodes().get(procID);
			s_Logger.debug("Exploring : " + procRoot);
			Stack <AnnotatedProgramPoint> stack = new Stack <AnnotatedProgramPoint>();
			stack.add(procRoot);
			EmptinessCheck emptinessCheck = new EmptinessCheck();
			int iterationsCount = 0; // for DEBUG
			if (DEBUG)
				codeChecker.debug();
			while (loop_forever | iterationsCount++ < iterationsLimit) {
				s_Logger.debug(String.format("Iterations = %d\n", iterationsCount));
				if (stack.isEmpty()) {
					s_Logger.info("This Program is SAFE, Check terminated with " + iterationsCount + " iterations.");
					break;
				}
				AnnotatedProgramPoint procedureRoot = stack.peek();
				Pair<AnnotatedProgramPoint[], NestedWord<CodeBlock>> errorTrace = 
						emptinessCheck.checkForEmptiness(procedureRoot);
				
				if (errorTrace == null) {
					// if an error trace doesn't exist, return safe
					stack.pop();
					continue;
				} else {
					s_Logger.info("Error Path is FOUND.");
					TraceChecker traceChecker = new TraceChecker(
							m_truePredicate, // checks whether the trace is feasible, i.e. the formula is satisfiable
							m_falsePredicate,  //return LBool.UNSAT if trace is infeasible
							errorTrace.getSecond(),
							m_smtManager, 
							m_originalRoot.getRootAnnot().getModGlobVarManager(), 
							dumpInitialize());
					LBool isSafe = traceChecker.isCorrect();
					if(isSafe == LBool.UNSAT) { //trace is infeasible
						PredicateUnifier pu = new PredicateUnifier(m_smtManager, m_truePredicate, m_falsePredicate);
						traceChecker.computeInterpolants(new TraceChecker.AllIntegers(), pu);
						IPredicate[] interpolants = traceChecker.getInterpolants();
						codeChecker.codeCheck(errorTrace, interpolants, procedureRoot);
					} else { // trace is feasible
						s_Logger.info("This program is UNSAFE, Check terminated with " + iterationsCount + " iterations.");
						if (DEBUG)
							codeChecker.debug();
						return false; //break
					}
				}
				if(DEBUG)
					codeChecker.debug();
			}
			m_graphRoot = copyGraph(originalGraphCopy);
			codeChecker.m_graphRoot = m_graphRoot;
		}

		if(DEBUG)
			codeChecker.debug();
		
		return false;
	}
	// Debug
	
	public ImpRootNode getRoot() {
		return codeChecker.m_graphRoot;
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	@Override
	public WalkerOptions getWalkerOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performedChanges() {
		// TODO Auto-generated method stub
		return false;
	}
}