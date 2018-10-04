package com.haulmont.addons.cuba.entity.views;

import com.haulmont.addons.cuba.entity.views.scan.AbstractEntityView;
import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;

/**
 * This is the base view, all Entity Views should extend this view, otherwise you won't be able to use
 * them for automatic wrapping and unwrapping entities. Please note that if you want to add a base method for
 * all your entity views you must extend this interface.
 * {@link AbstractEntityView} annotation prevents this interface from being registered as a valid Entity View.
 *
 */
@AbstractEntityView
public interface BaseEntityView<T extends Entity> extends Entity, Serializable {

    /**
     * Returns underlying entity a.k.a. "unwraps" it. Please note that unwrapped entity will be partially
     * loaded according to actual Entity View definition.
     * @return underlying entity.
     */
    T getOrigin();

    /**
     * Returns actual Entity View Interface class because effective
     * interface instance's class is {@link java.lang.reflect.Proxy}
     * @param <V> Entity View Interface class.
     * @return Effective view interface class.
     */
    <V extends BaseEntityView<T>> Class<V> getInterfaceClass();

    /**
     * Applies new view interface to an underlying entity. Current implementation may relod entity and
     * does not commit changes neither throws any exceptions, so if you reload modified entity, all changes will be lost.
     * @param targetView view class that should be applied to underlying entity.
     * @param <V> target view instance class.
     * @return target view instance with the same underlying entity.
     */
    <V extends BaseEntityView<T>> V reload(Class<V> targetView);

}
