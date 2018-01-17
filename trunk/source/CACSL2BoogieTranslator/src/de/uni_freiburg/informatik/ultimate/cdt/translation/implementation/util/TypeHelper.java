/*
 * Copyright (C) 2018 Yannick Bühler
 * Copyright (C) 2018 University of Freiburg
 *
 * This file is part of the ULTIMATE CACSL2BoogieTranslator plug-in.
 *
 * The ULTIMATE CACSL2BoogieTranslator plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE CACSL2BoogieTranslator plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CACSL2BoogieTranslator plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CACSL2BoogieTranslator plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE CACSL2BoogieTranslator plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.FlatSymbolTable;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.UnsupportedSyntaxException;

/**
 * @author Yannick Bühler
 * @since 2018-01-15
 */
public class TypeHelper {

	public static CType typeFromSpecifier(final FlatSymbolTable fs, final IASTDeclSpecifier ds) {
		if (ds instanceof IASTSimpleDeclSpecifier) {
			return new CPrimitive(ds);
		} else if (ds instanceof IASTNamedTypeSpecifier) {
			final IASTNamedTypeSpecifier nts = (IASTNamedTypeSpecifier) ds;
			final String rslvName = fs.applyMultiparseRenaming(ds.getContainingFilename(), nts.getName().toString());
			if (!fs.containsCSymbol(ds, rslvName)) {
				throw new UnsupportedSyntaxException(null, "Typedef used before collected");
			}
			return fs.findCSymbol(ds, rslvName).getCDecl().getType();
		}
		throw new UnsupportedOperationException("only simple decl specifiers are currently supported");
	}
}
