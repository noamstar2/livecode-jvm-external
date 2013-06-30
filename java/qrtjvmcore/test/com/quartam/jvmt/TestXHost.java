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

import java.util.HashMap;
import java.util.Map;

import com.quartam.external.ControlSearchModifier;
import com.quartam.external.ExternalException;
import com.quartam.external.ExternalInterface;
import com.quartam.external.ExternalString;
import com.quartam.internal.ExternalLoader;

/**
 * @author jans
 *
 */
public class TestXHost extends ExternalLoader implements ExternalInterface {
	
	// STATIC FINAL FIELDS TO AID IN COMMUNICATION WITH <external.h>
	
	private static final int EXTERNAL_SUCCESS = 0;
	private static final int EXTERNAL_FAILURE = 1;
	
	private static final String SEARCH_AS_CARD_CONTROL = "true";
	private static final String SEARCH_AS_BACKGROUND_CONTROL = "false";
	
	// STATIC FIELDS AND METHODS
	
	private static final TestXHost INSTANCE;
	
	static {
		INSTANCE = new TestXHost();
	}
	
	public static TestXHost getInstance() {
		return INSTANCE;
	}
	
	// PRIVATE FIELDS AND CONSTRUCTOR
	
	private final Map<String,String> globals;
	private final Map<String,String> variables;
	
	private TestXHost() {
		super();
		globals = new HashMap<String,String>();
		variables = new HashMap<String,String>();
	}
	
	// INSTANCE METHODS
	
	@Override
	protected ExternalInterface getExternalInterface() {
		return this;
	}

	// EXTERNAL INTERFACE CALLBACK METHODS
	
	public void sendCardMessage(String message) throws ExternalException {
		System.out.println("[TestXHost]: receiving message [" + message + "]");
	}
	
	
	public String evaluateExpression(String expression) throws ExternalException {
		System.out.println("[TestXHost]: evaluating expression [" + expression + "] as empty");
		return "";
	}
	
	
	public String getGlobal(String name) throws ExternalException {
		if (globals.containsKey(name)) {
			System.out.println("[TestXHost]: getting existing global [" + name + "]");
		} else {
			System.out.println("[TestXHost]: getting new global [" + name + "]");
			globals.put(name, "");
		}
		return globals.get(name);
	}
	
	
	public void setGlobal(String name, String value) throws ExternalException {
		System.out.println("[TestXHost]: setting global [" + name + "] to [" + value + "]");
		globals.put(name, value);
	}
	
	
	public String getVariable(String name) throws ExternalException {
		if (variables.containsKey(name)) {
			System.out.println("[TestXHost]: getting existing variable [" + name + "]");
		} else {
			System.out.println("[TestXHost]: getting new variable [" + name + "]");
			variables.put(name, "");
		}
		return variables.get(name);
	}
	
	
	public void setVariable(String name, String value) throws ExternalException {
		System.out.println("[TestXHost]: setting variable [" + name + "] to [" + value + "]");
		globals.put(name, value);
	}
	
	
	public ExternalString getVariableAsExternalString(String name, String key) throws ExternalException {
		String resultString = "{" + name + "[" + key + "]}";
		ExternalString result = new ExternalString(resultString.getBytes());
		if (result == null) {
			throw new ExternalException("Error while gettting variable '" + name + "'(key '" + key + "')");
		}
		return result;
	}
	
	
	public void setVariableAsExternalString(String name, String key, ExternalString value) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting variable '" + name + "'(key '" + key + "')");
		}
	}
	
	
	public Map<String,ExternalString> getVariableAsMap(String name) throws ExternalException {
		Map<String,ExternalString> result = new HashMap<String,ExternalString>();
		String nameString = "{" + name + "}";
		ExternalString nameXString = new ExternalString(nameString.getBytes());
		result.put("name", nameXString);
		if (result == null) {
			throw new ExternalException("Error while gettting variable '" + name + "' as map");
		}
		return result;
	}
	
	
	public void setVariableAsMap(String name, Map<String,ExternalString> map) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting variable '" + name + "' as map");
		}
	}
	
	
	public String getTextOfFieldByName(ControlSearchModifier searchModifier, String name) throws ExternalException {
		String group = null;
		switch (searchModifier) {
		case CARD:
			group = SEARCH_AS_CARD_CONTROL;
			break;
		case BACKGROUND:
			group = SEARCH_AS_BACKGROUND_CONTROL;
			break;
		}
		String result = String.format("text of group?%s field named %s", group, name);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field '" + name + "'");
		}
		return result;
	}

	
	public String getTextOfFieldByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException {
		String group = null;
		switch (searchModifier) {
		case CARD:
			group = SEARCH_AS_CARD_CONTROL;
			break;
		case BACKGROUND:
			group = SEARCH_AS_BACKGROUND_CONTROL;
			break;
		}
		String result = String.format("text of group?%s field index %d", group, index);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field number '" + index + "'");
		}
		return result;
	}
	

	public String getTextOfFieldByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException {
		String group = null;
		switch (searchModifier) {
		case CARD:
			group = SEARCH_AS_CARD_CONTROL;
			break;
		case BACKGROUND:
			group = SEARCH_AS_BACKGROUND_CONTROL;
			break;
		}
		if (id < 1) {
			throw new IllegalArgumentException("Short ID must be greater than 0");
		}
		String result = String.format("text of group?%s field id %d", group, id);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field id '" + id + "'");
		}
		return result;
	}
	

	public void setTextOfFieldByName(ControlSearchModifier searchModifier, String name, String value) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field '" + name + "' to value '" + value + "'");
		}
	}
	

	public void setTextOfFieldByNumber(ControlSearchModifier searchModifier, int index, String value) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field number '" + index + "' to value '" + value + "'");
		}
	}
	

	public void setTextOfFieldByShortId(ControlSearchModifier searchModifier, long id, String value) throws ExternalException {
		if (id < 1) {
			throw new IllegalArgumentException("Short ID must be greater than 0");
		}
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field id '" + id + "' to value '" + value + "'");
		}
	}
	

	public void repaintImageByName(ControlSearchModifier searchModifier, String name) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image '" + name + "'");
		}
	}
	

	public void repaintImageByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException {
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image number '" + index + "'");
		}
	}
	

	public void repaintImageByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException {
		if (id < 1) {
			throw new IllegalArgumentException("Short ID must be greater than 0");
		}
		int result = EXTERNAL_SUCCESS;
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image id '" + id + "'");
		}
	}

}
