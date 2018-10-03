package com.company.playground.views.user;

import com.company.playground.entity.SampleEntity;
import com.company.playground.views.sample.SampleMinimalView;
import com.haulmont.cuba.core.views.BaseEntityView;
import com.haulmont.cuba.security.entity.User;

public interface SampleWithUserView extends BaseEntityView<SampleEntity> {

    String getName();

    SampleMinimalView getParent();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User>{

        String getName();

        String getLogin();
    }


}
