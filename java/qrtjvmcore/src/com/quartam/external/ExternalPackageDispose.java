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
 * disposal method. This method will be called automatically when the 
 * <code>ExternalLibrary</code> containing the <code>ExternalPackage</code>
 * is unloaded and provides the ideal place to clean-up any resources
 * that are loaded for this <code>ExternalPackage</code>.
 * 
 * <p>This disposal method MUST NOT accept any parameters and MUST NOT
 * have a return type other than <code>void</code>.  
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
public @interface ExternalPackageDispose {

}
