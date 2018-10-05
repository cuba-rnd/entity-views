package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

public interface SampleWithParentView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
