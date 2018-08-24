package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;

public interface CyclicView extends BaseEntityView<SampleEntity> {

    String getName();

    CyclicView getParent();

}
