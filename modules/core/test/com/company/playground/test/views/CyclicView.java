package com.company.playground.test.views;

import com.company.playground.entity.SampleEntity;
import com.haulmont.cuba.core.views.BaseEntityView;

@SuppressWarnings("unused")//It is used to test bootstrap failure
public interface CyclicView extends BaseEntityView<SampleEntity> {

    String getName();

    CyclicView getParent();

}
