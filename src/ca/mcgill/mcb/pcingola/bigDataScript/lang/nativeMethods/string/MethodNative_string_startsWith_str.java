package ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeMethods.string;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import ca.mcgill.mcb.pcingola.bigDataScript.task.Task;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.Parameters;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.Type;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.TypeList;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeMethods.MethodNative;
import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;
import ca.mcgill.mcb.pcingola.bigDataScript.util.Gpr;

public class MethodNative_string_startsWith_str extends MethodNative {
	public MethodNative_string_startsWith_str() {
		super();
	}

	@Override
	protected void initMethod() {
		functionName = "startsWith";
		classType = Type.STRING;
		returnType = Type.BOOL;

		String argNames[] = { "this", "str" };
		Type argTypes[] = { Type.STRING, Type.STRING };
		parameters = Parameters.get(argTypes, argNames);
		addNativeMethodToClassScope();
	}

	@Override
	protected Object runMethodNative(BigDataScriptThread csThread, Object objThis) {
		return objThis.toString().startsWith( csThread.getString("str") );
	}
}
