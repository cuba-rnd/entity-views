package com.company.playground.views.scan;

import org.apache.commons.lang.StringUtils;

import java.lang.annotation.*;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityView {
    String name() default StringUtils.EMPTY;
}
