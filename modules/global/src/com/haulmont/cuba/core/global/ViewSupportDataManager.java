package com.haulmont.cuba.core.global;

import com.company.playground.views.sample.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;

public interface ViewSupportDataManager extends DataManager {

    <E extends Entity<K>, V extends BaseEntityView<E>, K> V reload(E entity, Class<V> viewInterface);

    <E extends Entity<K>, V extends BaseEntityView<E>, K> ViewsSupportFluentLoader<E, V, K> load(Class<E> entityClass, Class<V> entityView);

}
