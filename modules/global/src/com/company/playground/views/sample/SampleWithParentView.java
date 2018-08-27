package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;

import java.util.UUID;

public interface SampleWithParentView extends BaseEntityView<SampleEntity> {

    UUID getId();

    String getName();

    void setName(String name);

    SampleMinimalView getParent();

}
