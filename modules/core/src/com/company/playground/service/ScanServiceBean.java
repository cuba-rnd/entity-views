package com.company.playground.service;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.CyclicView;
import com.company.playground.views.sample.SampleWithUserView;
import com.company.playground.views.scan.ViewsConfiguration;
import com.company.playground.views.scan.exception.ViewInitializationException;
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
    public void checkProxy() {
        User user = dataManager.load(User.class).list().get(0);

        SampleWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleWithUserView.UserMinimalView.class);
        log.info("SampleWithUserView.UserMinimalView - Login:{}, Name:{}", userMinimal.getLogin(), userMinimal.getName());

        SampleEntity se = dataManager.load(SampleEntity.class)
                .view(conf.getViewByInterface(SampleWithUserView.class))
                .list().get(0);
        SampleWithUserView swu = EntityViewWrapper.wrap(se, SampleWithUserView.class);
        log.info("SampleWithUserView - Name: {}, User.Name: {}", swu.getName(), swu.getUser().getName());
        log.info("SampleWithUserView - Origin: {}, User.Origin: {}", swu.getOrigin(), swu.getUser().getOrigin());

        CyclicView entityWithParent = null;
        try {
            entityWithParent = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                            .view(conf.getViewByInterface(CyclicView.class))
                            .list().get(0)
                    , CyclicView.class);
            log.info("CyclicView - Name: {}, Parent.Name: {}", entityWithParent.getName(), entityWithParent.getParent().getName());
        } catch (ViewInitializationException e) {
            log.error(e.getMessage(), e); //It's OK bro
        }
    }
}