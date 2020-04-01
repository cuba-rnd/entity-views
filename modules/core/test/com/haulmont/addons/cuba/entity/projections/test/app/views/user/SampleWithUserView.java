package com.haulmont.addons.cuba.entity.projections.test.app.views.user;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.test.app.entity.SampleEntity;
import com.haulmont.addons.cuba.entity.projections.test.app.views.sample.SampleMinimalView;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

public interface SampleWithUserView extends Projection<SampleEntity, UUID> {

    String getName();

    SampleMinimalView getParent();

    UserMinimalView getUser();

    interface UserMinimalView extends Projection<User, UUID> {

        String getName();

        String getLogin();
    }
}
