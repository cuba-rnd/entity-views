package com.haulmont.addons.cuba.entity.views.scan;

import com.google.common.collect.ImmutableSet;
import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper;
import com.haulmont.addons.cuba.entity.views.scan.exception.ViewInitializationException;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.sys.events.AppContextStartedEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ApplicationListener;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry that builds and stores entity views and corresponding CUBA views. This class also builds proper substitution
 * for entity views for extended entities and prevents cyclic references between entity views.
 * <br/>
 * The class is in application context despite on fact that it is not marked as a Spring component.
 */
public class ViewsConfiguration implements InitializingBean, ApplicationListener<AppContextStartedEvent> {

    public static final String NAME = "cuba_core_ViewsConfiguration";

    private static final Logger log = LoggerFactory.getLogger(ViewsConfiguration.class);

    private final Map<Class<? extends BaseEntityView>, ViewInterfaceInfo> viewInterfaceDefinitions;

    public ViewsConfiguration(Map<Class<? extends BaseEntityView>, ViewInterfaceInfo> viewInterfaceDefinitions) {
        this.viewInterfaceDefinitions = new ConcurrentHashMap<>(viewInterfaceDefinitions);
    }

    @Override
    public void afterPropertiesSet() {
        buildViewSubstitutionChain();
    }

    @Override
    public void onApplicationEvent(AppContextStartedEvent event) {
        log.debug("Creating CUBA views for EntityViews");
        for (Class<? extends BaseEntityView> interfaceClass : viewInterfaceDefinitions.keySet()) {
            log.debug("Creating view for {}", interfaceClass);
            ViewInterfaceInfo info = viewInterfaceDefinitions.get(interfaceClass);
            info.setView(composeCubaView(interfaceClass, Collections.emptySet()));
        }
    }

    /**
     * Creates entity views substitution chain by going through existing reverse substitution chain that was created
     * during scanning. This is not a final substitution - getEffectiveView() returns actual substitution class.
     */
    private void buildViewSubstitutionChain() {
        //TODO Can be replaced with lambda.
        log.trace("Building view interface substitution chain");
        for (ViewInterfaceInfo replacing : viewInterfaceDefinitions.values()) {
            if (replacing.getReplacedView() != null) {
                ViewInterfaceInfo toBeReplaced = viewInterfaceDefinitions.get(replacing.getReplacedView());
                if (toBeReplaced.getEntityClass().isAssignableFrom(replacing.getEntityClass())) {
                    toBeReplaced.setReplacedBy(replacing.viewInterface);
                    log.trace("Interface {} will be replaced by {}", toBeReplaced.viewInterface, replacing.viewInterface);
                } else {
                    throw new ViewInitializationException(String.format("Error building substitution chain:" +
                            " %s cannot be replaced by %s: " +
                            "entity types are not within inheritance tree.", toBeReplaced, replacing));
                }
            }
        }
        log.trace("Finished view interface substitution build");
    }

    /**
     * Checks if a method is a candidate for a CUBA view property.
     *
     * @param m Candidate method instance.
     * @return True if the method fits.
     */
    private static boolean isMethodCandidate(Method m) {
        return (m.getReturnType() != Void.TYPE) &&
                (m.getDeclaredAnnotation(MetaProperty.class) == null);
    }

    /**
     * Recursively creates CUBA view definition based on entity view interface contract taking into account views
     * substitution.
     *
     * @param viewInterface Entity views interface class.
     * @param visited       Set of processed entity view classes. We need it to prevent cyclic references.
     * @return CUBA view definition.
     * @throws ViewInitializationException Throws exception in case of a cyclic reference or bad parent view reference.
     */
    private View composeCubaView(Class<? extends BaseEntityView> viewInterface, Set<String> visited) throws ViewInitializationException {

        log.trace("Creating view for: {}", viewInterface.getName());

        Class<? extends BaseEntityView> effectiveView = getEffectiveView(viewInterface);

        ViewInterfaceInfo viewInterfaceInfo = viewInterfaceDefinitions.get(effectiveView);
        //Preventing cyclic reference
        if (visited.contains(effectiveView.getName())) {
            throw new ViewInitializationException(String.format("Cyclic dependencies in views. Offending view: %s , Parent views: %s"
                    , effectiveView.getName()
                    , String.join(",", visited)));
        }

        Set<Method> baseEntityViewMethods = Arrays.stream(BaseEntityView.class.getMethods()).collect(Collectors.toSet());
        //compose view only by getters and exclude default interface methods
        Set<Method> viewInterfaceMethods = Arrays.stream(effectiveView.getMethods())
                .filter(ViewsConfiguration::isMethodCandidate)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // skip utility methods from BaseEntityView
        viewInterfaceMethods.removeAll(baseEntityViewMethods);

        View result = new View(viewInterfaceInfo.getEntityClass(), viewInterfaceInfo.getViewName());
        viewInterfaceInfo.setView(result);
        log.trace("View for: {} is created: {}, adding properties", effectiveView.getName(), result.getName());
        viewInterfaceMethods.forEach(viewMethod -> {
            //Check methods to have delegatable method in entity
            // (if returns another view interface check that the entity has a reference to another entity)
            if (MethodUtils.getAccessibleMethod(viewInterfaceInfo.entityClass, viewMethod.getName(), viewMethod.getParameterTypes()) == null) {
                throw new ViewInitializationException(
                        String.format("Method %s is not found in corresponding entity class %s"
                                , viewMethod
                                , viewInterfaceInfo.entityClass));
            }

            Class<?> fieldViewInterface = EntityViewWrapper.getReturnViewType(viewMethod);
            log.trace("Method {} return type {}", viewMethod.getName(), fieldViewInterface);

            log.trace("Checking if a method {} refers an entity with a certain view", viewMethod.getName());
            if (BaseEntityView.class.isAssignableFrom(fieldViewInterface)) {

                ViewInterfaceInfo refFieldInterfaceInfo = viewInterfaceDefinitions.get(fieldViewInterface);

                if (refFieldInterfaceInfo == null)
                    throw new ViewInitializationException(
                            String.format("View interface %s references %s view interface which was not initially registered in ViewsConfiguration#scan"
                                    , effectiveView.getName()
                                    , fieldViewInterface.getName()));

                Set<String> parents = ImmutableSet.<String>builder().addAll(visited).add(effectiveView.getName()).build();
                View refFieldView = composeCubaView(refFieldInterfaceInfo.getViewInterface(), parents);
                refFieldInterfaceInfo.setView(refFieldView);

                addProperty(result, methodName2FieldName(viewMethod), refFieldView);
            } else {
                addProperty(result, methodName2FieldName(viewMethod), null);
            }
        });
        return result;
    }

