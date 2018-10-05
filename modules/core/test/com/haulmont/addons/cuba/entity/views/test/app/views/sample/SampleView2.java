package com.haulmont.addons.cuba.entity.views.test.app.views.sample;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
//Should not be detected as doesn't implement @BaseEntityView
public interface SampleView2 {

    SampleMinimalView getSampleEntity();

}
