package com.company.playground.service;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.sample.SampleWithParentView;
import com.company.playground.views.user.SampleMinimalWithUserView;
import com.haulmont.cuba.core.global.ViewSupportDataManager;
import com.haulmont.cuba.core.views.factory.EntityViewWrapper;
import com.haulmont.cuba.security.entity.User;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(ScanService.NAME)
public class ScanServiceBean implements ScanService {

    @Inject
    private Logger log;

    @Inject
    private ViewSupportDataManager dataManager;

    @Override
    public void checkProxy() {
        User user = dataManager.load(User.class).list().get(0);

        SampleMinimalWithUserView.UserMinimalView userMinimal = EntityViewWrapper.wrap(user, SampleMinimalWithUserView.UserMinimalView.class);
        log.info("SampleMinimalWithUserView.UserMinimalView - Login:{}, Name:{}", userMinimal.getLogin(), userMinimal.getName());

        SampleMinimalWithUserView swu = dataManager.loadWithView(SampleMinimalWithUserView.class).list().get(0);

        log.info("SampleMinimalWithUserView - Name: {}, User.Name: {}", swu.getName(), swu.getUser().getName());
        log.info("SampleMinimalWithUserView - Origin: {}, User.Origin: {}", swu.getOrigin(), swu.getUser().getOrigin());

        SampleWithParentView se2 = dataManager.loadWithView(SampleWithParentView.class)
                .query("select e from playground$SampleEntity e where e.parent is not null")
                .list()
                .get(0);
        log.info("Entity name: {}, entity parent name in lowercase; {}", se2.getName(), se2.getParent().getNameLowercase());
    }


    @Override
    public SampleMinimalWithUserView getAnySampleWithUser(){
        return dataManager.loadWithView(SampleMinimalWithUserView.class).list().get(0);
    }

    @Override
    public void saveInterface(SampleMinimalWithUserView sample){
        SampleEntity entity = sample.getOrigin();
        dataManager.commit(entity);
    }
}