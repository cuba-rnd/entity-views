package com.company.playground.web.screens;

import com.company.playground.service.ScanService;
import com.company.playground.views.sample.SampleWithUserView;
import com.haulmont.cuba.web.app.mainwindow.AppMainWindow;

import javax.inject.Inject;

public class ExtAppMainWindow extends AppMainWindow {

    @Inject
    private ScanService scanService;

    public void onCheckProxyBtnClick() {
        scanService.checkProxy();
        SampleWithUserView sample = scanService.getAnySampleWithUser();
        SampleWithUserView.UserMinimalView sampleUser = sample.getUser();
        showNotification(String.format("Name: %s User: %s User Login: %s", sample.getName(), sampleUser.getName(), sampleUser.getLogin()), NotificationType.HUMANIZED);

        sample.setName("Sample"+System.currentTimeMillis());
        scanService.saveInterface(sample);
        showNotification("Entity saved");

    }
}