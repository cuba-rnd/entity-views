package com.company.playground.web.screens;

import com.company.playground.service.ScanService;
import com.haulmont.cuba.web.app.mainwindow.AppMainWindow;

import javax.inject.Inject;

public class ExtAppMainWindow extends AppMainWindow {

    @Inject
    private ScanService scanService;

    public void runScan() {
        scanService.runScan();
    }
}