    /**
     * Returns effective entity view class based on entity view substitution chain.
     *
     * @param viewInterface Initial view interface type that we want to return in our code.
     * @return Effective entity view class.
     */
    public Class<? extends BaseEntityView> getEffectiveView(Class<? extends BaseEntityView> viewInterface) {
        log.trace("Getting effective view for {}", viewInterface);
        ViewInterfaceInfo info = viewInterfaceDefinitions.get(viewInterface);
        while (info.getReplacedBy() != null) {
            info = viewInterfaceDefinitions.get(info.getReplacedBy());
        }
        log.trace("Effective view for {} is {}", viewInterface, info.getViewInterface());
        return info.getViewInterface();
    }

    /**
     * Adds a property to a CUBA view.
     *
     * @param targetView View to be modified.
     * @param propName   Property name.
     * @param propView   View for complex property type.
     */
    //TODO support fetch mode and lazy fetching
    private void addProperty(View targetView, String propName, @Nullable View propView) {
        log.trace("Adding property {} to view {}", propName, targetView.getName());
        if (!targetView.containsProperty(propName)) {
            if (propView == null) {
                targetView.addProperty(propName);
            } else {
                targetView.addProperty(propName, propView);
            }
        }
    }

    private String methodName2FieldName(Method method) throws ViewInitializationException {
        try {
            PropertyDescriptor propertyDescriptor = BeanUtils.findPropertyForMethod(method);
            if (propertyDescriptor == null) {
                throw new BeanDefinitionValidationException(String.format("Method %s is not an accessor method", method.getName()));
            }
            return propertyDescriptor.getName();
        } catch (BeansException e) {
            throw new ViewInitializationException(String.format("Method %s of view interface %s doesn't comply with access fields convention (setter or getter)",
                    method.getName(), method.getClass().getName()), e);
        }
    }

    /**
     * Returns CUBA view definition based on Entity View class.
     *
     * @param viewInterface Entity view class.
     * @return CUBA view.
     */
    public View getViewByInterface(Class<? extends BaseEntityView> viewInterface) {
        ViewInterfaceInfo viewInterfaceInfo = viewInterfaceDefinitions.get(viewInterface);
        if (viewInterfaceInfo == null) {
            throw new ViewInitializationException(String.format("View %s is not registered", viewInterface));
        }
        return viewInterfaceInfo.getView();
    }

    /**
     * POJO containing all information about view interface.
     */
    public static class ViewInterfaceInfo {

        protected final Class<? extends BaseEntityView> viewInterface;

        protected final Class<Entity> entityClass;

        protected View view; //We create views in lazy manner after context is initialized

        protected final Class<? extends BaseEntityView> replacedView;

        protected Class<? extends BaseEntityView> replacedBy;

        public ViewInterfaceInfo(@NotNull Class<? extends BaseEntityView> viewInterface, @NotNull Class<Entity> entityClass, Class<? extends BaseEntityView> replacedView) {
            this.viewInterface = viewInterface;
            this.entityClass = entityClass;
            this.replacedView = replacedView;
        }

        public Class<? extends BaseEntityView> getViewInterface() {
            return viewInterface;
        }

        public Class<Entity> getEntityClass() {
            return entityClass;
        }

        protected Class<? extends BaseEntityView> getReplacedView() {
            return replacedView;
        }

        protected Class<? extends BaseEntityView> getReplacedBy() {
            return replacedBy;
        }

        protected void setReplacedBy(Class<? extends BaseEntityView> replacedBy) {
            this.replacedBy = replacedBy;
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

        @Override
        public String toString() {
            return "ViewInterfaceInfo{" +
                    "viewInterface=" + viewInterface.getName() +
                    ", entityClass=" + entityClass.getName() +
                    '}';
        }
    }
}
