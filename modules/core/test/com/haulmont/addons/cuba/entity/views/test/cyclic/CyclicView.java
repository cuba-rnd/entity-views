package com.haulmont.addons.cuba.entity.views.test.cyclic;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

@SuppressWarnings("unused")//It is used to test bootstrap failure
public interface CyclicView extends BaseEntityView<SampleEntity> {

    String getName();

    CyclicView getParent();

}
