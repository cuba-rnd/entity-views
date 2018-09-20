package com.company.playground.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@NamePattern("%s|name")
@Table(name = "PLAYGROUND_ENTITY_PARAMETER")
@Entity(name = "playground$EntityParameter")
public class EntityParameter extends StandardEntity {
    private static final long serialVersionUID = -657342717634491546L;

    @Column(name = "NAME")
    protected String name;

    @Column(name = "PARAM_VALUE")
    protected Long paramValue;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAMPLE_ENTITY_ID")
    protected SampleEntity sampleEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMP_ENTITY_ID")
    protected SampleEntity compEntity;

    public void setCompEntity(SampleEntity compEntity) {
        this.compEntity = compEntity;
    }

    public SampleEntity getCompEntity() {
        return compEntity;
    }


    public void setSampleEntity(SampleEntity sampleEntity) {
        this.sampleEntity = sampleEntity;
    }

    public SampleEntity getSampleEntity() {
        return sampleEntity;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setParamValue(Long paramValue) {
        this.paramValue = paramValue;
    }

    public Long getParamValue() {
        return paramValue;
    }


}