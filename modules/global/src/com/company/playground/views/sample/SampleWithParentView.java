package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;
import com.haulmont.cuba.core.views.BaseEntityView;

public interface SampleWithParentView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
