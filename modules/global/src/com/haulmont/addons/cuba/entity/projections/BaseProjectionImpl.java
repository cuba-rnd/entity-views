package com.haulmont.addons.cuba.entity.projections;

import com.haulmont.addons.cuba.entity.projections.factory.EntityProjectionWrapper;
import com.haulmont.addons.cuba.entity.projections.scan.ProjectionsConfiguration;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public abstract class BaseProjectionImpl<E extends Entity<K>, V extends BaseProjection<E, K>, K> implements BaseProjection<E, K> {

    protected final Logger log = LoggerFactory.getLogger(BaseProjectionImpl.class);

    protected E entity;
    private boolean needReload;
    private final Class<V> projectionInterface;
    private View view;

    public BaseProjectionImpl(E entity, Class<V> projectionInterface) {
        this.entity = entity;
        this.projectionInterface = projectionInterface;
        view = AppBeans.get(ProjectionsConfiguration.class).getViewByProjection(projectionInterface);
        this.needReload = !AppBeans.get(EntityStates.class).isLoadedWithView(entity, view);
    }

    public View getView() {
        return view;
    }

    protected void doReload() {
        if (needReload) {
            log.info("Reloading entity {} using view {}", entity, view);
            DataManager dm = AppBeans.get(DataManager.class);
            E reloaded = dm.reload(entity, view);
            entity = (reloaded instanceof BaseProjection) ? (E)((BaseProjection) reloaded).getOrigin() : reloaded;
            log.info("Entity: {} class is: {}", entity, entity.getClass());
            needReload = false;
        }
    }

    @Override
    public E getOrigin() {
        return entity;
    }

    @Override
    public Class<V> getInterfaceClass() {
        return projectionInterface;
    }

    @Override
    public <U extends BaseProjection<E, K>> U reload(Class<U> targetView) {
        if (projectionInterface.isAssignableFrom(targetView)) {
            //noinspection unchecked
            return (U) this;
        }
        return EntityProjectionWrapper.wrap(entity, targetView);
    }

    @Override
    public K getId() {
        return entity.getId();
    }

    @Override
    public MetaClass getMetaClass() {
        return entity.getMetaClass();
    }

    @Override
    @Deprecated
    public String getInstanceName() {
        return entity.getInstanceName();
    }

    @Override
    @Nullable
    public <T> T getValue(String name) {
        return entity.getValue(name);
    }

    @Override
    public void setValue(String name, Object value) {
        entity.setValue(name, value);
    }

    @Override
    @Nullable
    public <T> T getValueEx(String propertyPath) {
        return entity.getValueEx(propertyPath);
    }

    @Override
    @Nullable
    public <T> T getValueEx(BeanPropertyPath propertyPath) {
        return entity.getValueEx(propertyPath);
    }

    @Override
    public void setValueEx(String propertyPath, Object value) {
        entity.setValueEx(propertyPath, value);
    }

    @Override
    public void setValueEx(BeanPropertyPath propertyPath, Object value) {
        entity.setValueEx(propertyPath, value);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        entity.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        entity.removePropertyChangeListener(listener);
    }

    @Override
    public void removeAllListeners() {
        entity.removeAllListeners();
    }
}
