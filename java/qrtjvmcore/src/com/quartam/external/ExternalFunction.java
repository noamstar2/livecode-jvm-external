package com.quartam.external;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply this annotation to a method in an <code>ExternalPackage</code> class
 * that you want to make available as an <code>ExternalFunction</code>.
 * <ul>
 * <li>Such a method MUST NOT have a return type other than <code>String</code>.
 * <li>Such a method MUST have either no parameters or a <code>String[]</code> parameter.
 * </ul>
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExternalFunction {

}
