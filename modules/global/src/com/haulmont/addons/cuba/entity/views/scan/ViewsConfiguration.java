package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.global.View;

import java.util.Map;

public interface ViewsConfiguration {

    String NAME = "entity_views_core_ViewsConfiguration";

    Map<Class<? extends BaseEntityView>, ViewsConfigurationBean.ViewInterfaceInfo> getViewInterfaceDefinitions();

    ViewsConfigurationBean.ViewInterfaceInfo getViewInterfaceDefinition(Class<? extends BaseEntityView> interfaceClass);

    Class<? extends BaseEntityView> getEffectiveView(Class<? extends BaseEntityView> viewInterface);

    View getViewByInterface(Class<? extends BaseEntityView> viewInterface);
}
