package com.haulmont.addons.cuba.entity.views.scan;

import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.scan.exception.ViewInitializationException;
import com.haulmont.cuba.core.entity.Entity;
import org.apache.commons.lang.ClassUtils;
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
 * It is a custom tag config parser and an entity view registrar. It is called by Spring on every <code><views/><code/>
 * tag entry found in spring.xml. This class reads package names from the tag, runs a scanner to find entity views and
 * then creates {@link ViewsConfiguration} bean with all entity view interface definitions and puts it to Spring context.
 * @see BeanDefinitionParser
 */
public class ViewsConfigurationParser implements BeanDefinitionParser {

    private static final Logger log = LoggerFactory.getLogger(ViewsConfigurationParser.class);
    public static final String BASE_PACKAGES = "base-packages";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {

        BeanDefinitionRegistry registry = parserContext.getRegistry();
        String attribute = element.getAttribute(BASE_PACKAGES);
        String[] packages = StringUtils.delimitedListToStringArray(attribute, ",", " ");

        log.trace("Scanning views in packages {}", Arrays.toString(packages));
        try {
            Map<Class<? extends BaseEntityView>, ViewsConfiguration.ViewInterfaceInfo> viewInterfaceDefinitions = scanForViewInterfaces(parserContext, packages);
            if (registry.containsBeanDefinition(ViewsConfiguration.NAME)){
                log.debug("Adding new views into existing configuration storage: {}", viewInterfaceDefinitions);
                Map<Class<? extends BaseEntityView>, ViewsConfiguration.ViewInterfaceInfo> initmap =
                        (Map<Class<? extends BaseEntityView>, ViewsConfiguration.ViewInterfaceInfo>) registry
                                .getBeanDefinition(ViewsConfiguration.NAME)
                                .getConstructorArgumentValues().getArgumentValue(0, Map.class).getValue();

                initmap.putAll(viewInterfaceDefinitions);
            } else {
                log.debug("Creating new views configuration storage: {}", viewInterfaceDefinitions);
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ViewsConfiguration.class)
                        .addConstructorArgValue(viewInterfaceDefinitions);
                AbstractBeanDefinition viewsConfigurationBean = builder.getBeanDefinition();
                registry.registerBeanDefinition(ViewsConfiguration.NAME, viewsConfigurationBean);
            }
        } catch (Exception e) {
            throw new BeanInitializationException("Cannot create view interface definitions", e);
        }
        log.trace("Scanning views in packages {} successfully completed", Arrays.toString(packages));
        return null;
    }

    /**
     * Scans packages to find entity views building views replacement chains.
     * @param parserContext Parser context that contains information about tag and load context.
     * @param packages List of packages to scan.
     * @return Map containing entity view classes and DTOs with description for building CUBA views, replacement chains, etc.
     */
    protected Map<Class<? extends BaseEntityView>, ViewsConfiguration.ViewInterfaceInfo> scanForViewInterfaces(ParserContext parserContext, String[] packages) {
        XmlReaderContext readerContext = parserContext.getReaderContext();
        ViewCandidateProvider provider = new ViewCandidateProvider(readerContext.getResourceLoader());
        Map<Class<? extends BaseEntityView>, ViewsConfiguration.ViewInterfaceInfo> viewInterfaceDefinitions = new HashMap<>();
        for (String scanPackage : packages) {
            log.trace("Scanning package {}", scanPackage);
            Set<BeanDefinition> viewInterfaceDefinitionsCandidates = provider.findCandidateComponents(scanPackage);
            for (BeanDefinition candidate : viewInterfaceDefinitionsCandidates) {

                Class<? extends BaseEntityView> viewInterface = extractViewInterface(candidate);
                Class<Entity> entityClass = extractEntityClass(viewInterface);

                ReplaceEntityView replaceViewAnnotation = viewInterface.getAnnotation(ReplaceEntityView.class);
                Class<? extends BaseEntityView> replacedView = replaceViewAnnotation != null ? replaceViewAnnotation.value() : null;

                viewInterfaceDefinitions.put(viewInterface, new ViewsConfiguration.ViewInterfaceInfo(viewInterface, entityClass, replacedView));

                log.info("View interface {} detected", candidate.getBeanClassName());
            }

        }
        return viewInterfaceDefinitions;
    }

    private Class<? extends BaseEntityView> extractViewInterface(BeanDefinition beanDefinition) throws ViewInitializationException {
        Class<? extends BaseEntityView> viewInterface;
        try {
            viewInterface = ClassUtils.getClass(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new ViewInitializationException(String.format("Interface was not found for %s", beanDefinition.getBeanClassName()), e);
        }
        //noinspection unchecked - as it is checked in ViewCandidateProvider#isImplementBaseEntityViewInterface
        return viewInterface;
    }

    private Class<Entity> extractEntityClass(Class<? extends BaseEntityView> viewInterface) throws ViewInitializationException {
        //noinspection unchecked
        List<Class> implementedInterfaces = ClassUtils.getAllInterfaces(viewInterface);
        implementedInterfaces.add(viewInterface);

        for (Class intf : implementedInterfaces) {
            Set<ParameterizedType> candidateTypes = Arrays.stream(intf.getGenericInterfaces())
                    .filter(type -> type instanceof ParameterizedType)
                    .map(type -> ((ParameterizedType) type))
                    .filter(parameterizedType -> BaseEntityView.class.getTypeName().equals(parameterizedType.getRawType().getTypeName()))
                    .collect(Collectors.toSet());

            if (candidateTypes.size() == 1) {
                ParameterizedType baseEntityViewIntf = candidateTypes.iterator().next();
                Type entityType = Arrays.asList(baseEntityViewIntf.getActualTypeArguments()).get(0);
                try {
                    //noinspection unchecked - as parameter type must implement Entity by declaraion
                    return ClassUtils.getClass(entityType.getTypeName());
                } catch (ClassNotFoundException e) {
                    throw new ViewInitializationException(String.format("Class was not found for %s", entityType.getTypeName()), e);
                }
            }
        }
        throw new ViewInitializationException(String.format("View interface %s extends %s interface with no parameter type"
                ,viewInterface.getName(), BaseEntityView.class.getName()));
    }

}
