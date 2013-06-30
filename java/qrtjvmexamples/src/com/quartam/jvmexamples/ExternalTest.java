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

package com.quartam.jvmexamples;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.quartam.external.ControlSearchModifier;
import com.quartam.external.ExternalCommand;
import com.quartam.external.ExternalException;
import com.quartam.external.ExternalFunction;
import com.quartam.external.ExternalInterface;
import com.quartam.external.ExternalPackage;
import com.quartam.external.ExternalPackageInit;
import com.quartam.external.ExternalString;

/**
 * @author jans
 *
 */
@ExternalPackage(author="Jan Schenkel", date="20090327")
public class ExternalTest {
	
	private static final Date loadDate;
	
	static {
		loadDate = new Date();
	}
	
	private ExternalInterface externalInterface;
	private Date instDate;
	private Date initDate;

	public ExternalTest() {
		super();
		instDate = new Date();
	}

	@ExternalPackageInit
	public void init(ExternalInterface externalInterface) {
		this.externalInterface = externalInterface;
		initDate = new Date();
	}
	
	@ExternalCommand
	public void etSendCardMessage() throws ExternalException {
		externalInterface.sendCardMessage("qrtjvm_callbackcommand 1");
	}

	@ExternalFunction
	public String etEvaluateExpression() throws ExternalException {
		return externalInterface.evaluateExpression("qrtjvm_callbackfunction(1)");
	}

	@ExternalFunction
	public String etGetGlobal() throws ExternalException {
		return externalInterface.getGlobal("gJVMglobal");
	}

	@ExternalCommand
	public void etSetGlobal() throws ExternalException {
		externalInterface.setGlobal("gJVMglobal", "value set by the etSetGlobal XCMD");
	}

	@ExternalFunction
	public String etGetVariable() throws ExternalException {
		return externalInterface.getVariable("tJVMlocal");
	}

	@ExternalCommand
	public void etSetVariable() throws ExternalException {
		externalInterface.setVariable("tJVMlocal", "value set by the etSetVariable XCMD");
	}

	@ExternalFunction
	public String etGetVariableAsExternalString() throws ExternalException {
		ExternalString rawData = externalInterface.getVariableAsExternalString("tJVMlocal", "");
		return "(length: " + rawData.getLength() + " | first byte: " + rawData.getBytes()[0] + ")";
	}

	@ExternalCommand
	public void etSetVariableAsExternalString() throws ExternalException {
		String newData = "value set by the etSetVariableAsExternalString XCMD";
		ExternalString rawData = new ExternalString(newData.getBytes());
		externalInterface.setVariableAsExternalString("tJVMlocal", "", rawData);		
	}

	@ExternalFunction
	public String etGetVariableAsExternalString2() throws ExternalException {
		ExternalString rawData = externalInterface.getVariableAsExternalString("tJVMlocalA", "name");
		return "(length: " + rawData.getLength() + " | first byte: " + rawData.getBytes()[0] + ")";
	}

	@ExternalCommand
	public void etSetVariableAsExternalString2() throws ExternalException {
		String newData = "value set by the etSetVariableAsExternalString2 XCMD";
		ExternalString rawData = new ExternalString(newData.getBytes());
		externalInterface.setVariableAsExternalString("tJVMlocalA", "name", rawData);		
	}

	@ExternalFunction
	public String etGetVariableAsMap() throws ExternalException {
		StringBuilder builder = new StringBuilder();
		builder.append("\n  Variable name: tJVMlocalA");
		Map<String,ExternalString> rawMap = externalInterface.getVariableAsMap("tJVMlocalA"); 
		for (String key:rawMap.keySet()) {
			ExternalString rawData = rawMap.get(key);
			String rawString = new String(rawData.getBytes());
			builder.append("\n  '");
			builder.append(key);
			builder.append(" ' => (length: ");
			builder.append(rawData.getLength());
			builder.append(" | first byte: ");
			builder.append(rawData.getBytes()[0]);
			builder.append(" | content: ");
			builder.append(rawString);
			builder.append(")");
		}
		return builder.toString(); 
	}

