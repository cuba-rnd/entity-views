package com.haulmont.addons.cuba.entity.views.test.app.entity;

import com.haulmont.cuba.core.entity.annotation.Extends;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;

@Extends(User.class)
@Entity(name = "playground$ExtendedUser")
public class ExtendedUser extends User {
    private static final long serialVersionUID = 2718704033278012702L;

    @Column(name = "LONG_NAME")
    protected String longName;

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getLongName() {
        return longName;
    }


}