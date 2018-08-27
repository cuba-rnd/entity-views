package com.company.playground.service;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.factory.EntityViewWrapper;
import com.company.playground.views.sample.CyclicView;
import com.company.playground.views.sample.SampleMinimalWithUserView;
import com.company.playground.views.sample.SampleWithParentView;
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

        SampleMinimalWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class);
        log.info("SampleMinimalWithUserView.UserMinimalView - Login:{}, Name:{}", userMinimal.getLogin(), userMinimal.getName());

        SampleEntity se = dataManager.load(SampleEntity.class)
                .view(conf.getViewByInterface(SampleMinimalWithUserView.class))
                .list().get(0);
        SampleMinimalWithUserView swu = EntityViewWrapper.wrap(se, SampleMinimalWithUserView.class);
        log.info("SampleMinimalWithUserView - Name: {}, User.Name: {}", swu.getName(), swu.getUser().getName());
        log.info("SampleMinimalWithUserView - Origin: {}, User.Origin: {}", swu.getOrigin(), swu.getUser().getOrigin());

        CyclicView entityWithParent = null;
        try {
            entityWithParent = EntityViewWrapper.wrap(dataManager.load(SampleEntity.class)
                            .view(conf.getViewByInterface(CyclicView.class))
                            .list().get(0)
                    , CyclicView.class);
            log.info("CyclicView - Name: {}, Parent.Name: {}", entityWithParent.getName(), entityWithParent.getParent().getName());
        } catch (ViewInitializationException e) {
            log.error(e.getMessage()); //It's OK bro
        }

        SampleEntity se2 = dataManager.load(SampleEntity.class)
                .view(conf.getViewByInterface(SampleWithParentView.class))
                .list()
                .stream()
                .filter(e -> e.getParent() != null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find proper test data"));

        SampleWithParentView view = EntityViewWrapper.wrap(se2, SampleWithParentView.class);
        log.info("Entity name: {}, entity parent name in lowercase; {}", view.getName(), view.getParent().getNameLowercase());
    }
}