	@ExternalCommand
	public void etSetVariableAsMap() throws ExternalException {
		Map<String,ExternalString> rawMap = new HashMap<String,ExternalString>();
		String newData = "first value set by the etSetVariableAsMap XCMD";
		ExternalString rawData = new ExternalString(newData.getBytes());
		rawMap.put("name", rawData);
		String newData2 = "second value set by the etSetVariableAsMap XCMD";
		ExternalString rawData2 = new ExternalString(newData2.getBytes());
		rawMap.put("data", rawData2);
		externalInterface.setVariableAsMap("tJVMlocalA", rawMap);		
	}

	@ExternalFunction
	public String etGetFieldByName() throws ExternalException {
		return externalInterface.getTextOfFieldByName(ControlSearchModifier.NONE, "RevField");
	}
	@ExternalCommand
	public void etSetFieldByName() throws ExternalException {
		externalInterface.setTextOfFieldByName(ControlSearchModifier.NONE, "RevField", "Reset by etSetFieldByName XCMD");
	}

	@ExternalFunction
	public String etGetFieldByNumber() throws ExternalException {
		return externalInterface.getTextOfFieldByNumber(ControlSearchModifier.NONE, 2);
	}
	@ExternalCommand
	public void etSetFieldByNumber() throws ExternalException {
		externalInterface.setTextOfFieldByNumber(ControlSearchModifier.NONE, 2, "Reset by etSetFieldByNumber XCMD");
	}

	@ExternalFunction
	public String etGetFieldByShortId() throws ExternalException {
		return externalInterface.getTextOfFieldByShortId(ControlSearchModifier.NONE, 1010);
	}
	@ExternalCommand
	public void etSetFieldByShortId() throws ExternalException {
		externalInterface.setTextOfFieldByShortId(ControlSearchModifier.NONE, 1010, "Reset by etSetFieldByShortId XCMD");
	}


	// Miscellaneous examples
	
	@ExternalFunction
	public String etHello() {
		return "(etHello XFCN) Hello, world";
	}

	@ExternalFunction
	public String etHelloVar(String[] params) {
		String variableData = null;
		try {
			variableData = externalInterface.getVariable(params[0]);
		} catch (ExternalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "(etHelloVar XFCN) Hello, " + (variableData == null ? "stranger" : variableData);
	}
	
	@ExternalFunction
	public String etHelloArrVar(String[] params) {
		ExternalString rawData = null;
		String rawString = null;
		try {
			rawData = externalInterface.getVariableAsExternalString(params[0],"name");
			rawString = new String(rawData.getBytes());
		} catch (ExternalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "(etHelloArrVar XFCN) Howdy, " + (rawData == null ? "stranger" : "(length: " + rawData.getLength() + " | first byte: " + rawData.getBytes()[0] + " | content: " + rawString + ")");
	}
	
	@ExternalFunction
	public String etHelloMapVar(String[] params) {
		Map<String,ExternalString> rawMap = null;
		ExternalString rawData = null;
		String rawString = null;
		try {
			rawMap = externalInterface.getVariableAsMap(params[0]);
			rawData = rawMap.get("name");
			rawString = new String(rawData.getBytes());
		} catch (ExternalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "(etHelloMapVar XFCN) Yello, " + (rawData == null ? "stranger" : "(length: " + rawData.getLength() + " | first byte: " + rawData.getBytes()[0] + " | content: " + rawString + ")");
	}
	
	@ExternalFunction
	public String etLoadDatetime() {
		return loadDate.toString();
	}

	@ExternalFunction
	public String etInstDatetime() {
		return instDate.toString();
	}

	@ExternalFunction
	public String etInitDatetime() {
		return initDate.toString();
	}

}
