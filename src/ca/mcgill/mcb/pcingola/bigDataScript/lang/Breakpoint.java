package ca.mcgill.mcb.pcingola.bigDataScript.lang;

import org.antlr.v4.runtime.tree.ParseTree;

import ca.mcgill.mcb.pcingola.bigDataScript.compile.CompilerMessage.MessageType;
import ca.mcgill.mcb.pcingola.bigDataScript.compile.CompilerMessages;
import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;
import ca.mcgill.mcb.pcingola.bigDataScript.run.DebugMode;
import ca.mcgill.mcb.pcingola.bigDataScript.scope.Scope;

/**
 * An "breakpoint" statement
 *
 * @author pcingola
 */
public class Breakpoint extends Exit {

	public Breakpoint(BigDataScriptNode parent, ParseTree tree) {
		super(parent, tree);
	}

	@Override
	public Type returnType(Scope scope) {
		if (returnType != null) return returnType;

		// Calculate expression's return type
		if (expr != null) expr.returnType(scope);

		returnType = Type.STRING;
		return returnType;
	}

	/**
	 * Run the program
	 */
	@Override
	public void runStep(BigDataScriptThread bdsThread) {
		// Switch debug mode to 'step'
		bdsThread.setDebugMode(DebugMode.STEP);

		// Show message
		String msg = "";
		if (expr != null) {
			// Evaluate expression to show
			bdsThread.run(expr);
			if (bdsThread.isCheckpointRecover()) return;
			msg = popString(bdsThread);
		}

		if (bdsThread.isCheckpointRecover()) return;
		System.err.print("Breakpoint " + getFileName() + ", line " + getLineNum() + (!msg.isEmpty() ? ": " + msg : ""));
	}

	@Override
	public String toString() {
		return "breakpoint " + expr;
	}

	@Override
	protected void typeCheck(Scope scope, CompilerMessages compilerMessages) {
		returnType(scope);
		if ((expr.getReturnType() != null) && (!expr.getReturnType().canCast(Type.STRING))) compilerMessages.add(this, "Cannot cast " + expr.getReturnType() + " to " + Type.STRING, MessageType.ERROR);
	}

}