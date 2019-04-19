package com.haulmont.addons.cuba.entity.views.test.app.views.user;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleMinimalView;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

public interface SampleWithUserView extends BaseEntityView<SampleEntity, UUID> {

    String getName();

    SampleMinimalView getParent();

    UserMinimalView getUser();

    interface UserMinimalView extends BaseEntityView<User, UUID>{

        String getName();

        String getLogin();
    }
}
