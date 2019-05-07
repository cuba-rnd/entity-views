package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
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

public class ViewsSupportMetadataLoader extends MetadataLoader {

    private final Logger log = LoggerFactory.getLogger(ViewsSupportMetadataLoader.class);

    @Inject
    private ViewsConfigurationBean viewsConfigurationBean;

    @Override
    public void loadMetadata() {
        log.info("Loading Entity Views Metadata");

        super.loadMetadata();

        Map<Class<? extends BaseEntityView>, ViewsConfigurationBean.ViewInterfaceInfo> viewInterfaceDefinitions
                = viewsConfigurationBean.getViewInterfaceDefinitions();


        Set<Class<? extends BaseEntityView>> interfaceClasses = viewInterfaceDefinitions.keySet();

        for (Class<? extends BaseEntityView> interfaceClass : interfaceClasses) {
            ViewsConfigurationBean.ViewInterfaceInfo info = viewInterfaceDefinitions.get(interfaceClass);
            for (MetaModel m : session.getModels()) {
                MetaModelImpl metaModel = (MetaModelImpl) m;

                MetaClassImpl mClass = new MetaClassImpl(metaModel, interfaceClass.getName());
                mClass.setJavaClass(interfaceClass);
                mClass.getAnnotations().put(MetadataTools.STORE_ANN_NAME, Stores.MAIN);

                log.debug("Creating view {} for entity {} in metamodel {}", interfaceClass, info.getEntityClass(), metaModel.getName());
                metaModel.registerClass(mClass);
            }
        }
    }
}
