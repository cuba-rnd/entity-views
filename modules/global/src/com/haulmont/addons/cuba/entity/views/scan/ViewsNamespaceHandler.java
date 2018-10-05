package com.haulmont.addons.cuba.entity.views.scan;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers parser for the custom tag.
 */
public class ViewsNamespaceHandler extends NamespaceHandlerSupport {

    public static final String VIEWS = "views";

    @Override
    public void init() {
        registerBeanDefinitionParser(VIEWS, new ViewsConfigurationParser());
    }
}
