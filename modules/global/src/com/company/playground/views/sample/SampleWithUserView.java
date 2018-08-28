package com.company.playground.views.sample;

import com.company.playground.entity.SampleEntity;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

public interface SampleWithUserView extends BaseEntityView<SampleEntity> {

    UUID getId();

    String getName();

    SampleMinimalView getParent();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User>{

        String getName();

        String getLogin();
    }


}
