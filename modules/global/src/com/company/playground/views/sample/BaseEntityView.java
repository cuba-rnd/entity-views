package com.company.playground.views.sample;

import com.company.playground.views.scan.AbstractEntityView;
import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
@AbstractEntityView
public interface BaseEntityView<T extends Entity> extends Entity, Serializable {

    T getOrigin();

    <V extends BaseEntityView<T>> V transform(Class<? extends BaseEntityView<T>> targetView);

}
