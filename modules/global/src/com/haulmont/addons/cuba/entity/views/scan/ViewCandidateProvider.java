package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Scans classpath and looks for entity view candidates. Candidates should implement {@link BaseEntityView}
 * and should not be annotated with {@link AbstractEntityView}.
 * @see ClassPathScanningCandidateComponentProvider
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
