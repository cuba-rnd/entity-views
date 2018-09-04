package com.haulmont.cuba.core.views.scan;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ViewsNamespaceHandler extends NamespaceHandlerSupport {

    public static final String VIEWS = "views";

    @Override
    public void init() {
        registerBeanDefinitionParser(VIEWS, new ViewsConfigurationParser());
    }
}
