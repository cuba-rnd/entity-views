package com.haulmont.addons.cuba.entity.views.global;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.EntityStates;

/**
 * The class adds support for Entity Views to determine the state of an underlying entity.
 * @see EntityStates
 */
public class ViewsSupportEntityStates  extends EntityStates {

    /**
     * @see EntityStates#isNew(Object)
     */
    public <E extends Entity, V extends BaseEntityView<E>> boolean isNew(V entityView) {
        return isNew(entityView.getOrigin());
    }

    /**
     * @see EntityStates#isDetached(Object)
     */
    public <E extends Entity, V extends BaseEntityView<E>> boolean isDetached(V entityView) {
        return isDetached(entityView.getOrigin());
    }

    /**
     * @see EntityStates#isManaged(Object)
     */
    public <E extends Entity, V extends BaseEntityView<E>> boolean isManaged(V entityView) {
        return isManaged(entityView.getOrigin());
    }

}
