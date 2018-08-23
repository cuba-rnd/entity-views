package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

//TODO support @Extends
public interface SampleMinimalView extends BaseEntityView<SampleEntity> {

    String getName();
    void setName(String name);

// TODO support default interface methods
//    @MetaProperty
//    default Double getRandomNumber() {
//        return new Random().nextDouble();
//    }

}
