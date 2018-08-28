package com.haulmont.cuba.core.global;

import com.company.playground.views.sample.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;

public class ViewsSupportEntityStates  extends EntityStates {

    public <E extends Entity, V extends BaseEntityView<E>> boolean isNew(V entityView) {
        return isNew(entityView.getOrigin());
    }

    public <E extends Entity, V extends BaseEntityView<E>> boolean isDetached(V entityView) {
        return isDetached(entityView.getOrigin());
    }

    public <E extends Entity, V extends BaseEntityView<E>> boolean isManaged(V entityView) {
        return isManaged(entityView.getOrigin());
    }

}
