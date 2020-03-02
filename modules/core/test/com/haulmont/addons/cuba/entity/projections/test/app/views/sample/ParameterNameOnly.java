package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.EntityParameter;

import java.util.UUID;

public interface ParameterNameOnly extends BaseProjection<EntityParameter, UUID> {

    String getName();

    void setName(String name);

}
