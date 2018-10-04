package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.chile.core.annotations.MetaProperty;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    @MetaProperty
    default String getNameLowercase() {
        return getName().toLowerCase();
    }

}
