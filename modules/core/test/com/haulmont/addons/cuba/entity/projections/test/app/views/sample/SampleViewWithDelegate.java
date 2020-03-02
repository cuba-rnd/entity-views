package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;

import java.util.UUID;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public interface SampleViewWithDelegate extends BaseProjection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    String getNameUppercase();

}
