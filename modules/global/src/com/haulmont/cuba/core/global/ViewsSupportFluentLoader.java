package com.haulmont.cuba.core.global;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.views.BaseEntityView;
import com.haulmont.cuba.core.views.factory.EntityViewWrapper;
import com.haulmont.cuba.core.views.scan.ViewsConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class ViewsSupportFluentLoader<E extends Entity<K>, V extends BaseEntityView<E>, K> {

    private final Class<V> viewInterface;

    private final FluentLoader<E, K> delegate;

    public ViewsSupportFluentLoader(Class<E> entityClass, DataManager dataManager, Class<V> viewInterface) {
        delegate = new FluentLoader<>(entityClass, dataManager);
        this.viewInterface = viewInterface;
    }

    public List<V> list() {
        View view = AppBeans.get(ViewsConfiguration.class).getViewByInterface(viewInterface);
        return delegate.view(view).list().stream().map((e) -> EntityViewWrapper.wrap(e, viewInterface)).collect(Collectors.toList());
    }

    public ViewsSupportFluentLoader<E, V, K>.ViewQuery query(String queryString) {
        return new ViewQuery(queryString);
    }

    public class ViewQuery {

        private final FluentLoader<E, K>.ByQuery delegateQuery;

        ViewQuery(String queryString) {
            delegateQuery = delegate.query(queryString);
        }

        public ViewQuery parameter(String name, Object value) {
            delegateQuery.parameter(name, value);
            return this;
        }

        public List<V> list() {
            View view = AppBeans.get(ViewsConfiguration.class).getViewByInterface(viewInterface);
            return delegateQuery.view(view).list().stream().map((e) -> EntityViewWrapper.wrap(e, viewInterface)).collect(Collectors.toList());
        }
    }

}
