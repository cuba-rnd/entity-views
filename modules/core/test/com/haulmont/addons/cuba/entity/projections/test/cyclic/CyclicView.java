package com.haulmont.addons.cuba.entity.projections.test.cyclic;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;

import java.util.UUID;

@SuppressWarnings("unused")//It is used to test bootstrap failure
public interface CyclicView extends BaseProjection<SampleEntity, UUID> {

    String getName();

    CyclicView getParent();

}
