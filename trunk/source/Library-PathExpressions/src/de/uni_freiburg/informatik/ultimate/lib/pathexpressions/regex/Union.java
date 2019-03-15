/*
 * Code taken from https://github.com/johspaeth/PathExpression
 * Copyright (C) 2018 Johannes Spaeth
 * Copyright (C) 2018 Fraunhofer IEM, Paderborn, Germany
 * 
 * Copyright (C) 2019 Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE Library-PathExpressions plug-in.
 *
 * The ULTIMATE Library-PathExpressions plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Library-PathExpressions plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-PathExpressions plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-PathExpressions plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-PathExpressions plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lib.pathexpressions.regex;

import java.util.Objects;

import de.uni_freiburg.informatik.ultimate.lib.pathexpressions.IRegex;

public class Union<V> implements IRegex<V> {
	private final IRegex<V> b;
	private final IRegex<V> a;

	public Union(IRegex<V> a, IRegex<V> b) {
		assert a != null;
		assert b != null;
		this.a = a;
		this.b = b;
	}

	public String toString() {
		return "{" + Objects.toString(a, "null") + " U " + Objects.toString(b, "null") + "}";
	}

	public IRegex<V> getFirst() {
		return a;
	}

	public IRegex<V> getSecond() {
		return b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hashCode(a, b));
		return result;
	}

	private int hashCode(IRegex<V> a, IRegex<V> b) {
		if (a == null && b == null)
			return 1;
		if (a == null)
			return b.hashCode();
		if (b == null)
			return a.hashCode();
		return a.hashCode() + b.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Union other = (Union) obj;
		if (matches(a, other.a)) {
			return matches(b, other.b);
		}
		if (matches(a, other.b)) {
			return matches(b, other.a);
		}
		return false;
	}

	private boolean matches(IRegex<V> a, IRegex<V> b) {
		if (a == null) {
			if (b != null)
				return false;
			return true;
		}
		return a.equals(b);
	}

}