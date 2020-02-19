package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;

import java.util.UUID;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public interface SampleViewWithDelegate extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    String getNameUppercase();

}
