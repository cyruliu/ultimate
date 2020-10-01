/*
 * Copyright (C) 2020 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.concurrency;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.ITransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Marking;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.IIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.BasicPredicateFactory;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;


/**
 * TODO
 * 
 * @author Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * @author Miriam Lagunes (miriam.lagunes@students.uni-freiburg.de)
 * 
 *
 * @param <PLACE>
 */
public class OwickiGriesConstruction<LOC extends IcfgLocation, PLACE> {	
	
	private final IPetriNet<IIcfgTransition<LOC>, PLACE> mNet; 	
	private final Map<Marking<IIcfgTransition<LOC>, PLACE>, IPredicate> mFloydHoareAnnotation;
	
	private final OwickiGriesAnnotation<IIcfgTransition<LOC>, PLACE> mAnnotation;
	private final IUltimateServiceProvider mServices;
	private final ManagedScript mManagedScript;
	private final Script mScript;
	private final BasicPredicateFactory mFactory;
	private final IIcfgSymbolTable mSymbolTable;
	
	//Variables for Annotation construction
	private final Map<PLACE, IProgramVar> mGhostVariables;
	private final Map<PLACE, IPredicate> mFormulaMapping;
	private final Set<UnmodifiableTransFormula> mGhostInitAssignment;
	private final Map<ITransition<IIcfgTransition<LOC>, PLACE>,UnmodifiableTransFormula> mAssignmentMapping;
	
	public OwickiGriesConstruction(IUltimateServiceProvider services, CfgSmtToolkit csToolkit,
			IPetriNet<IIcfgTransition<LOC>, PLACE> net,
			Map<Marking<IIcfgTransition<LOC>, PLACE>, IPredicate> floydHoare) {			
			mNet = net;				
			mFloydHoareAnnotation = floydHoare;			
			mScript = null; 
			mManagedScript = csToolkit.getManagedScript();
			mSymbolTable = csToolkit.getSymbolTable();
			mServices = services;
			mFactory = new BasicPredicateFactory(mServices, mManagedScript,
				csToolkit.getSymbolTable());
			
			
			mGhostVariables = getGhostVariables();
			mFormulaMapping = getFormulaMapping();
			mGhostInitAssignment = getGhostInitAssignment();
			mAssignmentMapping = getAssignmentMapping();
			
			mAnnotation = new OwickiGriesAnnotation<>();
			
			//TODO: Cambiar esto a mandarlo a Annotation constructor, notas en Annotation class	 
	}	 

	/**
	 * Predicate: disjunction of Markings predicate.
	 * Markings predicate: Conjunction of GhostVariable and FH predicate.
	 * @return a Map with a predicate for each place in Net.
	 */	
	public Map<PLACE, IPredicate> getFormulaMapping () {
		Map<PLACE, IPredicate> Mapping = new HashMap<PLACE, IPredicate>();	     
	    for (PLACE place: mNet.getPlaces()) {
	    	Set<IPredicate> Clauses = new HashSet<>();	    	  	
	    	mFloydHoareAnnotation.forEach((key,value)-> {
	    	if(mFloydHoareAnnotation.containsKey(place)) {
	    			Clauses.add(getMarkingPredicate(place, key));}});
	    	Mapping.put(place, mFactory.or(Clauses)); }	    	
		return Mapping;	
	}
	
	/**
	 * @param place
	 * @param marking
	 * @return Predicate with conjunction of Ghost variables and predicate of marking
	 */
	private  IPredicate getMarkingPredicate(PLACE place, Marking<IIcfgTransition<LOC>, PLACE> marking) {
		//TODO: Conjunction of variables not in Marking 
		//OptionA: Just Marking shared variables, Which variables?: Find if marking is subset of another.
			//If marking if subset of another marking in FHAnn, get variables and set to false from SuperMarking\marking.
		//OptionB: Negation of all other Ghost variables not in Marking.
			//Complement set: GhostVariables(Only for places/ only construction??)\marking places
		
		//TODO:Formula Type: Conjunction and Implication		
		
		Set<IPredicate> terms =  new HashSet<>(); 
		marking.forEach(element -> terms.add(getGhostPredicate(element))); //GhostVariables of places in marking		
		terms.addAll(getAllNotMarking(marking)); //OptionB
		terms.add(mFloydHoareAnnotation.get(place)); //Predicate of marking	
		return  mFactory.and(terms);		
	}
	
	private Set<IPredicate> getAllNotMarking(Marking<IIcfgTransition<LOC>, PLACE> marking){
	//Formula MethodB: GhostVariables of all other places not in marking
		Set<IPredicate> predicates = new HashSet<>();
	    Collection<PLACE> notMarking = mNet.getPlaces();
		notMarking.removeAll(marking.stream().collect(Collectors.toSet()));		
	    notMarking.forEach(element -> predicates.add(mFactory.not(getGhostPredicate(element))));
	    return predicates;		
	}
	
