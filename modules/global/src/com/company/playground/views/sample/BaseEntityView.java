package com.company.playground.views.sample;

import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
public interface BaseEntityView<T extends Entity> extends Serializable {
    T getOrigin();

    BaseEntityView<T> transform(BaseEntityView<T> targetView);
}
