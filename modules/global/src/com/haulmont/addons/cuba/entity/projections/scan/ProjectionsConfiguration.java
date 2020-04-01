package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.cuba.core.global.View;

import java.util.Map;

public interface ProjectionsConfiguration {

    String NAME = "entity_projections_core_ProjectionsConfiguration";

    Map<Class<? extends Projection>, ProjectionsConfigurationBean.ProjectionInfo> getProjectionDefinitions();

    ProjectionsConfigurationBean.ProjectionInfo getProjectionInfo(Class<? extends Projection> interfaceClass);

    Class<? extends Projection> getEffectiveProjection(Class<? extends Projection> projectionInterface);

    View getViewByProjection(Class<? extends Projection> projectionInterface);

    ProjectionsConfigurationBean.ProjectionInfo getViewInfoByView(View view);
}
