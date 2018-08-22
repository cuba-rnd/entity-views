package com.company.playground.views.sample;

import com.company.playground.views.scan.EntityView;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
//Should not be detected as doesn't implement @BaseEntityView
@EntityView
public interface SampleView2 {

    SampleMinimalView getSampleEntity();

    //TODO check inner interface definitions

}
