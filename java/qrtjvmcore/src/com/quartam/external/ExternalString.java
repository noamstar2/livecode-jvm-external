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
