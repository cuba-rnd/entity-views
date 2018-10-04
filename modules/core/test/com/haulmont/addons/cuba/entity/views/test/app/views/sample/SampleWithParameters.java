package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

import java.util.List;

public interface SampleWithParameters extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    List<ParameterNameOnly> getParams();

    List<ParameterNameOnly> getCompParams();

}
