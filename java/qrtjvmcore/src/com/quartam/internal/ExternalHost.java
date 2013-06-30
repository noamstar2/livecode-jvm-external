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

package com.quartam.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quartam.external.ControlSearchModifier;
import com.quartam.external.ExternalException;
import com.quartam.external.ExternalInterface;
import com.quartam.external.ExternalString;


/**
 * The ExternalHost singleton acts as the glue between the External Interface
 * layer of Revolution and the Java classes that implement the external commands
 * and functions.
 * <p>
 * IMPORTANT NOTE: at this point in time, this class is NOT thread-safe.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
public final class ExternalHost extends ExternalLoader implements ExternalInterface {
	
	// STATIC FINAL FIELDS TO AID IN COMMUNICATION WITH <external.h>
	
	@SuppressWarnings("unused")
	private static final int EXTERNAL_SUCCESS = 0;
	private static final int EXTERNAL_FAILURE = 1;
	
	private static final String SEARCH_AS_CARD_CONTROL = "true";
	private static final String SEARCH_AS_BACKGROUND_CONTROL = "false";
	
	// STATIC FIELDS AND METHODS
	
	private static final ExternalHost INSTANCE;
	
	static {
		INSTANCE = new ExternalHost();
	}
	
	/**
	 * Returns the default ExternalHost implementation
	 * 
	 * @return an ExternalHost implementation
	 */
	public static ExternalHost getInstance() {
		return INSTANCE;
	}
	
	// INSTANCE METHODS
	
	/**
	 * Private constructor to setup the instance variables
	 */
	private ExternalHost() {
		super();
	}

	@Override
	protected ExternalInterface getExternalInterface() {
		return this;
	}

	// EXTERNAL INTERFACE CALLBACK METHODS
	
	/**
	 * Calls the External Interface 'sendCardMessage' through JNI
	 */
	private native int externalSendCardMessage(String message);
	
	/**
	 * Sends a message to the current card of this stack. The message may 
	 * contain parameters, but these should be pre-evaluated.
	 * 
	 * Implementation: Calls the External Interface <code>sendCardMessage</code> through JNI.
	 * 
	 * @param message                    the message that must be sent to the current card
	 * @throws IllegalArgumentException  if the message is <code>null</code>
	 * @throws ExternalException         if the message could not be sent
	 * @see ExternalInterface#sendCardMessage(String message)
	 */
	public void sendCardMessage(String message) throws ExternalException {
		if (message == null) {
			throw new IllegalArgumentException("message can not be null");
		}
		int result = externalSendCardMessage(message);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while sending card message '" + message + "'");
		}
	}
	
	
	/**
	 * Calls the External Interface <code>evalExpr</code> through JNI
	 */
	private native String externalEvalExpr(String expression);
	
	/**
	 * Evaluates the expression in the context of the handler that invoked
	 * the current external command or function.
	 * <p>
	 * Implementation: Calls the External Interface <code>evalExpr</code> through JNI.
	 * 
	 * @param expression                 the expression to evaluate
	 * @return                           the result of the evaluation as a <code>String</code>
	 * @throws IllegalArgumentException  if the expression is <code>null</code>
	 * @throws ExternalException         if the expression could not be evaluated
	 * @see ExternalInterface#evaluateExpression(String expression)
	 */
	public String evaluateExpression(String expression) throws ExternalException {
		if (expression == null) {
			throw new IllegalArgumentException("expression can not be null");
		}
		String result = externalEvalExpr(expression);
		if (result == null) {
			throw new ExternalException("Error while evaluating expression '" + expression + "'");
		}
		return result;
	}
	
	
	/**
	 * Calls the External Interface <code>getGlobal</code> through JNI
	 */
	private native String externalGetGlobal(String name);
	
	/**
	 * Returns the contents of a global variable
	 * <p>
	 * Implementation: Calls the External Interface <code>getGlobal</code> through JNI.
	 * 
	 * @param name                       the name of the global variable
	 * @return                           the contents of the global variable as a <code>String</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the global variable could not be found
	 * @see ExternalInterface#getGlobal(String name)
	 */
	public String getGlobal(String name) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		String result = externalGetGlobal(name);
		if (result == null) {
			throw new ExternalException("Error while gettting global '" + name + "'");
		}
		return result;
	}
	
	
	/**
	 * Calls the External Interface <code>setGlobal</code> through JNI
	 */
	private native int externalSetGlobal(String name, String value);
	
	/**
	 * Sets the value of a global variable
	 * <p>
	 * Implementation: Calls the External Interface <code>setGlobal</code> through JNI.
	 * 
	 * @param name                       the name of the global variable
	 * @param value                      the value to set
	 * @throws IllegalArgumentException  if the name or value is <code>null</code>
	 * @throws ExternalException         if the global variable could not be changed
	 * @see ExternalInterface#setGlobal(String name, String value)
	 */
	public void setGlobal(String name, String value) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		int result = externalSetGlobal(name, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting global '" + name + "' to value '" + value + "'");
		}
	}
	
	
	/**
	 * Calls the External Interface <code>getVariable</code> through JNI
	 */
	private native String externalGetVariable(String name);
	
	/**
	 * Returns the contents of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>getVariable</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @return                           the contents of the variable as a <code>String</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 * @see ExternalInterface#getVariable(String name)
	 */
	public String getVariable(String name) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		String result = externalGetVariable(name);
		if (result == null) {
			throw new ExternalException("Error while gettting variable '" + name + "'");
		}
		return result;
	}
	
	
	/**
	 * Calls the External Interface <code>setVariable</code> through JNI
	 */
	private native int externalSetVariable(String name, String value);
	
	/**
	 * Sets the value of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>setVariable</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @param value                      the value to set 
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be changed
	 * @see ExternalInterface#setVariable(String name, String value)
	 */
	public void setVariable(String name, String value) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		int result = externalSetVariable(name, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting variable '" + name + "' to value '" + value + "'");
		}
	}
	
	
	/**
	 * Calls the External Interface <code>getVariableEx</code> through JNI
	 */
	private native ExternalString externalGetVariableEx(String name, String key);
	
	/**
	 * Returns the contents of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>getVariableEx</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @param key                        the key of the variable element (if the variable is an array)
	 * @return                           the contents of the variable as an <code>ExternalString</code>
	 * @throws IllegalArgumentException  if the name or key is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 * @see ExternalInterface#getVariableAsExternalString(String name, String key)
	 */
	public ExternalString getVariableAsExternalString(String name, String key) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (key == null) {
			throw new IllegalArgumentException("key can not be null");
		}
		ExternalString result = externalGetVariableEx(name, key);
		if (result == null) {
			throw new ExternalException("Error while gettting variable '" + name + "'(key '" + key + "')");
		}
		return result;  // .asReadOnlyBuffer() caused invocation exception
	}
	
	
	/**
	 * Calls the External Interface <code>setVariableEx</code> through JNI
	 */
	private native int externalSetVariableEx(String name, String key, ExternalString value);
	
	/**
	 * Sets the value of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>setVariableEx</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @param key                        the key of the variable element (if the variable is an array)
	 * @param value                      the value to set 
	 * @throws IllegalArgumentException  if the name or key or value is <code>null</code>
	 * @throws ExternalException         if the variable could not be changed
	 * @see ExternalInterface#setVariableAsExternalString(String name, String key, ExternalString value)
	 */
	public void setVariableAsExternalString(String name, String key, ExternalString value) throws ExternalException {
		if (name == null) {
			throw new NullPointerException("name can not be null");
		}
		if (key == null) {
			throw new NullPointerException("key can not be null");
		}
		if (value == null) {
			throw new NullPointerException("value can not be null");
		}
		int result = externalSetVariableEx(name, key, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting variable '" + name + "'(key '" + key + "')");
		}
	}
	
	
	/**
	 * Calls the External Interface <code>getArray</code> through JNI
	 */
	private native Map<String,ExternalString> externalGetArray(String name); 
	
	/**
	 * Returns the contents of an array variable that is accessible within the scope 
	 * of the handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>getArray</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @return                           the contents of the variable as a <code>Map</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 * @see ExternalInterface#getVariableAsMap(String name)
	 */
	public Map<String,ExternalString> getVariableAsMap(String name) throws ExternalException {
		if (name == null) {
			throw new NullPointerException("name can not be null");
		}
		Map<String,ExternalString> result = externalGetArray(name);
		if (result == null) {
			throw new ExternalException("Error while gettting variable '" + name + "' as map");
		}
		return result;
	}
	
	
	/**
	 * Calls the External Interface <code>setArray</code> through JNI
	 */
	private native int externalSetArray(String name, int elementCount, ExternalString[] values, String[] keys);
	
	/**
	 * Sets the contents of an array variable that is accessible within the scope 
	 * of the handler that invoked the external command or function
	 * <p>
	 * Implementation: Calls the External Interface <code>setArray</code> through JNI.
	 * 
	 * @param name                       the name of the variable
	 * @param map                        the contents of the variable
	 * @throws IllegalArgumentException  if the name or map is null
	 * @throws ExternalException         if the variable could not be changed
	 * @see ExternalInterface#setVariableAsMap(String name, Map map)
	 */
	public void setVariableAsMap(String name, Map<String,ExternalString> map) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (map == null) {
			throw new IllegalArgumentException("map can not be null");
		}
		int elementCount = map.size();
		Set<String> mapKeys = map.keySet();
		List<ExternalString> mapValues = new ArrayList<ExternalString>();
		String[] keys = {};
		ExternalString[] values = {};
		keys = mapKeys.toArray(keys);
		for(String key:keys) {
			mapValues.add(map.get(key));
		}
		values = mapValues.toArray(values);
		int result = externalSetArray(name, elementCount, values, keys);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting variable '" + name + "' as map");
		}
	}
	
	
	/**
	 * Calls the External Interface <code>getFieldByName</code> through JNI
	 */
	private native String externalGetFieldByName(String group, String name);
	
	/**
	 * Returns the content of a field identified by its name
	 * <p>
	 * Implementation: Calls the External Interface <code>getFieldByName</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the field
	 * @return                           the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws IllegalArgumentException  if the name is null
	 * @throws ExternalException         if the field could not be found
	 * @see ExternalInterface#getTextOfFieldByName(ControlSearchModifier searchModifier, String name)
	 */
	public String getTextOfFieldByName(ControlSearchModifier searchModifier, String name) throws ExternalException {
		if (name == null) {
			throw new NullPointerException("name can not be null");
		}
		String group = searchModifierToGroup(searchModifier);
		String result = externalGetFieldByName(group, name);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field '" + name + "'");
		}
		return result;
	}

	/**
	 * Calls the External Interface <code>getFieldByNum</code> through JNI
	 */
	private native String externalGetFieldByNum(String group, int index);
	
	/**
	 * Returns the content of a field identified by its index
	 * <p>
	 * Implementation: Calls the External Interface <code>getFieldByNum</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the field
	 * @return                           the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws ExternalException         if the field could not be found
	 * @see  ExternalInterface#getTextOfFieldByNumber(ControlSearchModifier searchModifier, int index)
	 */
	public String getTextOfFieldByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException {
		String group = searchModifierToGroup(searchModifier);
		String result = externalGetFieldByNum(group, index);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field number '" + index + "'");
		}
		return result;
	}
	

	/**
	 * Calls the External Interface <code>getFieldById</code> through JNI
	 */
	private native String externalGetFieldById(String group, long id);
	
	/**
	 * Returns the content of a field identified by its id
	 * <p>
	 * Implementation: Calls the External Interface <code>getFieldById</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param id                         the short id of the field
	 * @return                           the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws IllegalArgumentException  if the id <= 0
	 * @throws ExternalException         if the field could not be found
	 * @see ExternalInterface#getTextOfFieldByShortId(ControlSearchModifier searchModifier, long id)
	 */
	public String getTextOfFieldByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException {
		if (id < 1) {
			throw new IllegalArgumentException("short id must be greater than 0");
		}
		String group = searchModifierToGroup(searchModifier);
		String result = externalGetFieldById(group, id);
		if (result == null) {
			throw new ExternalException("Error while gettting text of field id '" + id + "'");
		}
		return result;
	}
	

	/**
	 * Calls the External Interface <code>setFieldByName</code> through JNI
	 */
	private native int externalSetFieldByName(String group, String name, String value);
	
	/**
	 * Sets the content of a field identified by its name
	 * <p>
	 * Implementation: Calls the External Interface <code>setFieldByName</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the name or value is null
	 * @throws ExternalException         if the field could not be changed
	 * @see ExternalInterface#setTextOfFieldByName(ControlSearchModifier searchModifier, String name, String value)
	 */
	public void setTextOfFieldByName(ControlSearchModifier searchModifier, String name, String value) throws ExternalException {
		if (name == null) {
			throw new IllegalArgumentException("name can not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		String group = searchModifierToGroup(searchModifier);
		int result = externalSetFieldByName(group, name, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field '" + name + "' to value '" + value + "'");
		}
	}
	

	/**
	 * Calls the External Interface <code>setFieldByNum</code> through JNI
	 */
	private native int externalSetFieldByNum(String group, int index, String value);
	
	/**
	 * Sets the content of a field identified by its index
	 * <p>
	 * Implementation: Calls the External Interface <code>setFieldByNum</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the value is <code>null</code>
	 * @throws ExternalException         if the field could not be changed
	 * @see ExternalInterface#setTextOfFieldByNumber(ControlSearchModifier searchModifier, int index, String value)
	 */
	public void setTextOfFieldByNumber(ControlSearchModifier searchModifier, int index, String value) throws ExternalException {
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		String group = searchModifierToGroup(searchModifier);
		int result = externalSetFieldByNum(group, index, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field number '" + index + "' to value '" + value + "'");
		}
	}
	

	/**
	 * Calls the External Interface <code>setFieldById</code> through JNI
	 */
	private native int externalSetFieldById(String group, long id, String value);
	
	/**
	 * Sets the content of a field identified by its short id
	 * <p>
	 * Implementation: Calls the External Interface <code>setFieldById</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param id                         the short id of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the id <= 0 or value is <code>null</code>
	 * @throws ExternalException         if the field could not be changed
	 * @see ExternalInterface#setTextOfFieldByShortId(ControlSearchModifier searchModifier, long id, String value)
	 */
	public void setTextOfFieldByShortId(ControlSearchModifier searchModifier, long id, String value) throws ExternalException {
		if (id < 1) {
			throw new IllegalArgumentException("Short ID must be greater than 0");
		}
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}
		String group = searchModifierToGroup(searchModifier);
		int result = externalSetFieldById(group, id, value);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while setting field id '" + id + "' to value '" + value + "'");
		}
	}
	

	/**
	 * Calls the External Interface <code>showImageByName</code> through JNI
	 */
	private native int externalShowImageByName(String group, String name);
	
	/**
	 * Redraws an image identified by its name
	 * <p>
	 * Implementation: Calls the External Interface <code>showImageByName</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the image
	 * @throws IllegalArgumentException  if the name is null
	 * @throws ExternalException         if the image could not be redrawn
	 * @see ExternalInterface#repaintImageByName(ControlSearchModifier searchModifier, String name)
	 */
	public void repaintImageByName(ControlSearchModifier searchModifier, String name) throws ExternalException {
		String group = searchModifierToGroup(searchModifier);
		int result = externalShowImageByName(group, name);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image '" + name + "'");
		}
	}
	

	/**
	 *  Calls the External Interface <code>showImageByNum</code> through JNI
	 */
	private native int externalShowImageByNum(String group, int index);
	
	/**
	 * Redraws an image identified by its index
	 * <p>
	 * Implementation: Calls the External Interface <code>showImageByNum</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the image
	 * @throws ExternalException         if the image could not be redrawn
	 * @see ExternalInterface#repaintImageByNumber(ControlSearchModifier searchModifier, int index)
	 */
	public void repaintImageByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException {
		String group = searchModifierToGroup(searchModifier);
		int result = externalShowImageByNum(group, index);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image number '" + index + "'");
		}
	}
	

	/**
	 * Calls the External Interface <code>showImageById</code> through JNI
	 */
	private native int externalShowImageById(String group, long id);
	
	/**
	 * Redraws an image identified by its short id
	 * <p>
	 * Implementation: Calls the External Interface <code>showImageById</code> through JNI.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param id                         the short id of the image
	 * @throws IllegalArgumentException  if the id is not greater than zero
	 * @throws ExternalException         if the image could not be redrawn
	 * @see ExternalInterface#repaintImageByShortId(ControlSearchModifier searchModifier, long id)
	 */
	public void repaintImageByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException {
		String group = searchModifierToGroup(searchModifier);
		if (id < 1) {
			throw new IllegalArgumentException("Short ID must be greater than 0");
		}
		int result = externalShowImageById(group, id);
		if (result == EXTERNAL_FAILURE) {
			throw new ExternalException("Error while showing image id '" + id + "'");
		}
	}
	
	/**
	 * Converts a <code>ControlSearchModifier</code> to the <code>String</code> value
	 * expected by Revolution.
	 * 
	 * @param searchModifier
	 * @return the <code>String</code> value as expected by Revolution
	 */
	private String searchModifierToGroup(ControlSearchModifier searchModifier) {
		String group = null;
		switch (searchModifier) {
		case CARD:
			group = SEARCH_AS_CARD_CONTROL;
			break;
		case BACKGROUND:
			group = SEARCH_AS_BACKGROUND_CONTROL;
			break;
		}
		return group;
	}
	
}
