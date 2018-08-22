package com.company.playground.service;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.SampleWithUserView;
import com.company.playground.views.scan.ViewsConfiguration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(ScanService.NAME)
public class ScanServiceBean implements ScanService {

    @Inject
    private Logger log;

    @Inject
    private ViewsConfiguration conf;

    @Inject
    private DataManager dataManager;


    @Override
    public void runScan() {
        try {

            conf.scan();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void checkProxy() {
        User user = dataManager.load(User.class).list().get(0);

        SampleWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleWithUserView.UserMinimalView.class);
        log.info("{}, {}", userMinimal.getLogin(), userMinimal.getName());

        SampleEntity se = dataManager.load(SampleEntity.class)
                .view(conf.getViewInterfaceDefinitions().get(SampleWithUserView.class).getView())
                .list().get(0);
        SampleWithUserView swu = EntityViewWrapper.wrap(se, SampleWithUserView.class);
        log.info("{}, {}", swu.getName(), swu.getUser().getName());
        log.info("{}, {}", swu.getOrigin(), swu.getUser().getOrigin());
    }
}