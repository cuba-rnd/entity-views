package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;

import java.util.UUID;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public interface SampleViewWithDelegate extends Projection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    String getNameUppercase();

}
