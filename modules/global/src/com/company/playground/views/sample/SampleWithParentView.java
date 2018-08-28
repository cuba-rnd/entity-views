package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;

public interface SampleWithParentView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
