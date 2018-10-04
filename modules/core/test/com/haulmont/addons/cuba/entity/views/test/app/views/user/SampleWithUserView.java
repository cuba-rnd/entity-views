package com.haulmont.addons.cuba.entity.views.test.app.views.user;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.views.test.app.views.sample.SampleMinimalView;
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
