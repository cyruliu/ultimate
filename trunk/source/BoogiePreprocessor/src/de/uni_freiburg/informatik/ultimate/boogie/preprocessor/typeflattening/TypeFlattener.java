package de.uni_freiburg.informatik.ultimate.boogie.preprocessor.typeflattening;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.access.IUnmanagedObserver;
import de.uni_freiburg.informatik.ultimate.access.WalkerOptions;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Unit;

/**
 * {@link TypeFlattener} converts types with parameters to types without
 * parameters.
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class TypeFlattener implements IUnmanagedObserver {

	private final Logger mLogger;

	public TypeFlattener(final Logger logger) {
		mLogger = logger;
	}

	@Override
	public void init(GraphType modelType, int currentModelIndex, int numberOfModels) throws Throwable {

	}

	@Override
	public void finish() throws Throwable {

	}

	@Override
	public WalkerOptions getWalkerOptions() {
		return null;
	}

	@Override
	public boolean performedChanges() {
		return true;
	}

	@Override
	public boolean process(IElement root) throws Throwable {
		if (!(root instanceof Unit)) {
			return true;
		}

		final Unit unit = (Unit) root;

		NewTypeCollector ntc = new NewTypeCollector(mLogger);
		ntc.run(unit);
		
		final TypeCollector tc = new TypeCollector(mLogger);
		tc.run(unit);

		return false;
	}
}
