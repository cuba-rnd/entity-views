package com.haulmont.addons.cuba.entity.views.test.app.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.List;

@NamePattern("%s|name")
@Table(name = "PLAYGROUND_SAMPLE_ENTITY")
@Entity(name = "playground$SampleEntity")
public class SampleEntity extends StandardEntity {
    private static final long serialVersionUID = 6323743611817286101L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    protected SampleEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected User user;

    @OneToMany(mappedBy = "sampleEntity")
    protected List<com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter> params;

    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "compEntity")
    protected List<com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter> compParams;

    public void setCompParams(List<com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter> compParams) {
        this.compParams = compParams;
    }

    public List<com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter> getCompParams() {
        return compParams;
    }


    public void setParams(List<com.haulmont.addons.cuba.entity.views.test.app.entity.EntityParameter> params) {
        this.params = params;
    }

    public List<EntityParameter> getParams() {
        return params;
    }


    public void setParent(SampleEntity parent) {
        this.parent = parent;
    }

    public SampleEntity getParent() {
        return parent;
    }


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