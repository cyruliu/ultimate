/**
 * CHandler variation for the SV-COMP 2014.
 */
package de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.svComp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;

import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.LocationFactory;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.CHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.CastAndConversionHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.base.cHandler.MemoryHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.InferredType.Type;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPointer;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CPrimitive.PRIMITIVE;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.container.c.CType;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.exception.IncorrectSyntaxException;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.LRValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.RValue;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.Result;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ResultExpression;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.result.ResultSkip;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.svComp.cHandler.SVCompArrayHandler;
//import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.svComp.cHandler.SVCompFunctionHandler;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.ConvExpr;
import de.uni_freiburg.informatik.ultimate.cdt.translation.implementation.util.SFO;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.Dispatcher;
import de.uni_freiburg.informatik.ultimate.cdt.translation.interfaces.handler.ITypeHandler;
import de.uni_freiburg.informatik.ultimate.model.annotation.Overapprox;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.ASTType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssertStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Attribute;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BinaryExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.BooleanLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.HavocStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.IntegerLiteral;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.NamedType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.PrimitiveType;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableDeclaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.CACSL2BoogieBacktranslator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.cacsl2boogietranslator.preferences.CACSLPreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.result.Check;
import de.uni_freiburg.informatik.ultimate.result.Check.Spec;

/**
 * @author Markus Lindenmann, Matthias Heizmann
 * @date 12.6.2012
 */
public class SvComp14CHandler extends CHandler {
	
	/**
	 * Add assume statements that state that the nondeterministic values are
	 * in the range of the values of the data type.
	 */
	private static final boolean mAssumeThatNondeterministicValuesAreInRange = true;
	/**
	 * The string representing SV-Comp's error method.
	 */
	private static final String ERROR_STRING = "__VERIFIER_error";
	/**
	 * The string representing SV-Comp's havoc method.
	 */
	private static final String NONDET_STRING = "__VERIFIER_nondet_";
	/**
	 * Nondet_X | X in {int, float, char, short, long, pointer}
	 */
	private static final String[] NONDET_TYPE_STRINGS = { "int", "long", "float", "char", "short", "pointer" };
	/**
	 * The string representing SV-Comp's assert method.
	 */
	private static final String ASSUME_STRING = "__VERIFIER_assume";

	/**
	 * Constructor.
	 * 
	 * @param main
	 *            a reference to the main dispatcher.
	 */
	public SvComp14CHandler(Dispatcher main, CACSL2BoogieBacktranslator backtranslator, 
			Logger logger, ITypeHandler typeHandler) {
		super(main, backtranslator, false, logger, typeHandler);
		super.mArrayHandler = new SVCompArrayHandler();
	}

	//
	// __VERIFIER_assume(EXPR) && skip VERIFIER_nondet_X()
	//

