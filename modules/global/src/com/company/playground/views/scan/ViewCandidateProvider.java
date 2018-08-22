package com.company.playground.views.scan;

import com.company.playground.views.sample.BaseEntityView;
import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
public class ViewCandidateProvider extends ClassPathScanningCandidateComponentProvider {

    private static final Logger log = LoggerFactory.getLogger(ViewCandidateProvider.class);

    public ViewCandidateProvider() {
        super(false);
        addIncludeFilter(new AnnotationTypeFilter(EntityView.class, false, true));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface()
                && beanDefinition.getMetadata().isIndependent()
                && isImplementBaseEntityViewInterface(beanDefinition);
    }

    //View interface must extend BaseEntityView
    protected boolean isImplementBaseEntityViewInterface(BeanDefinition beanDefinition) {
        Class<?> viewInterface;
        try {
            viewInterface = ClassUtils.getClass(beanDefinition.getBeanClassName());

        } catch (ClassNotFoundException e) {
            log.error("Interface was not found for {}", beanDefinition.getBeanClassName());
            return false;
        }

        if (BaseEntityView.class.isAssignableFrom(viewInterface)) {
            return true;
        } else {
            log.error("Interface {} is annotated as @EntityView, but doesn't extend {}"
                    , beanDefinition.getBeanClassName(), BaseEntityView.class.getName());
            return false;
        }
    }
}