	private Set<IPredicate> getSubsetMarking(Marking<IIcfgTransition<LOC>, PLACE> marking){
	//Formula MethodB: GhostVariables of all other places not in marking
		
		Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		//Get all Supersets of Marking
		mFloydHoareAnnotation.keySet().forEach(marking -> );
		Set<IPredicate> predicates = new HashSet<>();
	    Collection<PLACE> notMarking = mNet.getPlaces();
		notMarking.removeAll(markPlaces);		
	    notMarking.forEach(element -> predicates.add(mFactory.not(getGhostPredicate(element))));
	    return predicates;		
	}
	
	
	/** 
	 * @param place
	 * @return Predicate place's GhostVariable
	 */
	private IPredicate getGhostPredicate(PLACE place) {
	  return mFactory.newPredicate(mAnnotation.mGhostVariables.get(place).getTerm());
	 }
	
	/**
	 * @return Map of GhostVariables to Places
	 */
	private Map<PLACE, IProgramVar> getGhostVariables(){
		Map<PLACE, IProgramVar> GhostVars = new HashMap<PLACE, IProgramVar>();
		int i = 0;
		for(PLACE place: mNet.getPlaces()) {
			final TermVariable tVar = mManagedScript.constructFreshTermVariable
					("np_" + i, SmtSortUtils.getBoolSort(mManagedScript));
			final IProgramVar pVar = ProgramVarUtils.constructGlobalProgramVarPair
					(tVar.getName(), SmtSortUtils.getBoolSort(mManagedScript), mManagedScript, this);	
			GhostVars.put(place, pVar);
			i++;
		}
		return GhostVars;
	}
	
	/**
	 * @return set of Initial value assignment of all GhostVariables.
	 * TODO: result: Set with twoTransFormula, or a single Transformula?
	 * 
	 */
	private Set<UnmodifiableTransFormula> getGhostInitAssignment(){		
		Set<UnmodifiableTransFormula> InitAssignments = new HashSet<>();		
		Collection<IProgramVar> InitGhostVariables = new HashSet<>();//Get all GhostVariables from Initial places
		mNet.getInitialPlaces().forEach(place -> 
		InitGhostVariables.add(mGhostVariables.get(place)));
		Collection<IProgramVar> NotInitGhostVariables = mGhostVariables.values();
		NotInitGhostVariables.removeAll(InitGhostVariables); //Ghost variables of not Initial places
		InitAssignments.add(getGhostAssignment(InitGhostVariables, "true"));
		InitAssignments.add(getGhostAssignment(NotInitGhostVariables, "false"));
		return InitAssignments;
	}
	
	/**
	 * 
	 * @param place
	 * @return assignment of the place's GhostVariable.
	 */
	private UnmodifiableTransFormula getGhostAssignment(Collection<IProgramVar> vars, String term){
		return  TransFormulaBuilder.constructAssignment(new ArrayList<>(vars),
						Collections.nCopies(vars.size(), mScript.term(term)), mSymbolTable, mManagedScript);
	}	

	/**
	 * 
	 * @return Map of Places' Ghost Variables assignments to Transitions
	 * 
	 */
	
	private Map<ITransition<IIcfgTransition<LOC>, PLACE>,UnmodifiableTransFormula> getAssignmentMapping(){
		Map<ITransition<IIcfgTransition<LOC>,PLACE>,UnmodifiableTransFormula> AssignmentMapping = 
				new HashMap <ITransition<IIcfgTransition<LOC>,PLACE>, UnmodifiableTransFormula>();
		Collection<ITransition<IIcfgTransition<LOC>,PLACE>> Transitions = mNet.getTransitions();
		Transitions.forEach(transition -> AssignmentMapping.put(transition, getTransitionAssignment(transition)));				
		return AssignmentMapping;
	}
	
	/**
	 * 
	 * @param transition
	 * @return TransFormula of sequential compositions of GhostVariables assignments.
	 * GhostVariables of Predecessors Places are assign to false,
	 * GhostVariables of Successors Places are assign to true.
	 */
	private UnmodifiableTransFormula getTransitionAssignment(ITransition<IIcfgTransition<LOC>,PLACE> transition) {			
		List<UnmodifiableTransFormula> assignments = new ArrayList<>();
		Set<PLACE> Places = mNet.getPredecessors(transition);
		Places.forEach(place -> assignments.add
				(getGhostAssignment(Collections.nCopies(1,mGhostVariables.get(place)),"false")));
		Places = mNet.getSuccessors(transition);	
		Places.forEach(place -> assignments.add
				(getGhostAssignment(Collections.nCopies(1,mGhostVariables.get(place)),"true")));
		return TransFormulaUtils.sequentialComposition(null, mServices, mManagedScript, 
				false, false, false, null, null, assignments);			
	}
	
	public OwickiGriesAnnotation<IIcfgTransition<LOC>, PLACE> getResult() {
		return mAnnotation;
	}
	
	
}
