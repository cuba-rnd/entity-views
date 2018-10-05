package com.haulmont.addons.cuba.entity.views.test.cyclic;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

import java.util.UUID;

@SuppressWarnings("unused")//It is used to test bootstrap failure
public interface CyclicView extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    CyclicView getParent();

}
