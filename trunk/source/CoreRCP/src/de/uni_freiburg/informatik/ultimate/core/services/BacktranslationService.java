/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.core.services;

import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.services.model.IBacktranslatedCFG;
import de.uni_freiburg.informatik.ultimate.core.services.model.IBacktranslationService;
import de.uni_freiburg.informatik.ultimate.core.services.model.IStorable;
import de.uni_freiburg.informatik.ultimate.core.services.model.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.model.ITranslator;
import de.uni_freiburg.informatik.ultimate.result.model.IProgramExecution;

/**
 * 
 * @author dietsch@informatik.uni-freiburg.de
 * 
 */
public class BacktranslationService implements IStorable, IBacktranslationService {

	private static final String sKey = "BacktranslationService";
	private ModelTranslationContainer mTranslatorSequence;

	public BacktranslationService() {
		mTranslatorSequence = new ModelTranslationContainer();
	}

	@Override
	public <STE, TTE, SE, TE> void addTranslator(ITranslator<STE, TTE, SE, TE> translator) {
		mTranslatorSequence.addTranslator(translator);
	}

	@Override
	public <SE, TE> TE translateExpression(SE expression, Class<SE> clazz) {
		return mTranslatorSequence.translateExpression(expression, clazz);
	}

	@Override
	public <STE> List<?> translateTrace(List<STE> trace, Class<STE> clazz) {
		return mTranslatorSequence.translateTrace(trace, clazz);
	}

	@Override
	public <STE, SE> IProgramExecution<?, ?> translateProgramExecution(IProgramExecution<STE, SE> programExecution) {
		return mTranslatorSequence.translateProgramExecution(programExecution);
	}

	@Override
	public <STE, SE> IBacktranslatedCFG<?, ?> translateCFG(IBacktranslatedCFG< ?, STE> cfg) {
		return mTranslatorSequence.translateCFG(cfg);
	}

	@Override
	public <SE> String translateExpressionToString(SE expression, Class<SE> clazz) {
		return mTranslatorSequence.translateExpressionToString(expression, clazz);
	}

	@Override
	public <STE> List<String> translateTraceToHumanReadableString(List<STE> trace, Class<STE> clazz) {
		return mTranslatorSequence.translateTraceToHumanReadableString(trace, clazz);
	}

	@Override
	public IBacktranslationService getTranslationServiceCopy() {
		return mTranslatorSequence.getTranslationServiceCopy();
	}

	static IBacktranslationService getService(IToolchainStorage storage) {
		assert storage != null;
		IStorable rtr = storage.getStorable(sKey);
		if (rtr == null) {
			rtr = new BacktranslationService();
			storage.putStorable(sKey, rtr);
		}
		return (IBacktranslationService) rtr;
	}

	@Override
	public String toString() {
		return mTranslatorSequence.toString();
	}

	@Override
	public void destroy() {
		mTranslatorSequence = null;
	}

}
