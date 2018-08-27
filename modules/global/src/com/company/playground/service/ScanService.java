package com.company.playground.service;


import com.company.playground.views.sample.SampleMinimalWithUserView;

public interface ScanService {
    String NAME = "playground_ScanService";

    void checkProxy();

    SampleMinimalWithUserView getAnySampleWithUser();

    void saveInterface(SampleMinimalWithUserView sample);
}