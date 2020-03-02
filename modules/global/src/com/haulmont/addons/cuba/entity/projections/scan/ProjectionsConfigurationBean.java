package com.haulmont.addons.cuba.entity.projections.scan;

import com.google.common.collect.ImmutableSet;
import com.haulmont.addons.cuba.entity.projections.BaseProjection;
import com.haulmont.addons.cuba.entity.projections.factory.EntityProjectionWrapper;
import com.haulmont.addons.cuba.entity.projections.scan.exception.ProjectionInitException;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.sys.events.AppContextInitializedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

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
 * for entity views for extended entities and prevents cyclic references between entity views. <br>
 * The class is in application context despite on fact that it is not marked as a Spring component.
 */

public class ProjectionsConfigurationBean implements ProjectionsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProjectionsConfigurationBean.class);

    private final Map<Class<? extends BaseProjection>, ProjectionInfo> projectionDefinitions = new ConcurrentHashMap<>();

    private final Map<View, ProjectionInfo> definitionsByView = new ConcurrentHashMap<>();

    public ProjectionsConfigurationBean(Map<Class<? extends BaseProjection>, ProjectionInfo> projectionDefinitions) {
        this.projectionDefinitions.putAll(projectionDefinitions);
    }

    @EventListener(AppContextInitializedEvent.class)
    @Order
    public void onAppContextInitializedEvent()  {
        buildViewSubstitutionChain();
        log.debug("Creating CUBA views for projections");
        for (Class<? extends BaseProjection> interfaceClass : projectionDefinitions.keySet()) {
            log.debug("Creating view for {}", interfaceClass);
            ProjectionInfo info = projectionDefinitions.get(interfaceClass);
            View cubaView = composeCubaView(interfaceClass, Collections.emptySet());
            info.setView(cubaView);
            definitionsByView.put(cubaView, info);
        }
    }

    @Override
    public Map<Class<? extends BaseProjection>, ProjectionInfo> getProjectionDefinitions() {
        return Collections.unmodifiableMap(projectionDefinitions);
    }

    @Override
    public ProjectionInfo getProjectionInfo(Class<? extends BaseProjection> interfaceClass) {
        return projectionDefinitions.get(interfaceClass);
    }

    /**
     * Returns effective entity view class based on entity view substitution chain.
     *
     * @param projectionInterface Initial view interface type that we want to return in our code.
     * @return Effective entity view class.
     */
    @Override
    public Class<? extends BaseProjection> getEffectiveProjection(Class<? extends BaseProjection> projectionInterface) {
        log.trace("Getting effective projection for {}", projectionInterface);
        ProjectionInfo info = projectionDefinitions.get(projectionInterface);
        if (info == null) {
            throw new ProjectionInitException(
                    String.format("Projection %s was not initially registered in ProjectionsConfigurationBean#scan",
                            projectionInterface)
            );
        }
        while (info.getReplacedBy() != null) {
            info = projectionDefinitions.get(info.getReplacedBy());
        }
        log.trace("Effective projection for {} is {}", projectionInterface, info.getProjectionInterface());
        return info.getProjectionInterface();
    }


    /**
     * Returns CUBA view definition based on Entity View class.
     *
     * @param projectionInterface Entity view class.
     * @return CUBA view.
     */
    @Override
    public View getViewByProjection(Class<? extends BaseProjection> projectionInterface) {
        ProjectionInfo projectionInfo = projectionDefinitions.get(projectionInterface);
        if (projectionInfo == null) {
            throw new ProjectionInitException(String.format("View %s is not registered", projectionInterface));
        }
        return projectionInfo.getView();
    }

    @Override
    public ProjectionInfo getViewInfoByView(View view) {
        return definitionsByView.get(view);
    }

    /**
     * Creates entity views substitution chain by going through existing reverse substitution chain that was created
     * during scanning. This is not a final substitution - getEffectiveView() returns actual substitution class.
     */
    private void buildViewSubstitutionChain() {
        //TODO Can be replaced with lambda.
        log.trace("Building view interface substitution chain");
        for (ProjectionInfo replacing : projectionDefinitions.values()) {
            if (replacing.getReplacedProjection() != null) {
                ProjectionInfo toBeReplaced = projectionDefinitions.get(replacing.getReplacedProjection());
                if (toBeReplaced.getEntityClass().isAssignableFrom(replacing.getEntityClass())) {
                    toBeReplaced.setReplacedBy(replacing.projectionInterface);
                    log.trace("Interface {} will be replaced by {}", toBeReplaced.projectionInterface, replacing.projectionInterface);
                } else {
                    throw new ProjectionInitException(String.format("Error building substitution chain:" +
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
     * @param projectionInterface Entity views interface class.
     * @param visited       Set of processed entity view classes. We need it to prevent cyclic references.
     * @return CUBA view definition.
     * @throws ProjectionInitException Throws exception in case of a cyclic reference or bad parent view reference.
     */
    private View composeCubaView(Class<? extends BaseProjection> projectionInterface, Set<String> visited) throws ProjectionInitException {

        log.trace("Creating view for: {}", projectionInterface.getName());

        Class<? extends BaseProjection> effectiveProjection = getEffectiveProjection(projectionInterface);

        ProjectionInfo projectionInfo = projectionDefinitions.get(effectiveProjection);
        //Preventing cyclic reference
        if (visited.contains(effectiveProjection.getName())) {
            throw new ProjectionInitException(String.format("Cyclic dependencies in projections. Offending projection: %s , Parent projection: %s"
                    , effectiveProjection.getName()
                    , String.join(",", visited)));
        }

        Set<Method> baseProjectionMethods = Arrays.stream(BaseProjection.class.getMethods()).collect(Collectors.toSet());
        //compose view only by getters and exclude default interface methods
        Set<Method> projectionMethods = Arrays.stream(effectiveProjection.getMethods())
                .filter(ProjectionsConfigurationBean::isMethodCandidate)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // skip utility methods from BaseProjection
        projectionMethods.removeAll(baseProjectionMethods);

        View result = new View(projectionInfo.getEntityClass(), projectionInfo.getViewName());
        projectionInfo.setView(result);
        log.trace("View for: {} is created: {}, adding properties", effectiveProjection.getName(), result.getName());
        projectionMethods.forEach(method -> {
            //Check projection methods to have delegatable method in entity
            // (if returns another view interface check that the entity has a reference to another entity)
            if (MethodUtils.getAccessibleMethod(projectionInfo.entityClass, method.getName(), method.getParameterTypes()) == null) {
                throw new ProjectionInitException(
                        String.format("Method %s is not found in corresponding entity class %s"
                                , method
                                , projectionInfo.entityClass));
            }

            Class<?> fieldViewInterface = EntityProjectionWrapper.getMethodReturnType(method);

            log.trace("Checking if a method {} refers an entity with a certain view", method.getName());
            if (BaseProjection.class.isAssignableFrom(fieldViewInterface)) {

                ProjectionInfo refFieldInterfaceInfo = projectionDefinitions.get(fieldViewInterface);

                if (refFieldInterfaceInfo == null)
                    throw new ProjectionInitException(
                            String.format("Projection %s references %s projection which was not initially registered in ProjectionsConfigurationBean#scan"
                                    , effectiveProjection.getName()
                                    , fieldViewInterface.getName()));

                Set<String> parents = ImmutableSet.<String>builder().addAll(visited).add(effectiveProjection.getName()).build();
                View refFieldView = composeCubaView(refFieldInterfaceInfo.getProjectionInterface(), parents);
                refFieldInterfaceInfo.setView(refFieldView);

                addProperty(result, methodName2FieldName(method), refFieldView);
            } else {
                addProperty(result, methodName2FieldName(method), null);
            }
        });
        return result;
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
        log.trace("Adding property {} to projection {}", propName, targetView.getName());
        if (!targetView.containsProperty(propName)) {
            if (propView == null) {
                targetView.addProperty(propName);
            } else {
                targetView.addProperty(propName, propView);
            }
        }
    }

    private String methodName2FieldName(Method method) throws ProjectionInitException {
        try {
            PropertyDescriptor propertyDescriptor = BeanUtils.findPropertyForMethod(method);
            if (propertyDescriptor == null) {
                throw new BeanDefinitionValidationException(String.format("Method %s is not an accessor method", method.getName()));
            }
            return propertyDescriptor.getName();
        } catch (BeansException e) {
            throw new ProjectionInitException(String.format("Method %s of projections %s doesn't comply with access fields convention (setter or getter)",
                    method.getName(), method.getClass().getName()), e);
        }
    }


    /**
     * POJO containing all information about projection interface.
     */
    public static class ProjectionInfo {

        protected final Class<? extends BaseProjection> projectionInterface;

        protected final Class<Entity> entityClass;

        protected View view; //We create views in lazy manner after context is initialized

        protected final Class<? extends BaseProjection> replacedProjection;

        protected Class<? extends BaseProjection> replacedBy;

        public ProjectionInfo(@NotNull Class<? extends BaseProjection> projectionInterface, @NotNull Class<Entity> entityClass, Class<? extends BaseProjection> replacedProjection) {
            this.projectionInterface = projectionInterface;
            this.entityClass = entityClass;
            this.replacedProjection = replacedProjection;
        }

        public Class<? extends BaseProjection> getProjectionInterface() {
            return projectionInterface;
        }

        public Class<? extends Entity> getEntityClass() {
            return entityClass;
        }

        protected Class<? extends BaseProjection> getReplacedProjection() {
            return replacedProjection;
        }

        protected Class<? extends BaseProjection> getReplacedBy() {
            return replacedBy;
        }

        protected void setReplacedBy(Class<? extends BaseProjection> replacedBy) {
            this.replacedBy = replacedBy;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public String getViewName() {
            ProjectionName annotation = projectionInterface.getAnnotation(ProjectionName.class);
            if ((annotation == null) || (StringUtils.isEmpty(annotation.value()))) {
                return projectionInterface.getSimpleName();
            }
            return annotation.value();
        }

        @Override
        public String toString() {
            return "ProjectionInfo{" +
                    "projectionInterface=" + projectionInterface.getName() +
                    ", entityClass=" + entityClass.getName() +
                    '}';
        }
    }
}
