package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;
import com.haulmont.chile.core.annotations.MetaProperty;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

//TODO support @Extends
public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();

    void setName(String name);

    @MetaProperty
    default String getNameLowcase() {
        return getName().toLowerCase();
    }

}
