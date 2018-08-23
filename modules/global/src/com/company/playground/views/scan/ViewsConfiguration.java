package com.company.playground.views.scan;

import com.company.playground.views.sample.BaseEntityView;
import com.company.playground.views.scan.exception.ViewInitializationException;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */
public class ViewsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ViewsConfiguration.class);

    protected List<String> packages;

    protected ApplicationContext applicationContext;

    protected Map<Class<BaseEntityView>, ViewInterfaceInfo> viewInterfaceDefinitions = new HashMap<>();

    public ViewsConfiguration() {
    }

    public void scan() throws ViewInitializationException {
        log.trace("Scanning views in packages {}", packages);

        ViewCandidateProvider provider = new ViewCandidateProvider();
        provider.setResourceLoader(getResourceLoader());

        for (String scanPackage : packages) {
            Set<BeanDefinition> viewInterfaceDefinitionsCandidates = provider.findCandidateComponents(scanPackage);

            for (BeanDefinition candidate : viewInterfaceDefinitionsCandidates) {
                Class<BaseEntityView> viewInterface = extractViewInterface(candidate);
                Class<Entity> entityClass = extractEntityClass(viewInterface);
                viewInterfaceDefinitions.put(viewInterface, new ViewInterfaceInfo(viewInterface, entityClass));
                log.info("View interface {} detected", candidate.getBeanClassName());
            }
        }

        //create views for the detected view interfaces
        for (Class<BaseEntityView> baseEntityViewClass : viewInterfaceDefinitions.keySet())
            composeView(baseEntityViewClass);
        log.trace("Scanning views successfully completed", packages);
    }

    protected Class<BaseEntityView> extractViewInterface(BeanDefinition beanDefinition) throws ViewInitializationException {
        Class<?> viewInterface;
        try {
            viewInterface = ClassUtils.getClass(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new ViewInitializationException(String.format("Interface was not found for %s", beanDefinition.getBeanClassName()), e);
        }

        //noinspection unchecked - as it is checked in ViewCandidateProvider#isImplementBaseEntityViewInterface
        return (Class<BaseEntityView>) viewInterface;
    }

    protected Class<Entity> extractEntityClass(Class<BaseEntityView> viewInterface) throws ViewInitializationException {
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

    protected View composeView(Class<BaseEntityView> viewInterface) throws ViewInitializationException {

        ViewInterfaceInfo viewInterfaceInfo = viewInterfaceDefinitions.get(viewInterface);

        // view must be in viewInterfaceDefinitions otherwise can lead to infinit recursion
        if (viewInterfaceDefinitions.get(viewInterface) == null) {
            throw new ViewInitializationException(String.format("View interface %s was not initially registered in ViewsConfiguration#scan", viewInterface.getName()));
        }

        if (viewInterfaceInfo.getView() != null)
            return viewInterfaceInfo.getView();

        View result = new View(viewInterfaceInfo.getEntityClass(), viewInterfaceInfo.getViewName());

        Set<Method> baseEntityViewMethods = Arrays.stream(BaseEntityView.class.getMethods()).collect(Collectors.toSet());
        for (Method viewMethod : viewInterface.getMethods()) {
            // skip this utility methods from BaseEntityView
            if (baseEntityViewMethods.contains(viewMethod))
                continue;

            //TODO check methods to have delegatable method in entity (if returns another view interface check that the entity has a reference to another entity)

            //compose view only by getters
            if (viewMethod.getReturnType() == Void.TYPE)
                continue;

            // refers an entity with a certain view
            if (BaseEntityView.class.isAssignableFrom(viewMethod.getReturnType())) {
                //noinspection unchecked
                Class<BaseEntityView> fieldViewInterface = (Class<BaseEntityView>) viewMethod.getReturnType();
                ViewInterfaceInfo refFieldInterfaceInfo = viewInterfaceDefinitions.get(fieldViewInterface);

                if (refFieldInterfaceInfo == null)
                    throw new ViewInitializationException(
                            String.format("View interface %s references %s view interface which was not initially registered in ViewsConfiguration#scan"
                                    , viewInterface.getName()
                                    , fieldViewInterface.getName()));

                //TODO check cyclic references (check corner case when view refers itself as a field - is it a case at all?)

                View refFieldView = composeView(refFieldInterfaceInfo.getViewInterface());
                refFieldInterfaceInfo.setView(refFieldView);

                addProperty(result, methodName2FieldName(viewMethod), refFieldView);
            } else {
                addProperty(result, methodName2FieldName(viewMethod), null);
            }

        }
        viewInterfaceInfo.setView(result);
        return result;
    }

    //TODO support fetch mode and lazy fetching
    protected void addProperty(View targetView, String propName, @Nullable View propView) {
        if (!targetView.containsProperty(propName)) {
            if (propView == null) {
                targetView.addProperty(propName);
            } else {
                targetView.addProperty(propName, propView);
            }
        }
    }

    protected String methodName2FieldName(Method method) throws ViewInitializationException {
        String name = method.getName();

        if (name.startsWith("get") && method.getParameterTypes().length == 0)
            return StringUtils.uncapitalize(name.substring(3));

        if (name.startsWith("is") && method.getParameterTypes().length == 0)
            return StringUtils.uncapitalize(name.substring(2));

        if (name.startsWith("set") && method.getParameterTypes().length == 1)
            return StringUtils.uncapitalize(name.substring(3));

        throw new ViewInitializationException(String.format("Method %s of view interface %s doesn't comply with access fields convention (setter or getter)",
                name, method.getClass().getName()));
    }

//    protected boolean isMethodInClass(Class clazz, Method method) {
//        return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getViewName().equals(method.getViewName()));
//    }

//    public List<String> getPackages() {
//        return packages;
//    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    protected ResourceLoader getResourceLoader() {
        return applicationContext;
    }

    public View getViewByInterface(Class<? extends BaseEntityView> intf) {
        //noinspection SuspiciousMethodCalls
        return viewInterfaceDefinitions.get(intf).getView();
    }

    /**
     * POJO containing all information about view interface
     */
    public class ViewInterfaceInfo {

        protected Class<BaseEntityView> viewInterface;

        protected Class<Entity> entityClass;

        protected View view;

        public ViewInterfaceInfo(@NotNull Class<BaseEntityView> viewInterface, @NotNull Class<Entity> entityClass) {
            this.viewInterface = viewInterface;
            this.entityClass = entityClass;
        }

        public Class<BaseEntityView> getViewInterface() {
            return viewInterface;
        }

        public void setViewInterface(Class<BaseEntityView> viewInterface) {
            this.viewInterface = viewInterface;
        }

        public Class<Entity> getEntityClass() {
            return entityClass;
        }

        public void setEntityClass(Class<Entity> entityClass) {
            this.entityClass = entityClass;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public String getViewName() {
            String name = viewInterface.getAnnotation(EntityView.class).name();
            return name.equals(StringUtils.EMPTY) ? viewInterface.getSimpleName() : name;
        }
    }
}
