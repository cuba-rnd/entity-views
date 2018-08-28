package com.company.playground.views.scan;

import com.company.playground.views.sample.BaseEntityView;
import com.company.playground.views.scan.exception.ViewInitializationException;
import com.google.common.collect.ImmutableSet;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Aleksey Stukalov on 16/08/2018.
 */

public class ViewsConfiguration {

    public static final String NAME = "cuba_core_ViewsConfiguration";

    private static final Logger log = LoggerFactory.getLogger(ViewsConfiguration.class);

    private final Map<Class<? extends BaseEntityView>, ViewInterfaceInfo> viewInterfaceDefinitions;

    private final Map<ViewInterfaceInfo, View> lazyViewMap;

    public ViewsConfiguration(Map<Class<? extends BaseEntityView>, ViewInterfaceInfo> viewInterfaceDefinitions) {
        this.viewInterfaceDefinitions = new ConcurrentHashMap<>(viewInterfaceDefinitions);
        this.lazyViewMap = new ConcurrentHashMap<>(viewInterfaceDefinitions.keySet().size());
    }

    private static boolean isMethodCandidate(Method m) {
        return (m.getReturnType() != Void.TYPE) &&
                (m.getDeclaredAnnotation(MetaProperty.class) == null);
    }

    private ViewInterfaceInfo composeView(Class<? extends BaseEntityView> viewInterface, Set<String> visited) throws ViewInitializationException {

        log.trace("Creating view for: {}", viewInterface.getName());

        ViewInterfaceInfo viewInterfaceInfo = viewInterfaceDefinitions.get(viewInterface);
        //Preventing cyclic reference
        if (visited.contains(viewInterface.getName())) {
            throw new ViewInitializationException(String.format("Cyclic dependencies in views. Offending view: %s , Parent views: %s"
                    , viewInterface.getName()
                    , String.join(",", visited)));
        }

        Set<Method> baseEntityViewMethods = Arrays.stream(BaseEntityView.class.getMethods()).collect(Collectors.toSet());
        //compose view only by getters
        //TODO check methods to have delegatable method in entity (if returns another view interface check that the entity has a reference to another entity)
        Set<Method> viewInterfaceMethods = Arrays.stream(viewInterface.getMethods())
                .filter(ViewsConfiguration::isMethodCandidate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // skip utility methods from BaseEntityView
        viewInterfaceMethods.removeAll(baseEntityViewMethods);


        View result = new View(viewInterfaceInfo.getEntityClass(), viewInterfaceInfo.getViewName());
        viewInterfaceInfo.setView(result);

        log.trace("View for: {} is created: {}", viewInterface.getName(), result);

        viewInterfaceMethods.forEach(viewMethod -> {
            log.trace("Checking is a method {} refers an entity with a certain view", viewMethod);
            if (BaseEntityView.class.isAssignableFrom(viewMethod.getReturnType())) {
                //noinspection unchecked
                Class<BaseEntityView> fieldViewInterface = (Class<BaseEntityView>) viewMethod.getReturnType();
                ViewInterfaceInfo refFieldInterfaceInfo = viewInterfaceDefinitions.get(fieldViewInterface);

                if (refFieldInterfaceInfo == null)
                    throw new ViewInitializationException(
                            String.format("View interface %s references %s view interface which was not initially registered in ViewsConfiguration#scan"
                                    , viewInterface.getName()
                                    , fieldViewInterface.getName()));

                Set<String> parents = ImmutableSet.<String>builder().addAll(visited).add(viewInterface.getName()).build();
                View refFieldView = composeView(refFieldInterfaceInfo.getViewInterface(), parents).getView();
                refFieldInterfaceInfo.setView(refFieldView);

                addProperty(result, methodName2FieldName(viewMethod), refFieldView);
            } else {
                addProperty(result, methodName2FieldName(viewMethod), null);
            }
        });
        return viewInterfaceInfo;
    }

    //TODO support fetch mode and lazy fetching
    private void addProperty(View targetView, String propName, @Nullable View propView) {
        if (!targetView.containsProperty(propName)) {
            if (propView == null) {
                targetView.addProperty(propName);
            } else {
                targetView.addProperty(propName, propView);
            }
        }
    }

    private String methodName2FieldName(Method method) throws ViewInitializationException {
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

    //TODO I suspect we need to put views to CUBA's ViewRepository
    public View getViewByInterface(Class<? extends BaseEntityView> viewInterface) {
        ViewInterfaceInfo key = viewInterfaceDefinitions.get(viewInterface);
        if (key == null) {
            throw new ViewInitializationException(String.format("View interface %s is not registered in ViewsConfiguration", viewInterface.getName()));
        }
        return lazyViewMap.computeIfAbsent(key, (intface) -> composeView(viewInterface, Collections.emptySet()).getView());
    }

    /**
     * POJO containing all information about view interface
     */
    public static class ViewInterfaceInfo {

        protected final Class<? extends BaseEntityView> viewInterface;

        protected final Class<Entity> entityClass;

        protected View view; //We create views in lazy manner

        public ViewInterfaceInfo(@NotNull Class<? extends BaseEntityView> viewInterface, @NotNull Class<Entity> entityClass) {
            this.viewInterface = viewInterface;
            this.entityClass = entityClass;
        }

        public Class<? extends BaseEntityView> getViewInterface() {
            return viewInterface;
        }

        public Class<Entity> getEntityClass() {
            return entityClass;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public String getViewName() {
            EntityViewName annotation = viewInterface.getAnnotation(EntityViewName.class);
            if ((annotation == null) || (StringUtils.isEmpty(annotation.value()))) {
                return viewInterface.getSimpleName();
            }
            return annotation.value();
        }
    }
}
