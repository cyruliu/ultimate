/*
 * Copyright (C) 2016 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automata Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.ICsvProviderProvider;
import de.uni_freiburg.informatik.ultimate.util.csv.SimpleCsvProvider;

/**
 * Object that stores statistics of an automata library operation.
 * Stores a single row of a CSV as a key-value map.
 * 
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 */
public class AutomataOperationStatistics implements ICsvProviderProvider<Object> {
	
	private final LinkedHashMap<StatisticsType, Object> mKeyValueMap = new LinkedHashMap<>();
	
	@Override
	public ICsvProvider<Object> createCvsProvider() {
		final List<String> columnTitles = new ArrayList<>(mKeyValueMap.size());
		final List<Object> firstRow = new ArrayList<>(mKeyValueMap.size());
		for (final Entry<StatisticsType, Object> entry : mKeyValueMap.entrySet()) {
			columnTitles.add(entry.getKey().toString());
			firstRow.add(entry.getValue());
		}
		final SimpleCsvProvider<Object> result = new SimpleCsvProvider<>(columnTitles);
		result.addRow(firstRow);
		return result;
	}
	
	/**
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with key, or null if there was no
	 *         mapping for key.
	 */
	public Object addKeyValuePair(final StatisticsType key, final Object value) {
		return mKeyValueMap.put(key, value);
	}
}
