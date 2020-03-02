package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Scans classpath and looks for projection candidates. Candidates should implement {@link BaseProjection}
 * and should not be annotated with {@link AbstractProjection}.
 * @see ClassPathScanningCandidateComponentProvider
 */
public class ProjectionCandidateProvider extends ClassPathScanningCandidateComponentProvider {

    private static final Logger log = LoggerFactory.getLogger(ProjectionCandidateProvider.class);

    public ProjectionCandidateProvider(ResourceLoader resourceLoader) {
        super(false);
        addIncludeFilter(new AssignableTypeFilter(BaseProjection.class));
        addExcludeFilter(new AnnotationTypeFilter(AbstractProjection.class));
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
