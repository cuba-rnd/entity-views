package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.cuba.core.global.View;

import java.util.Map;

public interface ProjectionsConfiguration {

    String NAME = "entity_projections_core_ProjectionsConfiguration";

    Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> getProjectionDefinitions();

    ProjectionsConfigurationBean.ProjectionInfo getProjectionInfo(Class<? extends BaseProjection> interfaceClass);

    Class<? extends BaseProjection> getEffectiveProjection(Class<? extends BaseProjection> projectionInterface);

    View getViewByProjection(Class<? extends BaseProjection> projectionInterface);

    ProjectionsConfigurationBean.ProjectionInfo getViewInfoByView(View view);
}
