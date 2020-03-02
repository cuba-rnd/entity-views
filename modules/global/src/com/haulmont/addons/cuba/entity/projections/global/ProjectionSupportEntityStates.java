package com.haulmont.addons.cuba.entity.projections.global;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.EntityStates;

/**
 * The class adds support for Projections to determine the state of an underlying entity.
 * @see EntityStates
 */
public class ProjectionSupportEntityStates extends EntityStates {


    @Override
    public boolean isNew(Object entity) {
        if (entity instanceof BaseProjection) {
            return super.isNew(((BaseProjection) entity).getOrigin());
        } else {
            return super.isNew(entity);
        }
    }

    @Override
    public boolean isManaged(Object entity) {
        if (entity instanceof BaseProjection) {
            return super.isManaged(((BaseProjection) entity).getOrigin());
        } else {
            return super.isManaged(entity);
        }
    }

    @Override
    public boolean isDetached(Object entity) {
        if (entity instanceof BaseProjection) {
            return super.isDetached(((BaseProjection) entity).getOrigin());
        } else {
            return super.isDetached(entity);
        }
    }

    /**
     * Determines whether the instance is <em>New</em>, i.e. just created and not stored in database yet.
     * @see EntityStates#isNew(Object)
     *
     * @param projection - entity wrapped with a projection interface.
     * @param <E> - entity class.
     * @param <V> - projection interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is a new persistent entity, or if it is actually in Managed state
     *           but newly-persisted in this transaction <br>
     *         - true if the instance is a new non-persistent entity never returned from DataManager <br>
     *         - false otherwise.
     */
    public <E extends Entity<K>, V extends BaseProjection<E, K>, K> boolean isNew(V projection) {
        return isNew(projection.getOrigin());
    }


    /**
     * Determines whether the instance is <em>Detached</em>, i.e. stored in database but not attached to a persistence
     * context at the moment.
     * @see EntityStates#isDetached(Object)
     *
     * @param projection - entity wrapped with a projection interface.
     * @param <E> - entity class.
     * @param <V> - projection interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is detached,<br>
     *     - false if it is New or Managed, or if it is not a persistent entity.
     */
    public <E extends Entity<K>, V extends BaseProjection<E, K>, K> boolean isDetached(V projection) {
        return isDetached(projection.getOrigin());
    }

    /**
     * Determines whether the instance is <em>Managed</em>, i.e. attached to a persistence context.
     * @see EntityStates#isManaged(Object)
     * @param projection - entity wrapped with a projection interface.
     * @param <E> - entity class.
     * @param <V> - projection interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is managed,<br>
     *         - false if it is New (and not yet persisted) or Detached, or if it is not a persistent entity.
     */
    public <E extends Entity<K>, V extends BaseProjection<E, K>, K> boolean isManaged(V projection) {
        return isManaged(projection.getOrigin());
    }

}
