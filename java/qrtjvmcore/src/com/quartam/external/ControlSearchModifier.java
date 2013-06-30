package com.quartam.external;

/**
 * The <code>ControlSearchModifier</code> enumeration is used in the following 
 * <code>ExternalInterface</code> callback methods:
 * <ul>
 * <li><code>getTextOfFieldByName</code>
 * <li><code>getTextOfFieldByIndex</code>
 * <li><code>getTextOfFieldByShortId</code>
 * <li><code>setTextOfFieldByName</code>
 * <li><code>setTextOfFieldByIndex</code>
 * <li><code>setTextOfFieldByShortId</code>
 * <li><code>repaintImageByName</code>
 * <li><code>repaintImageByIndex</code>
 * <li><code>repaintImageByShortId</code>
 * </ul>
 * <p>
 * The ControlSearchModifier determines how the control is located by the engine
 * in the context of the current card of the current stack.
 * <ul>
 * <li><code>ControlSearchModifier.CARD</code> has the same effect as searching 
 * for 'card control'.
 * <li><code>ControlSearchModifier.BACKGROUND</code> has the same effect as 
 * searching for 'background control'.
 * <li><code>ControlSearchModifier.NONE</code> means no modifier is applied.
 * </ul>
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
public enum ControlSearchModifier {
	/**
	 * Using <code>ControlSearchModifier.CARD</code> has the same effect as searching 
     * for 'card control'.
	 */
	CARD, 
	/**
	 * Using <code>ControlSearchModifier.BACKGROUND</code> has the same effect as 
     * searching for 'background control'.
	 */
	BACKGROUND, 
	/**
	 * Using <code>ControlSearchModifier.NONE</code> means no modifier is applied.
	 */
	NONE
}
