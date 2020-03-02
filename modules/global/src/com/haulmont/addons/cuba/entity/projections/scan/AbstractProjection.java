package com.haulmont.addons.cuba.entity.projections.scan;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes projection interface from scanning. Should be used in case of BaseProjection extension.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AbstractProjection {
}
