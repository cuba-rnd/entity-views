package com.haulmont.addons.cuba.entity.views.gui.model.impl;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.model.impl.DataContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataContextViewSupportImpl extends DataContextImpl {

    private static final Logger log = LoggerFactory.getLogger(DataContextViewSupportImpl.class);

    public DataContextViewSupportImpl(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected Entity internalMerge(Entity entity, Set<Entity> mergedSet, boolean isRoot) {
        Map<Object, Entity> entityMap = content.computeIfAbsent(entity.getClass(), aClass -> new HashMap<>());
        Entity managed = entityMap.get(entity.getId());

        if (mergedSet.contains(entity)) {
            if (managed != null) {
                return managed;
            } else {
                // should never happen
                log.debug("Instance was merged but managed instance is null: {}", entity);
            }
        }

        boolean isEntityView = entity instanceof BaseEntityView;

        mergedSet.add(entity);

        if (managed == null) {

            Entity src = isEntityView ? ((BaseEntityView)entity).getOrigin() : entity;

            managed = copyEntity(src);
            entityMap.put(managed.getId(), managed);

            mergeState(src, managed, mergedSet, isRoot);

            managed.addPropertyChangeListener(propertyChangeListener);

            if (getEntityStates().isNew(managed)) {
                modifiedInstances.add(managed);
                fireChangeListener(managed);
            }
            return wrapEntity(entity, managed);
        } else {
            if (managed.getId() == null) {
                throw new IllegalStateException("DataContext already contains an instance with null id: " + managed);
            }

            if (managed != entity) {
                mergeState(entity, managed, mergedSet, isRoot);
            }
            return wrapEntity(entity, managed);
        }
    }

    private Entity wrapEntity(Entity srcEntity, Entity dst) {
        if (srcEntity instanceof BaseEntityView) {
            Class<? extends BaseEntityView> interfaceClass = ((BaseEntityView)srcEntity).getInterfaceClass();
            return EntityViewWrapper.wrap(dst, interfaceClass);
        } else {
            return dst;
        }
    }

}
