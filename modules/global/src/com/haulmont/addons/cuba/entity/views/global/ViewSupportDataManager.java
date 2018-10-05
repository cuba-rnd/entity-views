package com.haulmont.addons.cuba.entity.views.global;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;

/**
 * PoC for Entity Views support in CUBA's DataManager.
 * @see DataManager
 */
public interface ViewSupportDataManager extends DataManager {

    <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V reload(E entity, Class<V> viewInterface);

    <E extends Entity<K>, V extends BaseEntityView<E, K>, K> ViewsSupportFluentLoader<E, V, K> loadWithView(Class<V> entityView);

    <V extends BaseEntityView> V createWithView(Class<V> viewInterface);

    <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V commit(V entityView);

    <E extends Entity<K>, V extends BaseEntityView<E, K>, R extends BaseEntityView<E, K>, K> R commit(V entityView, Class<R> targetView);

    <E extends Entity<K>, V extends BaseEntityView<E, K>, R extends BaseEntityView<E, K>, K> void remove(V entityView);
}
