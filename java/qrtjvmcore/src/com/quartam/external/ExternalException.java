package com.quartam.external;

/**
 * The <code>ExternalException</code> exception contains both a textual description
 * of the exception that occurred and the cause exception where applicable.
 * 
 * <p> The callback methods in <code>ExternalInterface</code> will throw such an
 * exception if the callback cannot be executed properly.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
@SuppressWarnings("serial")
public class ExternalException extends Exception {
	
	public ExternalException(String description) {
		super(description);
	}
	
	public ExternalException(String description, Throwable cause) {
		super(description, cause);
	}
	
}
