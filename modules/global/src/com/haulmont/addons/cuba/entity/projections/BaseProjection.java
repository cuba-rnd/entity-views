package com.haulmont.addons.cuba.entity.projections;

import com.haulmont.addons.cuba.entity.projections.scan.AbstractProjection;
import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;

/**
 * This is the base view, all Entity Views should extend this view, otherwise you won't be able to use
 * them for automatic wrapping and unwrapping entities. Please note that if you want to add a base method for
 * all your entity views you must extend this interface.
 * {@link AbstractProjection} annotation prevents this interface from being registered as a valid Entity View.
 *
 */
@AbstractProjection
public interface BaseProjection<T extends Entity<K>, K> extends Entity<K>, Serializable {

    /**
     * Returns underlying entity a.k.a. "unwraps" it. Please note that unwrapped entity will be partially
     * loaded according to actual Entity View definition.
     * @return underlying entity.
     */
    T getOrigin();

    /**
     * Returns actual Projection Interface class.
     * @param <V> Projection Interface class.
     * @return Effective projection interface class.
     */
    <V extends BaseProjection<T, K>> Class<V> getInterfaceClass();

    /**
     * Applies new projection to an underlying entity. Current implementation may reload entity and
     * does not commit changes neither throws any exceptions, so if you reload modified entity, all changes will be lost.
     * @param targetView view class that should be applied to underlying entity.
     * @param <V> target view instance class.
     * @return target view instance with the same underlying entity.
     */
    <V extends BaseProjection<T, K>> V reload(Class<V> targetView);

}
