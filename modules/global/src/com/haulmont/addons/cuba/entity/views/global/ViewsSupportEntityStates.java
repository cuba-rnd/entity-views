package com.haulmont.addons.cuba.entity.views.global;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.EntityStates;

/**
 * The class adds support for Entity Views to determine the state of an underlying entity.
 * @see EntityStates
 */
public class ViewsSupportEntityStates extends EntityStates {

    /**
     * Determines whether the instance is <em>New</em>, i.e. just created and not stored in database yet.
     * @see EntityStates#isNew(Object)
     *
     * @param entityView - entity wrapped with a entity view interface.
     * @param <E> - entity class.
     * @param <V> - entity view interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is a new persistent entity, or if it is actually in Managed state
     *           but newly-persisted in this transaction <br>
     *         - true if the instance is a new non-persistent entity never returned from DataManager <br>
     *         - false otherwise.
     */
    public <E extends Entity<K>, V extends BaseEntityView<E, K>, K> boolean isNew(V entityView) {
        return isNew(entityView.getOrigin());
    }


    /**
     * Determines whether the instance is <em>Detached</em>, i.e. stored in database but not attached to a persistence
     * context at the moment.
     * @see EntityStates#isDetached(Object)
     *
     * @param entityView - entity wrapped with a entity view interface.
     * @param <E> - entity class.
     * @param <V> - entity view interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is detached,<br>
     *     - false if it is New or Managed, or if it is not a persistent entity.
     */
    public <E extends Entity<K>, V extends BaseEntityView<E, K>, K> boolean isDetached(V entityView) {
        return isDetached(entityView.getOrigin());
    }

    /**
     * Determines whether the instance is <em>Managed</em>, i.e. attached to a persistence context.
     * @see EntityStates#isManaged(Object)
     * @param entityView - entity wrapped with a entity view interface.
     * @param <E> - entity class.
     * @param <V> - entity view interface class.
     * @param <K> - entity ID key class.
     * @return - true if the instance is managed,<br>
     *         - false if it is New (and not yet persisted) or Detached, or if it is not a persistent entity.
     */
    public <E extends Entity<K>, V extends BaseEntityView<E, K>, K> boolean isManaged(V entityView) {
        return isManaged(entityView.getOrigin());
    }

}