	@Override
	public Result visit(Dispatcher main, IASTFunctionCallExpression node) {
		ILocation loc = LocationFactory.createCLocation(node);

		if (!(node.getFunctionNameExpression() instanceof IASTIdExpression))
			return super.visit(main, node);
		IASTIdExpression astIdExpression = (IASTIdExpression) node.getFunctionNameExpression();
		String methodName = astIdExpression.getName().toString();

		ArrayList<Statement> stmt = new ArrayList<Statement>();
		ArrayList<Declaration> decl = new ArrayList<Declaration>();
		Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
		ArrayList<Overapprox> overappr = new ArrayList<Overapprox>();
		LRValue returnValue = null;
		

		if (methodName.equals(ERROR_STRING)) {
			boolean checkSvcompErrorfunction = 
					main.mPreferences.getBoolean(CACSLPreferenceInitializer.LABEL_CHECK_SVCOMP_ERRORFUNCTION);
			if (checkSvcompErrorfunction) {
				Check check = new Check(Spec.ERROR_Function);
				AssertStatement assertStmt = new AssertStatement(loc, new BooleanLiteral(loc,
						new InferredType(Type.Boolean), false));
				check.addToNodeAnnot(assertStmt);
				stmt.add(assertStmt);
			} 
			return new ResultExpression(stmt, returnValue, decl, auxVars, overappr);
		}
		if (methodName.equals(ASSUME_STRING)) {
			ArrayList<Expression> args = new ArrayList<Expression>();
			for (IASTInitializerClause inParam : node.getArguments()) {
				ResultExpression in = ((ResultExpression) main.dispatch(inParam)).switchToRValueIfNecessary(main,
						mMemoryHandler, mStructHandler, loc);
				if (in.lrVal.getValue() == null) {
					String msg = "Incorrect or invalid in-parameter! " + loc.toString();
					throw new IncorrectSyntaxException(loc, msg);
				}
				in = ConvExpr.rexIntToBoolIfNecessary(loc, in);
				args.add(in.lrVal.getValue());
				stmt.addAll(in.stmt);
				decl.addAll(in.decl);
				auxVars.putAll(in.auxVars);
				overappr.addAll(in.overappr);
			}
			assert args.size() == 1; // according to SV-Comp specification!
			for (Expression a : args) {
//			could just take the first as there is only one, but it's so easy to make it more general..
				stmt.add(new AssumeStatement(loc, a));
			}
			assert (isAuxVarMapcomplete(main, decl, auxVars));
			return new ResultExpression(stmt, returnValue, decl, auxVars, overappr);
		}
		for (String t : NONDET_TYPE_STRINGS)
			if (methodName.equals(NONDET_STRING + t)) {
				
				final ASTType type;
				final CType cType;
				switch (t) {
				case "int":
					cType = new CPrimitive(PRIMITIVE.INT);
					type = mTypeHandler.ctype2asttype(loc, cType);
					break;
				case "long":
					cType = new CPrimitive(PRIMITIVE.LONG);
					type = mTypeHandler.ctype2asttype(loc, cType);
					break;
				case "float":
					cType = new CPrimitive(PRIMITIVE.FLOAT);
					type = mTypeHandler.ctype2asttype(loc, cType);
					break;
				case "char":
					cType = new CPrimitive(PRIMITIVE.CHAR);
					type = mTypeHandler.ctype2asttype(loc, cType);
					break;
				case "short":
					cType = new CPrimitive(PRIMITIVE.SHORT);
					type = mTypeHandler.ctype2asttype(loc, cType);
					break;
				case "pointer":
					NamedType boogiePointerType = new NamedType(null, new InferredType(Type.Struct), SFO.POINTER,
							new ASTType[0]);
					type = boogiePointerType;
					cType = new CPointer(new CPrimitive(PRIMITIVE.VOID));
					break;
				default:
					throw new AssertionError("unknown type " + t);
				}
				String tmpName = main.nameHandler.getTempVarUID(SFO.AUXVAR.NONDET);
				VariableDeclaration tVarDecl = SFO.getTempVarVariableDeclaration(tmpName, type, loc);
				decl.add(tVarDecl);
				auxVars.put(tVarDecl, loc);

				returnValue = new RValue(new IdentifierExpression(loc, tmpName), cType);
				
				if (mAssumeThatNondeterministicValuesAreInRange) {
					switch (t) {
					case "int":
					case "long":
					case "char":
					case "short":
						AssumeStatement inRange = constructAssumeInRangeStatement(loc, returnValue);
						stmt.add(inRange);
					}
				}
				assert (isAuxVarMapcomplete(main, decl, auxVars));
				return new ResultExpression(stmt, returnValue, decl, auxVars, overappr);
			}
		if (methodName.equals("printf")) {
			// skip if parent of parent is CompoundStatement
			// otherwise, replace by havoced variable
			if (node.getParent().getParent() instanceof IASTCompoundStatement) {
				return new ResultSkip();
			}
			ASTType tempType = new PrimitiveType(loc, SFO.INT);
			String tId = main.nameHandler.getTempVarUID(SFO.AUXVAR.NONDET);
			VariableDeclaration tVarDecl = new VariableDeclaration(loc, new Attribute[0], new VarList[] { new VarList(
					loc, new String[] { tId }, tempType) });
			auxVars.put(tVarDecl, loc);
			decl.add(tVarDecl);
			stmt.add(new HavocStatement(loc, new VariableLHS[] { new VariableLHS(loc, tId) }));
			returnValue = new RValue(new IdentifierExpression(loc, tId), null);
			assert (isAuxVarMapcomplete(main, decl, auxVars));
			return new ResultExpression(stmt, returnValue, decl, auxVars, overappr);
		}
//		this is a gcc-builtin function that helps with branch predication, it always returns the first argument.
		if (methodName.equals("__builtin_expect")) { 
			return main.dispatch(node.getArguments()[0]);
		}
		
		if (methodName.equals("__builtin_memcpy")) {
			((CHandler) main.cHandler).mMemoryHandler.setDeclareMemCpy();

			assert node.getArguments().length == 3;
			ResultExpression destRex = (ResultExpression) main.dispatch(node.getArguments()[0]);
			ResultExpression srcRex = (ResultExpression) main.dispatch(node.getArguments()[1]);
			ResultExpression sizeRex = (ResultExpression) main.dispatch(node.getArguments()[2]);
			
			destRex = destRex.switchToRValueIfNecessary(main, mMemoryHandler, mStructHandler, loc);
			srcRex = srcRex.switchToRValueIfNecessary(main, mMemoryHandler, mStructHandler, loc);
			sizeRex = sizeRex.switchToRValueIfNecessary(main, mMemoryHandler, mStructHandler, loc);
			
			stmt.addAll(destRex.stmt);
			stmt.addAll(srcRex.stmt);
			stmt.addAll(sizeRex.stmt);
			decl.addAll(destRex.decl);
			decl.addAll(srcRex.decl);
			decl.addAll(sizeRex.decl);
			auxVars.putAll(destRex.auxVars);
			auxVars.putAll(srcRex.auxVars);
			auxVars.putAll(sizeRex.auxVars);
			overappr.addAll(destRex.overappr);
			overappr.addAll(srcRex.overappr);
			overappr.addAll(sizeRex.overappr);		

			String tId = main.nameHandler.getTempVarUID(SFO.AUXVAR.MEMCPYRES);
			VariableDeclaration tVarDecl = new VariableDeclaration(loc, new Attribute[0], new VarList[] { new VarList(
					loc, new String[] { tId }, MemoryHandler.POINTER_TYPE) });
			decl.add(tVarDecl);
			auxVars.put(tVarDecl, loc);		
			
			Statement call = new CallStatement(loc, false, new VariableLHS[] { new VariableLHS(loc, tId) }, SFO.MEMCPY, 
					new Expression[] { destRex.lrVal.getValue(), srcRex.lrVal.getValue(), sizeRex.lrVal.getValue() });
			stmt.add(call);
			
			return new ResultExpression(stmt, new RValue(new IdentifierExpression(loc, tId), 
					destRex.lrVal.cType), decl, auxVars, overappr);
		}
		
		if (methodName.equals("__builtin_object_size")) {
			main.warn(loc, "used trivial implementation of __builtin_object_size");
			return new ResultExpression(new RValue(new IntegerLiteral(loc, SFO.NR0), 
					new CPrimitive(PRIMITIVE.INT)));
		}

		/*
		 * builtin_prefetch according to https://gcc.gnu.org/onlinedocs/gcc-3.4.5/gcc/Other-Builtins.html (state: 5.6.2015)
		 * triggers the processor to load something into cache, does nothing else
		 * is void thus has no return value
		 */
		if (methodName.equals("__builtin_prefetch")) {
			main.warn(loc, "ignored call to __builtin_prefetch");
			return new ResultSkip();
		}

		if (methodName.equals("abort")) {
			stmt.add(new AssumeStatement(loc, new BooleanLiteral(loc, false)));
			return new ResultExpression(stmt, null, decl, auxVars, overappr);
		}

		return super.visit(main, node);
	}

