package ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeFunctions.math;

import ca.mcgill.mcb.pcingola.bigDataScript.lang.Parameters;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.Type;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeFunctions.FunctionNative;
import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;

public class FunctionNative_asin_real extends FunctionNative {
	public FunctionNative_asin_real() {
		super();
	}

	@Override
	protected void initFunction() {
		functionName = "asin";
		returnType = Type.REAL;

		String argNames[] = { "a" };
		Type argTypes[] = { Type.REAL };
		parameters = Parameters.get(argTypes, argNames);
		addNativeFunctionToScope();
	}

	@Override
	protected Object runFunctionNative(BigDataScriptThread bdsThread) {
		return (Double) Math.asin(bdsThread.getReal("a"));
	}
}
