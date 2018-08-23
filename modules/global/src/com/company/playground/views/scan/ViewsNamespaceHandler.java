package com.company.playground.views.scan;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ViewsNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("views", new ViewsConfigurationParser());
    }
}
