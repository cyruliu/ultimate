/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Core.
 * 
 * The ULTIMATE Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Core. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Core, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Core grant you additional permission 
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.model.annotation;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.model.IElement;

/**
 * 
 * Annotation for assume statements. This marks whether an assume stems from a (loop, if) condition or its negation
 * after unstructuring.
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class ConditionAnnotation extends ModernAnnotations {

	private static final long serialVersionUID = 1L;
	private static final String KEY = Activator.PLUGIN_ID + "_Condition";

	@Visualizable
	private final boolean mIsNegated;

	public ConditionAnnotation(final boolean isNegated) {
		mIsNegated = isNegated;
	}

	public boolean isNegated() {
		return mIsNegated;
	}

	public void annotate(IElement node) {
		node.getPayload().getAnnotations().put(KEY, this);
	}

	public static ConditionAnnotation getAnnotation(final IElement node) {
		return ModernAnnotations.getAnnotation(node, KEY, a -> (ConditionAnnotation) a);
	}
	
	@Override
	public String toString() {
		return "IsNegated=" + String.valueOf(mIsNegated);
	}
}
