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

package com.quartam.external;

import java.util.Map;


/**
 * The <code>ExternalInterface</code> interface is provided as a generic layer 
 * between the external packages that you implement and the underlying engine.
 * <p>
 * The same calls are available as specified in the External Interface
 * supplied by <b>Runtime Revolution's ExternalEnvironment v3</b>, although some
 * calls have been renamed to better fit a Java developer's mind set.
 * <p>
 * An implementation of this interface will be passed as the single parameter 
 * to the optional <code>ExternalPackageInit</code>-annotated initialisation 
 * method of your <code>ExternalPackage</code> class.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
public interface ExternalInterface {
	
	/**
	 * Sends a message to the current card of this stack. The message may 
	 * contain parameters, but these should be pre-evaluated.
	 * <p>
	 * Equivalent to External Interface call: <code>sendCardMessage</code>.
	 * 
	 * @param message                    the message that must be sent to the current card
	 * @throws IllegalArgumentException  if the message is <code>null</code>
	 * @throws ExternalException         if the message could not be sent
	 */
	public void sendCardMessage(String message) throws ExternalException;
	
	/**
	 * Evaluates the expression in the context of the handler that invoked
	 * the current external command or function.
	 * <p>
	 * Equivalent to External Interface call: <code>evalExpr</code>.
	 * 
	 * @param expression                 the expression to evaluate
	 * @return                           the result of the evaluation as a <code>String</code>
	 * @throws IllegalArgumentException  if the expression is <code>null</code>
	 * @throws ExternalException         if the expression could not be evaluated
	 */
	public String evaluateExpression(String expression) throws ExternalException;
	
	/**
	 * Returns the contents of a global variable
	 * <p>
	 * Equivalent to External Interface call: <code>getGlobal</code>.
	 * 
	 * @param name                       the name of the global variable
	 * @return                           the contents of the global variable as a <code>String</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the global variable could not be found
	 */
	public String getGlobal(String name) throws ExternalException;
	
	/**
	 * Sets the value of a global variable
	 * <p>
	 * Equivalent to External Interface call: <code>setGlobal</code>.
	 * 
	 * @param name                       the name of the global variable
	 * @param value                      the value to set
	 * @throws IllegalArgumentException  if the name or value is <code>null</code>
	 * @throws ExternalException         if the global variable could not be changed
	 */
	public void setGlobal(String name, String value) throws ExternalException;
	
	/**
	 * Returns the contents of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Equivalent to External Interface call: <code>getVariable</code>.
	 * 
	 * @param name                       the name of the variable
	 * @return                           the contents of the variable as a <code>String</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 */
	public String getVariable(String name) throws ExternalException;
	
	/**
	 * Sets the value of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * Equivalent to External Interface call: <code>setVariable</code>.
	 * 
	 * @param name                       the name of the variable
	 * @param value                      the value to set 
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be changed
	 */
	public void setVariable(String name, String value) throws ExternalException;
	
	/**
	 * Returns the contents of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * If <code>key</code> is the empty string ("") then the value fetched will be that 
	 * of the variable as a non-array (i.e. if the variable is an array then the  
	 * resulting value will be empty.
	 * <p>
	 * If <code>key</code> is not the empty string then the value fetched will be the 
	 * value of the element of the variable with key <code>key</code>, or empty if there 
	 * is no such key or the variable is not an array.
	 * <p>
	 * Equivalent to External Interface call: <code>getVariableEx</code>.
	 * 
	 * @param name                       the name of the variable
	 * @param key                        the key of the variable element (if the variable is an array)
	 * @return                           the contents of the variable as an <code>ExternalString</code>
	 * @throws IllegalArgumentException  if the name or key is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 */
	public ExternalString getVariableAsExternalString(String name, String key) throws ExternalException;
	
	/**
	 * Sets the value of a variable that is accessible within the scope of the
	 * handler that invoked the external command or function
	 * <p>
	 * If <code>key</code> is the empty string ("") then the variable will be set as a 
	 * non-array. In particular, if it was previously an array its elements will be 
	 * deleted.
	 * <p>
	 * If <code>key</code> is not the empty string then the element with key 
	 * <code>key</code> will be set to <code>value</code>. In particular, if the variable 
	 * was not previously an array its value will be cleared and it will become one. 
	 * Similarly, if the element with the given key does not exist, it will be created.
	 * <p>
	 * Equivalent to External Interface call: <code>setVariableEx</code>.
	 * 
	 * @param name                       the name of the variable
	 * @param key                        the key of the variable element (if the variable is an array)
	 * @param value                      the value to set 
	 * @throws IllegalArgumentException  if the name or key or value is <code>null</code>
	 * @throws ExternalException         if the variable could not be changed
	 */
	public void setVariableAsExternalString(String name, String key, ExternalString value) throws ExternalException;
	
	/**
	 * Returns the contents of an array variable that is accessible within the scope 
	 * of the handler that invoked the external command or function
	 * <p>
	 * Equivalent to External Interface call: <code>getArray</code>.
	 * 
	 * @param name                       the name of the variable
	 * @return                           the contents of the variable as a <code>Map</code>
	 * @throws IllegalArgumentException  if the name is <code>null</code>
	 * @throws ExternalException         if the variable could not be found
	 */
	public Map<String,ExternalString> getVariableAsMap(String name) throws ExternalException;
	
	/**
	 * Sets the contents of an array variable that is accessible within the scope 
	 * of the handler that invoked the external command or function
	 * <p>
	 * This call clears the contents of the named variable and then sets it up as
	 * an array with the specified values.
	 * <p>
	 * Equivalent to External Interface call: <code>setArray</code>.
	 * 
	 * @param name                       the name of the variable
	 * @param map                        the contents of the variable
	 * @throws IllegalArgumentException  if the name or map is null
	 * @throws ExternalException         if the variable could not be changed
	 */
	public void setVariableAsMap(String name, Map<String,ExternalString> map) throws ExternalException;
	
	/**
	 * Returns the content of a field identified by its name
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>getFieldByName</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the field
	 * @return                           the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws IllegalArgumentException  if the name is null
	 * @throws ExternalException         if the field could not be found
	 */
	public String getTextOfFieldByName(ControlSearchModifier searchModifier, String name) throws ExternalException;
	
	/**
	 * Returns the content of a field identified by its index
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>getFieldByNum</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the field
	 * @return                           the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws ExternalException         if the field could not be found
	 */
	public String getTextOfFieldByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException;
	
	/**
	 * Returns the content of a field identified by its id
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>getFieldById</code>.
	 * 
	 * @param searchModifier            the ControlSearchModifier to apply
	 * @param id                        the short id of the field
	 * @return                          the content of the field (i.e. the value of its <code>text</code> property)
	 * @throws IllegalArgumentException if the id <= 0
	 * @throws ExternalException        if the field could not be found
	 */
	public String getTextOfFieldByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException;
	
	/**
	 * Sets the content of a field identified by its name
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>setFieldByName</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the name or value is null
	 * @throws ExternalException         if the field could not be changed
	 */
	public void setTextOfFieldByName(ControlSearchModifier searchModifier, String name, String value) throws ExternalException;
	
	/**
	 * Sets the content of a field identified by its index
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>setFieldByNum</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the value is <code>null</code>
	 * @throws ExternalException         if the field could not be changed
	 */
	public void setTextOfFieldByNumber(ControlSearchModifier searchModifier, int index, String value) throws ExternalException;
	
	/**
	 * Sets the content of a field identified by its short id
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background field'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>setFieldById</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param id                         the short id of the field
	 * @param value                      the new content of the field
	 * @throws IllegalArgumentException  if the id <= 0 or value is <code>null</code>
	 * @throws ExternalException         if the field could not be changed
	 */
	public void setTextOfFieldByShortId(ControlSearchModifier searchModifier, long id, String value) throws ExternalException;

	/**
	 * Redraws an image identified by its name
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>showImageByName</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param name                       the name of the image
	 * @throws IllegalArgumentException  if the name is null
	 * @throws ExternalException         if the image could not be redrawn
	 */
	public void repaintImageByName(ControlSearchModifier searchModifier, String name) throws ExternalException;

	/**
	 * Redraws an image identified by its index
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>showImageByNum</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param index                      the index of the image
	 * @throws ExternalException         if the image could not be redrawn
	 */
	public void repaintImageByNumber(ControlSearchModifier searchModifier, int index) throws ExternalException;

	/**
	 * Redraws an image identified by its short id
	 * <p>
	 * <ul>
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.CARD</code>, 
	 * then it has the same effect as searching for 'card image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.BACKGROUND</code>, 
	 * then it has the same effect as searching for 'background image'.
	 * <li>If <code>searchModifier</code> is <code>ControlSearchModifier.NONE</code>, 
	 * then no modifier is applied.
	 * </ul>
	 * <p>
	 * Equivalent to External Interface call: <code>showImageById</code>.
	 * 
	 * @param searchModifier             the <code>ControlSearchModifier</code> to apply
	 * @param id                         the short id of the image
	 * @throws IllegalArgumentException  if the id <= 0
	 * @throws ExternalException         if the image could not be redrawn
	 */
	public void repaintImageByShortId(ControlSearchModifier searchModifier, long id) throws ExternalException;

}
