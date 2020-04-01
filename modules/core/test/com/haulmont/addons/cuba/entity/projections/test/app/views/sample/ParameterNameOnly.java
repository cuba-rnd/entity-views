package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.EntityParameter;

import java.util.UUID;

public interface ParameterNameOnly extends Projection<EntityParameter, UUID> {

    String getName();

    void setName(String name);

}
