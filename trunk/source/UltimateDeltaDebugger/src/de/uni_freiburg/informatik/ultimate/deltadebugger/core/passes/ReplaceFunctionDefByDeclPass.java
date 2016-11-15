package de.uni_freiburg.informatik.ultimate.deltadebugger.core.passes;

import de.uni_freiburg.informatik.ultimate.deltadebugger.core.PassDescription;
import de.uni_freiburg.informatik.ultimate.deltadebugger.core.generators.ReplaceFunctionDefByDecl;

/**
 * Pass to replace function definitions by their declarations.
 */
public final class ReplaceFunctionDefByDeclPass {
	public static final PassDescription INSTANCE = PassDescription.builder(ReplaceFunctionDefByDecl::analyze)
			.name("Replace function definition by declaration")
			.description("(Actually just replaces the body statement by \";\")").build();
	
	private ReplaceFunctionDefByDeclPass() {
		
	}
}
