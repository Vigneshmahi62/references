package com.precision.mdm.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.precision.mdm.data.aspects.LoggingAspect;

/**
 * Custom annotation used for logging execution time of any method
 *
 * @see LoggingAspect
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Timer {
}
