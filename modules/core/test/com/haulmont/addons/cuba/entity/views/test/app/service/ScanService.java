package com.haulmont.addons.cuba.entity.views.test.app.service;


import com.haulmont.addons.cuba.entity.views.test.app.views.user.SampleMinimalWithUserView;

public interface ScanService {
    String NAME = "playground_ScanService";

    void checkProxy();

    SampleMinimalWithUserView getAnySampleWithUser();

    void saveInterface(SampleMinimalWithUserView sample);
}