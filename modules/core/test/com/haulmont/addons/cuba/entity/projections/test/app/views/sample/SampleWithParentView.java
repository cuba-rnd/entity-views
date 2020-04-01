package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;

import java.util.UUID;

public interface SampleWithParentView extends Projection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
