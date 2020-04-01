package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;
import com.haulmont.chile.core.annotations.MetaProperty;

import java.util.UUID;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public interface SampleMinimalView extends Projection<SampleEntity, UUID> {

    String getName();

    void setName(String name);

    @MetaProperty
    default String getNameLowercase() {
        return getName().toLowerCase();
    }

}
