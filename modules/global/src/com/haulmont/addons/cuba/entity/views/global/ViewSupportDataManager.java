package com.haulmont.addons.cuba.entity.views.global;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;

/**
 * PoC for Entity Views support in CUBA's DataManager.
 * @see DataManager
 */
public interface ViewSupportDataManager extends DataManager {

    <E extends Entity<K>, V extends BaseEntityView<E>, K> V reload(E entity, Class<V> viewInterface);

    <E extends Entity<K>, V extends BaseEntityView<E>, K> ViewsSupportFluentLoader<E, V, K> loadWithView(Class<V> entityView);

    <V extends BaseEntityView> V createWithView(Class<V> viewInterface);

    <E extends Entity, V extends BaseEntityView<E>> V commit(V entityView);

    <E extends Entity, V extends BaseEntityView<E>, K extends BaseEntityView<E>> K commit(V entityView, Class<K> targetView);

    <E extends Entity, V extends BaseEntityView<E>, K extends BaseEntityView<E>> void remove(V entityView);
}
