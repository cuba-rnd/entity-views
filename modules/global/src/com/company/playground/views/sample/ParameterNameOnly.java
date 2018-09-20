package com.company.playground.views.sample;

import com.company.playground.entity.EntityParameter;
import com.haulmont.cuba.core.views.BaseEntityView;

public interface ParameterNameOnly extends BaseEntityView<EntityParameter> {

    String getName();

    void setName(String name);

}
