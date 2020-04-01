package com.haulmont.addons.cuba.entity.projections.test.app.views.sample;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
//Should not be detected as doesn't implement @Projection
public interface SampleView2 {

    SampleMinimalView getSampleEntity();

}
