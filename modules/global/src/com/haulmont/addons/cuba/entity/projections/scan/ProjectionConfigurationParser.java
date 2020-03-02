package com.haulmont.addons.cuba.entity.projections.scan;

import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.addons.cuba.entity.projections.scan.exception.ProjectionInitException;
import com.haulmont.cuba.core.entity.Entity;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * It is a custom tag config parser and an entity view registrar. It is called by Spring on every {@code <views/>}
 * tag entry found in spring.xml. This class reads package names from the tag, runs a scanner to find entity views and
 * then creates {@link ProjectionsConfiguration} bean with all entity view interface definitions and puts it to Spring context.
 * @see BeanDefinitionParser
 */
public class ProjectionConfigurationParser implements BeanDefinitionParser {

    private static final Logger log = LoggerFactory.getLogger(ProjectionConfigurationParser.class);
    public static final String BASE_PACKAGES = "base-packages";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {

        BeanDefinitionRegistry registry = parserContext.getRegistry();
        String attribute = element.getAttribute(BASE_PACKAGES);
        String[] packages = StringUtils.delimitedListToStringArray(attribute, ",", " ");

        log.trace("Scanning views in packages {}", Arrays.toString(packages));
        try {
            Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> projectionDefinitions = scanForProjections(parserContext, packages);
            if (registry.containsBeanDefinition(ProjectionsConfigurationBean.NAME)){
                log.debug("Adding new views into existing configuration storage: {}", projectionDefinitions);
                Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> initMap =
                        (Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo>) registry
                                .getBeanDefinition(ProjectionsConfigurationBean.NAME)
                                .getConstructorArgumentValues().getArgumentValue(0, Map.class).getValue();

                initMap.putAll(projectionDefinitions);
            } else {
                log.debug("Creating new views configuration storage: {}", projectionDefinitions);
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ProjectionsConfigurationBean.class)
                        .addConstructorArgValue(projectionDefinitions);
                AbstractBeanDefinition viewsConfigurationBean = builder.getBeanDefinition();
                registry.registerBeanDefinition(ProjectionsConfigurationBean.NAME, viewsConfigurationBean);
            }
        } catch (Exception e) {
            throw new BeanInitializationException("Cannot create view interface definitions", e);
        }
        log.trace("Scanning views in packages {} successfully completed", Arrays.toString(packages));
        return null;
    }

    /**
     * Scans packages to find projections and building projection replacement chains.
     * @param parserContext Parser context that contains information about tag and load context.
     * @param packages List of packages to scan.
     * @return Map containing projection classes and DTOs with description for building CUBA views, replacement chains, etc.
     */
    protected Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> scanForProjections(ParserContext parserContext, String[] packages) {
        XmlReaderContext readerContext = parserContext.getReaderContext();
        ProjectionCandidateProvider provider = new ProjectionCandidateProvider(readerContext.getResourceLoader());
        Map<Class<? extends BaseProjection>, ProjectionsConfigurationBean.ProjectionInfo> projectionDefinitions = new HashMap<>();
        for (String scanPackage : packages) {
            log.trace("Scanning package {}", scanPackage);
            Set<BeanDefinition> projectionDefinitionsCandidates = provider.findCandidateComponents(scanPackage);
            for (BeanDefinition candidate : projectionDefinitionsCandidates) {

                Class<? extends BaseProjection> projectionInterface = extractProjectionInterface(candidate);
                Class<Entity> entityClass = extractEntityClass(projectionInterface);

                ReplaceProjection projectionAnnotation = projectionInterface.getAnnotation(ReplaceProjection.class);
                Class<? extends BaseProjection> replacedProjection = projectionAnnotation != null ? projectionAnnotation.value() : null;

                projectionDefinitions.put(projectionInterface, new ProjectionsConfigurationBean.ProjectionInfo(projectionInterface, entityClass, replacedProjection));

                log.info("Projection interface {} detected", candidate.getBeanClassName());
            }

        }
        return projectionDefinitions;
    }

    private Class<? extends BaseProjection> extractProjectionInterface(BeanDefinition beanDefinition) throws ProjectionInitException {
        Class<? extends BaseProjection> projectionInterface;
        try {
            projectionInterface = (Class<? extends BaseProjection>)ClassUtils.getClass(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new ProjectionInitException(String.format("Interface was not found for %s", beanDefinition.getBeanClassName()), e);
        }
        //noinspection unchecked - as it is checked in ProjectionCandidateProvider#isImplementBaseEntityViewInterface
        return projectionInterface;
    }

    private Class<Entity> extractEntityClass(Class<? extends BaseProjection> projectionInterface) throws ProjectionInitException {
        //noinspection unchecked
        List<Class<?>> implementedInterfaces = ClassUtils.getAllInterfaces(projectionInterface);
        implementedInterfaces.add(projectionInterface);

        for (Class intf : implementedInterfaces) {
            Set<ParameterizedType> candidateTypes = Arrays.stream(intf.getGenericInterfaces())
                    .filter(type -> type instanceof ParameterizedType)
                    .map(type -> ((ParameterizedType) type))
                    .filter(parameterizedType -> BaseProjection.class.getTypeName().equals(parameterizedType.getRawType().getTypeName()))
                    .collect(Collectors.toSet());

            if (candidateTypes.size() == 1) {
                ParameterizedType baseEntityViewIntf = candidateTypes.iterator().next();
                Type entityType = Arrays.asList(baseEntityViewIntf.getActualTypeArguments()).get(0);
                try {
                    //noinspection unchecked - as parameter type must implement Entity by declaraion
                    return (Class<Entity>)ClassUtils.getClass(entityType.getTypeName());
                } catch (ClassNotFoundException e) {
                    throw new ProjectionInitException(String.format("Class was not found for %s", entityType.getTypeName()), e);
                }
            }
        }
        throw new ProjectionInitException(String.format("Projection interface %s extends %s interface with no parameter type"
                ,projectionInterface.getName(), BaseProjection.class.getName()));
    }

}
