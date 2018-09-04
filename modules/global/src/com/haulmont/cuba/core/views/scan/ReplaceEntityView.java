package com.haulmont.cuba.core.views.scan;

import com.haulmont.cuba.core.views.BaseEntityView;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReplaceEntityView {
    Class<? extends BaseEntityView> value();
}
