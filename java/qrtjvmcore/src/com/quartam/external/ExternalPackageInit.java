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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply this annotation to your <code>ExternalPackage</code>'s optional
 * initialisation method. This method will be called automatically before any
 * of the <code>ExternalCommands</code> or <code>ExternalFunctions</code> in 
 * this <code>ExternalPackage</code> are called.
 * 
 * <p>This initialisation method:
 * <ul>
 * <li>MUST accept a single parameter of class type <code>ExternalInterface</code> 
 * <li>MUST NOT have a return type other than <code>void</code>.
 * </ul>
 * 
 * <p>This annotation MUST NOT be applied to more than one method per 
 * <code>ExternalPackage</code> class.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExternalPackageInit {

}
