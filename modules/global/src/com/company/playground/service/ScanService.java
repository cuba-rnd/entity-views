package com.company.playground.service;


import com.company.playground.views.sample.SampleWithUserView;

public interface ScanService {
    String NAME = "playground_ScanService";

    void checkProxy();

    SampleWithUserView getAnySampleWithUser();

    void saveInterface(SampleWithUserView sample);
}