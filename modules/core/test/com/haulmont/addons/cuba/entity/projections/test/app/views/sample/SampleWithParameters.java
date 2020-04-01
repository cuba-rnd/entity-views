package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;

import java.util.List;
import java.util.UUID;

public interface SampleWithParameters extends Projection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    List<ParameterNameOnly> getParams();

    List<ParameterNameOnly> getCompParams();

}
