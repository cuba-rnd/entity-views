package com.haulmont.cuba.core;

import com.haulmont.cuba.core.app.DataManagerBean;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewSupportDataManager;
import com.haulmont.cuba.core.global.ViewsSupportFluentLoader;
import com.haulmont.cuba.core.views.BaseEntityView;
import com.haulmont.cuba.core.views.factory.EntityViewWrapper;
import com.haulmont.cuba.core.views.scan.ViewsConfiguration;

import javax.inject.Inject;


public class ViewsSupportDataManagerBean extends DataManagerBean implements ViewSupportDataManager {

    @Inject
    private ViewsConfiguration conf;

    @Inject
    private Metadata metadata;

    @Override
    public <E extends Entity<K>, V extends BaseEntityView<E>, K> V reload(E entity, Class<V> viewInterface) {
        View view = conf.getViewByInterface(viewInterface);
        return EntityViewWrapper.wrap(reload(entity, view), viewInterface);
    }

    @Override
    public <E extends Entity<K>, V extends BaseEntityView<E>, K> ViewsSupportFluentLoader<E, V, K> loadWithView(Class<V> viewInterface) {
        Class<? extends Entity> entityClass = conf.getViewByInterface(viewInterface).getEntityClass();
        return new ViewsSupportFluentLoader(entityClass, this, viewInterface);
    }

    @Override
    public <V extends BaseEntityView> V create(Class<V> viewInterface) {
        Class<? extends Entity> c = conf.getViewByInterface(viewInterface).getEntityClass();
        return (V) EntityViewWrapper.wrap(metadata.create(c), viewInterface);
    }

    @Override
    public <E extends Entity, V extends BaseEntityView<E>> V commit(V entityView) {
        View view = conf.getViewByInterface(entityView.getInterfaceClass());
        E savedEntity = commit(entityView.getOrigin(), view);
        return EntityViewWrapper.wrap(savedEntity, entityView.getInterfaceClass());
    }

    @Override
    public <E extends Entity, V extends BaseEntityView<E>, T extends BaseEntityView<E>> T commit(V entityView, Class<T> targetView) {
        View view = conf.getViewByInterface(targetView);
        E savedEntity = commit(entityView.getOrigin(), view);
        return EntityViewWrapper.wrap(savedEntity, targetView);
    }

    @Override
    public <E extends Entity, V extends BaseEntityView<E>, K extends BaseEntityView<E>> void remove(V entityView) {
        remove(entityView.getOrigin());
    }
}
