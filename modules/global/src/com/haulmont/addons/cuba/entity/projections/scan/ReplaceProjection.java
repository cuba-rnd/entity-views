package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Complementary annotation for entity's {@link com.haulmont.cuba.core.entity.annotation.Extends}. If a projection
 * is annotated with it, it will be replaced in CUBA class hierarchy.
 */
//TODO implement static check for inheritance
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReplaceProjection {
    Class<? extends BaseProjection> value();
}
