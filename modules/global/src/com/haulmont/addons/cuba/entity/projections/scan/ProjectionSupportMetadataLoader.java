package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.chile.core.model.MetaModel;
import com.haulmont.chile.core.model.impl.MetaClassImpl;
import com.haulmont.chile.core.model.impl.MetaModelImpl;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Stores;
import com.haulmont.cuba.core.sys.MetadataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class ProjectionSupportMetadataLoader extends MetadataLoader {

    private final Logger log = LoggerFactory.getLogger(ProjectionSupportMetadataLoader.class);

    @Inject
    private ProjectionsConfigurationBean projectionsConfigurationBean;

    @Override
    public void loadMetadata() {
        log.info("Loading Projections Metadata");

        super.loadMetadata();

        Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> projectionDefinitions
                = projectionsConfigurationBean.getProjectionDefinitions();


        Set<Class<? extends BaseProjection>> interfaceClasses = projectionDefinitions.keySet();

        for (Class<? extends BaseProjection> interfaceClass : interfaceClasses) {
            ProjectionsConfigurationBean.ProjectionInfo info = projectionDefinitions.get(interfaceClass);
            for (MetaModel m : session.getModels()) {
                MetaModelImpl metaModel = (MetaModelImpl) m;

                MetaClassImpl mClass = new MetaClassImpl(metaModel, interfaceClass.getName());
                mClass.setJavaClass(interfaceClass);
                mClass.getAnnotations().put(MetadataTools.STORE_ANN_NAME, Stores.MAIN);

                log.debug("Creating projection {} for entity {} in metamodel {}", interfaceClass, info.getEntityClass(), metaModel.getName());
                metaModel.registerClass(mClass);
            }
        }
    }
}
