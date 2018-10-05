package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Complementary annotation for entity's {@link com.haulmont.cuba.core.entity.annotation.Extends}. If a view
 * is annotated with it, it will be replaced in CUBA class hierarchy.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReplaceEntityView {
    Class<? extends BaseEntityView> value();
}
