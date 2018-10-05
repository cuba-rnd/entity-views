package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter;

public interface ParameterNameOnly extends BaseEntityView<EntityParameter> {

    String getName();

    void setName(String name);

}
