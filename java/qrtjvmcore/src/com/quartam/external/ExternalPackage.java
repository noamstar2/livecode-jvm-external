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
 * Apply this annotation to a class whose methods you want to make available
 * as <code>ExternalCommand</code>s and <code>ExternalFunction</code>s.
 * <p>
 * Classes annotated with <code>ExternalPackage</code>:
 * <ul>
 * <li>MUST have a default constructor
 * <li>MUST have at least one <code>ExternalCommand</code> or 
 * <code>ExternalFunction</code> annotated method
 * <li>MAY annotate one method as <code>ExternalPackageInit</code>
 * to designate an initialisation method
 * <li>MAY annotate one method as <code>ExternalPackageDispose</code>
 * to designate a disposal method
 * </ul>
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExternalPackage {
	
	String author();
	String date();
	int currentRevision() default 1;
	String lastModified() default "N/A";
	String lastModifiedBy() default "N/A";
	String downloadURL() default "N/A";

}
