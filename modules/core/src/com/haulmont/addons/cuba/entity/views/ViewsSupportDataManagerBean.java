package com.haulmont.addons.cuba.entity.views;

import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfiguration;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfigurationBean;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.DataManagerBean;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.EntitySet;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class ViewsSupportDataManagerBean extends DataManagerBean {

    @Inject
    private ViewsConfiguration viewsConfiguration;

    @Nullable
    @Override
    public <E extends Entity> E load(LoadContext<E> context) {
        E load = super.load(context);
        return load;
    }

    @Override
    public <E extends Entity> List<E> loadList(LoadContext<E> context) {
        Class entityClass = metadata.getClass(context.getEntityMetaClass()).getJavaClass();
        boolean isEntityView = BaseEntityView.class.isAssignableFrom(entityClass);
        if (isEntityView) {
            context.setView(viewsConfiguration.getViewByInterface(entityClass));
        }
        List<E> es = super.loadList(context);
        if (isEntityView) {
            es = (List<E>)es.stream().map( e -> EntityViewWrapper.wrap(e, (Class<BaseEntityView>)entityClass)).collect(Collectors.toList());
        }
        return es;
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

        Collection<Entity> entities = context.getCommitInstances().stream().map(e -> {
                    if (e instanceof BaseEntityView)
                        return ((BaseEntityView) e).getOrigin();
                    else return e;
                }
        ).collect(Collectors.toList());

        context.setCommitInstances(entities);

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
            return (T)EntityViewWrapper.wrap(
                    entity, viewInterfaceDefinition.getViewInterface());
        }
        return super.create(entityClass);
    }
}
