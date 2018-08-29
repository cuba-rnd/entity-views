package com.company.playground.web.screens;

import com.company.playground.entity.SampleEntity;
import com.company.playground.service.ScanService;
import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.SampleMinimalView;
import com.company.playground.views.sample.SampleMinimalWithUserView;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.web.app.mainwindow.AppMainWindow;

import javax.inject.Inject;

public class ExtAppMainWindow extends AppMainWindow {

    @Inject
    private ScanService scanService;

    @Inject
    private DataManager dataManager;

    public void onCheckProxyBtnClick() {
        SampleEntity sampleEntity = dataManager.load(SampleEntity.class).one();
        SampleMinimalView userMinimal = EntityViewWrapper.wrap(sampleEntity, SampleMinimalView.class);
        showNotification(SampleMinimalWithUserView.class+" == "+ userMinimal.getInterfaceClass());
    }
}