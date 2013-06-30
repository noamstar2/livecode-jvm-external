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