	/**
	 * Returns "assume (minValue <= lrValue && lrValue <= maxValue)"
	 */
	private AssumeStatement constructAssumeInRangeStatement(ILocation loc,
			LRValue lrValue) {
		IntegerLiteral minValue = new IntegerLiteral(loc, CastAndConversionHandler.getMinValueOfPrimitiveType(
				mMemoryHandler, lrValue.cType.getUnderlyingType()).toString());
		IntegerLiteral maxValue = new IntegerLiteral(loc, CastAndConversionHandler.getMaxValueOfPrimitiveType(
				mMemoryHandler, lrValue.cType.getUnderlyingType()).toString());
		BinaryExpression biggerMinInt = new BinaryExpression(loc, 
				BinaryExpression.Operator.COMPLEQ, minValue, lrValue.getValue());
		BinaryExpression smallerMaxValue = new BinaryExpression(loc, 
				BinaryExpression.Operator.COMPLEQ, lrValue.getValue(), maxValue);
		AssumeStatement inRange = new AssumeStatement(loc, new BinaryExpression(loc, 
				BinaryExpression.Operator.LOGICAND, biggerMinInt, smallerMaxValue));
		return inRange;
	}
	
	@Override
	public Result visit(Dispatcher main, IASTIdExpression node) {
		ILocation loc = LocationFactory.createCLocation(node);
		if (node.getName().toString().equals("null")) {
			return new ResultExpression(
					new RValue(new IdentifierExpression(loc, SFO.NULL),
					new CPointer(new CPrimitive(PRIMITIVE.VOID))));
		}
		if (node.getName().toString().equals("__PRETTY_FUNCTION__")
				|| node.getName().toString().equals("__FUNCTION__")){
			String tId = main.nameHandler.getTempVarUID(SFO.AUXVAR.NONDET);
			VariableDeclaration tVarDecl = new VariableDeclaration(loc, new Attribute[0], new VarList[] { new VarList(
					loc, new String[] { tId }, MemoryHandler.POINTER_TYPE) });
			RValue rvalue = new RValue(new IdentifierExpression(loc, tId), new CPointer(new CPrimitive(PRIMITIVE.CHAR)));
			ArrayList<Declaration> decls = new ArrayList<Declaration>();
			decls.add(tVarDecl);
			Map<VariableDeclaration, ILocation> auxVars = new LinkedHashMap<VariableDeclaration, ILocation>();
			auxVars.put(tVarDecl, loc);
			return new ResultExpression(new ArrayList<Statement>(), rvalue, decls, auxVars);
		}
		return super.visit(main, node);
	}

	//
	// VERIFIER_nondet_X()
	//
	@Override
	public Result visit(Dispatcher main, IASTBinaryExpression node) {
		Result r = super.visit(main, node);
		if (node.getOperator() == IASTBinaryExpression.op_divide
				|| node.getOperator() == IASTBinaryExpression.op_divideAssign) {
			// remove division by zero asserts
			assert r instanceof ResultExpression;
			ResultExpression rex = (ResultExpression) r;
			Iterator<Statement> it = rex.stmt.iterator();
			while (it.hasNext())
				if (it.next() instanceof AssertStatement)
					it.remove();
		}
		return r;
	}

	@Override
	public Result visit(Dispatcher main, IASTASMDeclaration node) {
		//workaround for now: ignore inline assembler instructions
		return new ResultSkip();
	}

}