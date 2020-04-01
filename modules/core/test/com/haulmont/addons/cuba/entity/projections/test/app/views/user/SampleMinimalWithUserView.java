package com.haulmont.addons.cuba.entity.projections.test.app.views.user;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.scan.ProjectionName;
import com.haulmont.addons.cuba.entity.projections.scan.ReplaceProjection;
import com.haulmont.addons.cuba.entity.projections.test.app.views.sample.SampleMinimalViewReplace;
import com.haulmont.cuba.security.entity.User;

import java.util.UUID;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
@ReplaceProjection(SampleMinimalViewReplace.class)
public interface SampleMinimalWithUserView extends SampleMinimalViewReplace {

    UserMinimalView getUser();

    void setUser(UserMinimalView val);

    @ProjectionName(UserMinimalView.NAME)
    interface UserMinimalView extends Projection<User, UUID> {
        String NAME = "user-minimal-view";

        String getLogin();
        void setLogin(String val);

        String getName();
        void setName(String val);
    }

    interface UserMinimalView2 extends UserMinimalView {

    }
}
