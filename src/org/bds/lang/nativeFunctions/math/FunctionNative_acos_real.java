package org.bds.lang.nativeFunctions.math;

import org.bds.lang.Parameters;
import org.bds.lang.nativeFunctions.FunctionNative;
import org.bds.lang.type.Type;
import org.bds.run.BdsThread;

public class FunctionNative_acos_real extends FunctionNative {
	public FunctionNative_acos_real() {
		super();
	}

	@Override
	protected void initFunction() {
		functionName = "acos";
		returnType = Type.REAL;

		String argNames[] = { "a" };
		Type argTypes[] = { Type.REAL };
		parameters = Parameters.get(argTypes, argNames);
		addNativeFunctionToScope();
	}

	@Override
	protected Object runFunctionNative(BdsThread bdsThread) {
		return (Double) Math.acos(bdsThread.getReal("a"));
	}
}
