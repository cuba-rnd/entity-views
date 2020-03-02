package com.haulmont.addons.cuba.entity.projections.scan;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers parser for the custom tag.
 */
public class ProjectionsNamespaceHandler extends NamespaceHandlerSupport {

    public static final String PROJECTIONS = "projections";

    @Override
    public void init() {
        registerBeanDefinitionParser(PROJECTIONS, new ProjectionConfigurationParser());
    }
}
