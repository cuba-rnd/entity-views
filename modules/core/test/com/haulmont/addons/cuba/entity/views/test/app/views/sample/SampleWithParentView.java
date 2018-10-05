package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

import java.util.UUID;

public interface SampleWithParentView extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
