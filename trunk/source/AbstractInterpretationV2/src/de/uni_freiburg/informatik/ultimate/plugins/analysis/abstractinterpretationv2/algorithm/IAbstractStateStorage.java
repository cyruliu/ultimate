/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractState;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 *
 * @param <ACTION>
 * @param <VARDECL>
 */
public interface IAbstractStateStorage<ACTION, VARDECL> {

	Collection<IAbstractState<ACTION, VARDECL>> getAbstractPreStates(ACTION transition);

	Collection<IAbstractState<ACTION, VARDECL>> getAbstractPostStates(ACTION transition);

	IAbstractState<ACTION, VARDECL> getCurrentAbstractPreState(ACTION transition);

	IAbstractState<ACTION, VARDECL> getCurrentAbstractPostState(ACTION transition);

	void addAbstractPreState(ACTION transition, IAbstractState<ACTION, VARDECL> state);

	void addAbstractPostState(ACTION transition, IAbstractState<ACTION, VARDECL> state);
	
	IAbstractState<ACTION, VARDECL> setPostStateIsFixpoint(ACTION transition, IAbstractState<ACTION, VARDECL> state,
			boolean value);

	IAbstractState<ACTION, VARDECL> mergePostStates(ACTION transition);
	
	IAbstractStateStorage<ACTION, VARDECL> createStorage();

}
