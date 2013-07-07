package com.quartam.external;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply this annotation to a method in an <code>ExternalPackage</code> class
 * that has been made available as an <code>ExternalCommand</code> or <code>ExternalFunction</code>,
 * to disambiguate its name.
 * 
 * @author  Jan Schenkel
 * @version v1.0.0
 * @since   1.0
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExternalAlias {
	String value();
}
