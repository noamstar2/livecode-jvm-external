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
