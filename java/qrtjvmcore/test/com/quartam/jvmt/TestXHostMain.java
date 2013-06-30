/* Copyright (C) 2008-2013 Quartam Software / Jan Schenkel.
 
 This file is part of the [qrtjvm] project, also known as
 Quartam JavaVirtualMachine External for LiveCode.
 
 [qrtjvm] is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License v3 as published by the Free
 Software Foundation.
 
 [qrtjvm] is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.
 
 You should have received a copy of the GNU General Public License
 along with [qrtjvm].  If not see <http://www.gnu.org/licenses/>.  */

package com.quartam.jvmt;

import com.quartam.external.ExternalException;

/**
 * @author jans
 *
 */
public class TestXHostMain {

	private static final String JARPATH = "../qrtjvmexamples.jar";
	
	/**
	 * @param args
	 * @throws ExternalException 
	 */
	public static void main(String[] args) throws ExternalException {
		TestXHost host = TestXHost.getInstance();
		host.loadExternalLibrary(JARPATH);
		System.out.println("ExternalLibraries:\n" + host.getExternalLibraries() + "\n");
		System.out.println("ExternalPackages:\n" + host.getExternalPackages() + "\n");
		System.out.println("ExternalCommands:\n" + host.getExternalCommands() + "\n");
		System.out.println("ExternalFunctions:\n" + host.getExternalFunctions() + "\n");
		System.out.println("Calling externals...");
		System.out.println("etSendCardMessage() => " + host.callExternalCommand("etSendCardMessage", null));
		System.out.println("etEvaluateExpression() => " + host.callExternalFunction("etEvaluateExpression", null));
		System.out.println("etGetGlobal() => " + host.callExternalFunction("etGetGlobal", null));
		System.out.println("etSetGlobal() => " + host.callExternalCommand("etSetGlobal", null));
		System.out.println("etGetVariable() => " + host.callExternalFunction("etGetVariable", null));
		System.out.println("etSetVariable() => " + host.callExternalCommand("etSetVariable", null));
		System.out.println("etGetVariableAsExternalString() => " + host.callExternalFunction("etGetVariableAsExternalString", null));
		System.out.println("etSetVariableAsExternalString() => " + host.callExternalCommand("etSetVariableAsExternalString", null));
		System.out.println("etGetVariableAsExternalString2() => " + host.callExternalFunction("etGetVariableAsExternalString2", null));
		System.out.println("etSetVariableAsExternalString2() => " + host.callExternalCommand("etSetVariableAsExternalString2", null));
		System.out.println("etGetVariableAsMap() => " + host.callExternalFunction("etGetVariableAsMap", null));
		System.out.println("etSetVariableAsMap() => " + host.callExternalCommand("etSetVariableAsMap", null));
		System.out.println("\nCalling external functions...");
		System.out.println("etHello() => " + host.callExternalFunction("etHello", null));
		String[] fcnparams = {"theVariableName"};
		System.out.println("etHelloVar() => " + host.callExternalFunction("etHelloVar", fcnparams));
		System.out.println("etHelloArrVar() => " + host.callExternalFunction("etHelloArrVar", fcnparams));
		System.out.println("etHelloMapVar() => " + host.callExternalFunction("etHelloMapVar", fcnparams));
		System.out.println("etLoadDatatime() => " + host.callExternalFunction("etLoadDatetime", null));
		System.out.println("etInstDatatime() => " + host.callExternalFunction("etInstDatetime", null));
		System.out.println("etInitDatatime() => " + host.callExternalFunction("etInitDatetime", null));
	}

}
