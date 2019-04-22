package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.cuba.core.global.View;

public interface ViewsConfiguration {

    String NAME = "entity_views_core_ViewsConfiguration";

    Class<? extends BaseEntityView> getEffectiveView(Class<? extends BaseEntityView> viewInterface);

    View getViewByInterface(Class<? extends BaseEntityView> viewInterface);
}
