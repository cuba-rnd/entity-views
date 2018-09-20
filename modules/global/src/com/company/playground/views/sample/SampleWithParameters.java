package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;
import com.haulmont.cuba.core.views.BaseEntityView;

import java.util.List;

public interface SampleWithParameters extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    List<ParameterNameOnly> getParams();

    List<ParameterNameOnly> getCompParams();

}
