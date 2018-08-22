package com.company.playground.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.validation.constraints.NotNull;
import com.haulmont.cuba.security.entity.User;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Table(name = "PLAYGROUND_SAMPLE_ENTITY")
@Entity(name = "playground$SampleEntity")
public class SampleEntity extends StandardEntity {
    private static final long serialVersionUID = 6323743611817286101L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected User user;

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}