package com.haulmont.cuba.core.views.scan;

import com.haulmont.cuba.core.views.BaseEntityView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
public class ViewCandidateProvider extends ClassPathScanningCandidateComponentProvider {

    private static final Logger log = LoggerFactory.getLogger(ViewCandidateProvider.class);

    public ViewCandidateProvider(ResourceLoader resourceLoader) {
        super(false);
        addIncludeFilter(new AssignableTypeFilter(BaseEntityView.class));
        addExcludeFilter(new AnnotationTypeFilter(AbstractEntityView.class));
        setResourceLoader(resourceLoader);
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        log.trace("Trying: "+beanDefinition.getBeanClassName());
        boolean isCandidate = beanDefinition.getMetadata().isInterface()
                && beanDefinition.getMetadata().isIndependent();
        log.trace(beanDefinition.getBeanClassName()+" Candidate: "+isCandidate);
        return isCandidate;
    }
}
