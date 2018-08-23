package com.company.playground.views.sample;

import com.company.playground.views.scan.EntityViewName;
import com.haulmont.cuba.security.entity.User;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
public interface SampleWithUserView extends SampleMinimalView {
    UserMinimalView getUser();
    void setUser(UserMinimalView val);

    //TODO decide about additional setter?
    void setUser(User val);

    @EntityViewName(UserMinimalView.NAME)
    interface UserMinimalView extends BaseEntityView<User>{
        String NAME = "user-minimal-view";

        String getLogin();
        void setLogin(String val);

        String getName();
        void setName(String val);
    }
}
