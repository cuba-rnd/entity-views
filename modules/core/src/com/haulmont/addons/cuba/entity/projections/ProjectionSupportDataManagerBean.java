package com.haulmont.addons.cuba.entity.projections;

import com.haulmont.addons.cuba.entity.projections.factory.EntityProjectionWrapper;
import com.haulmont.addons.cuba.entity.projections.scan.ProjectionsConfiguration;
import com.haulmont.addons.cuba.entity.projections.scan.ProjectionsConfigurationBean;
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


public class ProjectionSupportDataManagerBean extends DataManagerBean {

    @Inject
    private ProjectionsConfiguration projectionsConfiguration;

    @Override
    public <E extends Entity<K>, K> FluentLoader<E, K> load(Class<E> entityClass) {
        boolean isProjection = BaseProjection.class.isAssignableFrom(entityClass);
        if (isProjection) {
            ProjectionsConfigurationBean.ProjectionInfo projectionInfo =
                    projectionsConfiguration.getProjectionInfo((Class<? extends BaseProjection>) entityClass);
            entityClass = (Class<E>) projectionInfo.getEntityClass();
            View view = projectionInfo.getView();
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
            ProjectionsConfigurationBean.ProjectionInfo viewInfo = projectionsConfiguration.getViewInfoByView(contextView);
            if (viewInfo != null) {
                return (E) EntityProjectionWrapper.wrap(entity, viewInfo.getProjectionInterface());
            }
        }
        return entity;
    }

    @Override
    public <E extends Entity<K>, K> FluentLoader.ById<E, K> load(Id<E, K> entityId) {
        Class<E> entityClass = entityId.getEntityClass();
        K idValue = entityId.getValue();
        boolean isProjection = BaseProjection.class.isAssignableFrom(entityClass);
        if (isProjection) {
            ProjectionsConfigurationBean.ProjectionInfo viewInterfaceDefinition =
                    projectionsConfiguration.getProjectionInfo((Class<? extends BaseProjection>) entityClass);
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
            ProjectionsConfigurationBean.ProjectionInfo viewInfo = projectionsConfiguration.getViewInfoByView(contextView);
            if (viewInfo != null) {
                entityList = (List<E>) entityList.stream().map(e -> EntityProjectionWrapper.wrap(e, viewInfo.getProjectionInterface())).collect(Collectors.toList());
            }
        }
        return entityList;
    }

    @Override
    public <E extends Entity> E commit(E entity, @Nullable View view) {
        CommitContext context = new CommitContext().addInstanceToCommit(entity, view);
        EntitySet commit = commit(context);
        if (entity instanceof BaseProjection) {
            Entity committedEntity = commit.get(((BaseProjection) entity).getOrigin());
            BaseProjection entityView = EntityProjectionWrapper.wrap(committedEntity, ((BaseProjection) entity).getInterfaceClass());
            return (E) entityView;
        }
        return commit.get(entity);
    }


    @Override
    public EntitySet commit(CommitContext context) {

        Collection<Entity> entitiesToCommit = context.getCommitInstances().stream().map(e -> {
                    if (e instanceof BaseProjection)
                        return ((BaseProjection) e).getOrigin();
                    else return e;
                }
        ).collect(Collectors.toList());
        context.setCommitInstances(entitiesToCommit);

        Collection<Entity> entitiesToRemove = context.getRemoveInstances().stream().map(e -> {
                    if (e instanceof BaseProjection)
                        return ((BaseProjection) e).getOrigin();
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
                entity instanceof BaseProjection ? ((BaseProjection) entity).getOrigin() : entity;
        super.remove(entityToRemove);
    }


    @Override
    public <T extends Entity> T create(Class<T> entityClass) {
        if (BaseProjection.class.isAssignableFrom(entityClass)) {
            ProjectionsConfigurationBean.ProjectionInfo viewInterfaceDefinition
                    = projectionsConfiguration.getProjectionInfo((Class<BaseProjection>) entityClass);
            Entity entity = super.create(viewInterfaceDefinition.getEntityClass());
            return (T) EntityProjectionWrapper.wrap(
                    entity, viewInterfaceDefinition.getProjectionInterface());
        }
        return super.create(entityClass);
    }
}
