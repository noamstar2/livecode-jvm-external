/**
 * 
 */
package com.quartam.external;

/**
 * The <code>ExternalString</code> class is provided as a wrapper around 
 * a byte array, and simplifies communication with the underlying engine.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
public class ExternalString {
	
	private final byte[] bytes;
	
	/**
	 * @param bytes
	 * @param length
	 */
	public ExternalString(byte[] bytes) {
		super();
		this.bytes = bytes;
	}

	/**
	 * @return the bytes
	 */
	public byte[] getBytes() {
		return this.bytes;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return this.bytes.length;
	}

}
