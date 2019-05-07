package com.haulmont.addons.cuba.entity.views;

import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfiguration;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfigurationBean;
import com.haulmont.cuba.core.app.DataManagerBean;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.EntitySet;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class ViewsSupportDataManagerBean extends DataManagerBean {

    @Inject
    private ViewsConfiguration viewsConfiguration;

    @Override
    public <E extends Entity<K>, K> FluentLoader<E, K> load(Class<E> entityClass) {
        boolean isEntityView = BaseEntityView.class.isAssignableFrom(entityClass);
        if (isEntityView) {
            ViewsConfigurationBean.ViewInterfaceInfo viewInterfaceDefinition =
                    viewsConfiguration.getViewInterfaceDefinition((Class<? extends BaseEntityView>) entityClass);
            entityClass = (Class<E>) viewInterfaceDefinition.getEntityClass();
            View view = viewInterfaceDefinition.getView();
            return new FluentLoader<>(entityClass, this).view(view);
        }
        return new FluentLoader<>(entityClass, this);
    }

    @Nullable
    @Override
    public <E extends Entity> E load(LoadContext<E> context) {
        E entity = super.load(context);
        View contextView = context.getView();
        if (contextView != null) {
            ViewsConfigurationBean.ViewInterfaceInfo viewInfo = viewsConfiguration.getViewInfoByView(contextView);
            if (viewInfo != null) {
                return (E) EntityViewWrapper.wrap(entity, viewInfo.getViewInterface());
            }
        }
        return entity;
    }

    @Override
    public <E extends Entity<K>, K> FluentLoader.ById<E, K> load(Id<E, K> entityId) {
        Class<E> entityClass = entityId.getEntityClass();
        K idValue = entityId.getValue();
        boolean isEntityView = BaseEntityView.class.isAssignableFrom(entityClass);
        if (isEntityView) {
            ViewsConfigurationBean.ViewInterfaceInfo viewInterfaceDefinition =
                    viewsConfiguration.getViewInterfaceDefinition((Class<? extends BaseEntityView>) entityClass);
            entityClass = (Class<E>) viewInterfaceDefinition.getEntityClass();
            View view = viewInterfaceDefinition.getView();
            return new FluentLoader<>(entityClass, this).view(view).id(idValue);
        }
        return new FluentLoader<>(entityClass, this).id(idValue);
    }


    @Override
    public <E extends Entity> List<E> loadList(LoadContext<E> context) {
        List<E> entityList = super.loadList(context);
        View contextView = context.getView();
        if (contextView != null) {
            ViewsConfigurationBean.ViewInterfaceInfo viewInfo = viewsConfiguration.getViewInfoByView(contextView);
            if (viewInfo != null) {
                entityList = (List<E>) entityList.stream().map(e -> EntityViewWrapper.wrap(e, viewInfo.getViewInterface())).collect(Collectors.toList());
            }
        }
        return entityList;
    }

    @Override
    public <E extends Entity> E commit(E entity, @Nullable View view) {
        CommitContext context = new CommitContext().addInstanceToCommit(entity, view);
        EntitySet commit = commit(context);
        if (entity instanceof BaseEntityView) {
            Entity committedEntity = commit.get(((BaseEntityView) entity).getOrigin());
            BaseEntityView entityView = EntityViewWrapper.wrap(committedEntity, ((BaseEntityView) entity).getInterfaceClass());
            return (E) entityView;
        }
        return commit.get(entity);
    }


    @Override
    public EntitySet commit(CommitContext context) {

        Collection<Entity> entitiesToCommit = context.getCommitInstances().stream().map(e -> {
                    if (e instanceof BaseEntityView)
                        return ((BaseEntityView) e).getOrigin();
                    else return e;
                }
        ).collect(Collectors.toList());
        context.setCommitInstances(entitiesToCommit);

        Collection<Entity> entitiesToRemove = context.getRemoveInstances().stream().map(e -> {
                    if (e instanceof BaseEntityView)
                        return ((BaseEntityView) e).getOrigin();
                    else return e;
                }
        ).collect(Collectors.toList());
        context.setRemoveInstances(entitiesToRemove);

        EntitySet commitSet = super.commit(context);

        return commitSet;
    }

    @Override
    public void remove(Entity entity) {
        Entity entityToRemove =
                entity instanceof BaseEntityView ? ((BaseEntityView) entity).getOrigin() : entity;
        super.remove(entityToRemove);
    }

    @Override
    public <T> T create(Class<T> entityClass) {
        if (BaseEntityView.class.isAssignableFrom(entityClass)) {
            ViewsConfigurationBean.ViewInterfaceInfo viewInterfaceDefinition
                    = viewsConfiguration.getViewInterfaceDefinition((Class<BaseEntityView>) entityClass);
            Entity entity = super.create(viewInterfaceDefinition.getEntityClass());
            return (T) EntityViewWrapper.wrap(
                    entity, viewInterfaceDefinition.getViewInterface());
        }
        return super.create(entityClass);
    }
}
