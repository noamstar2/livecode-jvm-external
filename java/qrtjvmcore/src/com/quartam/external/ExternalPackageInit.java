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
