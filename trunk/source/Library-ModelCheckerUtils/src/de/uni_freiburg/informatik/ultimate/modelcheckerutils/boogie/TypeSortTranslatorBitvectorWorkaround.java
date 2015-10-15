/*
 * Copyright (C) 2015 Thomas Lang
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 * 
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie;

import java.math.BigInteger;
import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.boogie.type.PrimitiveBoogieType;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.model.IType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.TypeDeclaration;

/**
 * Translate integers to bit vectors, otherwise call TypeSortTranslator.
 * 
 * @author Thomas Lang
 *
 */
public class TypeSortTranslatorBitvectorWorkaround extends TypeSortTranslator {

	public TypeSortTranslatorBitvectorWorkaround(
			Collection<TypeDeclaration> declarations, Script script,
			boolean blackHoleArrays, IUltimateServiceProvider services) {
		super(declarations, script, blackHoleArrays, services);
	}

	protected Sort constructSort(IType boogieType, BoogieASTNode BoogieASTNode) {
		if (boogieType.equals(PrimitiveBoogieType.intType)) {
			BigInteger[] sortIndices = { BigInteger.valueOf(32) };
			return m_Script.sort("BitVec", sortIndices);
		} else {
			return super.constructSort(boogieType, BoogieASTNode);
		}
	}
}
