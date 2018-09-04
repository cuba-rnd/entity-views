package com.haulmont.cuba.core.views;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.views.scan.AbstractEntityView;

import java.io.Serializable;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
@AbstractEntityView
public interface BaseEntityView<T extends Entity> extends Entity, Serializable {

    T getOrigin();

    <V extends BaseEntityView<T>> Class<V> getInterfaceClass();

    <V extends BaseEntityView<T>> V transform(Class<V> targetView);

}
