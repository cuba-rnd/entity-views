package com.haulmont.cuba.core;

import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.BaseEntityView;
import com.company.playground.views.scan.ViewsConfiguration;
import com.haulmont.cuba.core.app.DataManagerBean;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewSupportDataManager;
import com.haulmont.cuba.core.global.ViewsSupportFluentLoader;

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
    public <E extends Entity<K>, V extends BaseEntityView<E>, K> ViewsSupportFluentLoader<E, V, K> load(Class<E> entityClass, Class<V> viewInterface) {
        return new ViewsSupportFluentLoader<>(entityClass, this, viewInterface);
    }

    @Override
    public <E extends Entity<K>, V extends BaseEntityView<E>, K> V create(Class<E> entityClass, Class<V> viewInterface) {
        return EntityViewWrapper.wrap(metadata.create(entityClass), viewInterface);
    }

    @Override
    public <E extends Entity, V extends BaseEntityView<E>> V commit(V entityView) {
        E savedEntity = super.commit(entityView.getOrigin());
        return EntityViewWrapper.wrap(savedEntity, entityView.getInterfaceClass());
    }

    @Override
    public <E extends Entity, V extends BaseEntityView<E>, K extends BaseEntityView<E>> K commit(V entityView, Class<K> targetView) {
        E savedEntity = super.commit(entityView.getOrigin());
        return EntityViewWrapper.wrap(savedEntity, targetView);
    }

}
