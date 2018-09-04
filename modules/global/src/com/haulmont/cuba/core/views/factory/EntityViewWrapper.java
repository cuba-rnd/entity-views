package com.haulmont.cuba.core.views.factory;


import com.haulmont.cuba.core.config.ConfigHandler;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.views.BaseEntityView;
import com.haulmont.cuba.core.views.scan.ViewsConfiguration;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Class that "wraps" entity into Entity view by creating a proxy class that implements entity view interface
 * contract. Please note that class uses application context despite on the fact that all methods are static.
 */
public class EntityViewWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityViewWrapper.class);

    /**
     * Wraps entity instance into entity view interface.
     * @param entity Entity instance to be wrapped.
     * @param viewInterface Entity View Interface class.
     * @param <E> Entity Class
     * @param <V> Effective entity view interface class.
     * @return Proxy that implements entity view interface of class <code>V</code>
     */
    public static <E extends Entity, V extends BaseEntityView<E>> V wrap(E entity, Class<V> viewInterface) {
        log.trace("Wrapping entity: {} to view: {}", entity.getInstanceName(), viewInterface);
        Class<? extends BaseEntityView> effectiveView = AppBeans.get(ViewsConfiguration.class).getEffectiveView(viewInterface);
        log.trace("Effective view: {}", effectiveView);
        //noinspection unchecked
        ViewInterfaceInvocationHandler interfaceInvocationHandler = new ViewInterfaceInvocationHandler<>(entity, effectiveView);
        //noinspection unchecked
        return (V) Proxy.newProxyInstance(entity.getClass().getClassLoader()
                , new Class<?>[]{effectiveView}
                , interfaceInvocationHandler);
    }

    /**
     * Handler that process all invocations of a entity view's methods.
     * @param <E> Underlying entity's class.
     * @param <V> Entity View interface class.
     */
    static class ViewInterfaceInvocationHandler<E extends Entity, V extends BaseEntityView<E>> implements InvocationHandler, Serializable {

        private final E entity;
        private final Class<V> viewInterface;

        //Internals look similar to com.haulmont.cuba.core.views.scan.ViewsConfiguration.ViewInterfaceInfo
        //Should we think about merging these classes code somehow?
        ViewInterfaceInvocationHandler(E entity, Class<V> viewInterface) {
            this.entity = entity;
            this.viewInterface = viewInterface;
        }

        /**
         * @see InvocationHandler#invoke(Object, Method, Object[])
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

            final String methodName = method.getName();

            //Check if we should execute base interface method
            Method baseEntityViewMethod = getDelegateMethodCandidate(method, BaseEntityView.class);
            if (baseEntityViewMethod != null) {
                return executeBaseEntityMethod(proxy, args, baseEntityViewMethod);
            }

            //Check if we should execute origin entity method
            //Setter
            if (isSetterWithView(method, args)) {
                return MethodUtils.invokeMethod(entity, methodName, ((BaseEntityView)args[0]).getOrigin());
            }
            //Or getter or another method
            Method entityMethod = getDelegateMethodCandidate(method, entity.getClass());
            if (entityMethod != null) {
                return executeEntityMethod(method, args, entityMethod);
            }

            //It is an interface default method - should be executed
            return executeDefaultMethod(proxy, method, args);
        }

        /**
         * Checks if a method is setter method that has only one parameter of a type BaseEntityView.
         * @param method Method to be verified.
         * @param args Method's args.
         * @return True if is's a proper setter.
         */
        private boolean isSetterWithView(Method method, Object[] args) {
            return method.getReturnType().equals(Void.TYPE)
                    && method.getName().startsWith("set")
                    && (args.length == 1)
                    && (args[0] instanceof BaseEntityView);
        }

        /**
         * Invokes methods defined for all entity views in BaseEntityView.
         * @param proxy Entity view interface instance.
         * @param args Method's arguments.
         * @param method Method candidate to be invoked.
         * @return Method invocation result.
         * @throws InvocationTargetException If method is not found in the view interface instance.
         * @throws IllegalAccessException If entity view instance's method is not accessible.
         */
        //TODO We'd better create BaseEntityViewImpl and implement its methods there like in JPA Interfaces
        private Object executeBaseEntityMethod(Object proxy, Object[] args, Method method) throws InvocationTargetException, IllegalAccessException {
            String methodName = method.getName();
            log.trace("Invoking method {} from BaseEntityView", methodName);
            if ("getOrigin".equals(methodName)) {
                return entity;
            } else if ("transform".equals(methodName)) {
                //noinspection unchecked
                return transform(viewInterface, (Class) args[0], proxy);
            } else if ("getInterfaceClass".equals(methodName)) {
                return viewInterface;
            } else {
                return method.invoke(entity, args);
            }
        }

        /**
         * Implementation of the {@link BaseEntityView#transform(Class)}. Does not reload entity from data store if
         * we transform to a "parent" interface.
         * @param currentViewInterface Source interface class.
         * @param newViewInterface Target interface class.
         * @param proxy Current Entity View interface instance.
         * @param <T> Target interface class.
         * @return Target interface instance.
         */
        private <T extends BaseEntityView> T transform(Class<? extends BaseEntityView> currentViewInterface, Class<T> newViewInterface, Object proxy) {
            if (currentViewInterface.isAssignableFrom(newViewInterface))
                //noinspection unchecked
                return (T) proxy;

            EntityStates entityStates = AppBeans.get(EntityStates.class);
            ViewsConfiguration vc = AppBeans.get(ViewsConfiguration.class);
            View newView = vc.getViewByInterface(newViewInterface);
            Entity e = entity;
            if (!entityStates.isLoadedWithView(entity, newView)) {
                DataManager dm = AppBeans.get(DataManager.class);
                e = dm.reload(entity, newView);//TODO need to provide consistent behaviour for transform()
            }
            //noinspection unchecked
            return (T) wrap(e, newViewInterface);
        }

        /**
         * Executes entity methods apart from setters. Setter methods are executed separately.
         * @param method Method to be executed.
         * @param args Method's arguments.
         * @param entityMethod Effective instance's method that will be executed.
         * @return Method invocation result.
         * @throws IllegalAccessException If entity instance's method is not accessible.
         * @throws InvocationTargetException If method is not found in the entity instance.
         */
        private Object executeEntityMethod(Method method, Object[] args, Method entityMethod) throws IllegalAccessException, InvocationTargetException {
            log.trace("Invoking method {} from Entity class: {} name: {}", method.getName(), entity.getClass(), entity.getInstanceName());
            Object result = entityMethod.invoke(entity, args);
            return wrapResult(method, entityMethod, result);
        }

        /**
         * Default interface method invocation. Refactor this and ensure that we have the same code everywhere.
         * @param proxy Entity View interface proxy instance.
         * @param method Interface default method to be invoked.
         * @param args Method's arguments.
         * @return
         * @throws NoSuchMethodException
         * @link https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
         * @see com.haulmont.cuba.core.config.ConfigDefaultMethod#invoke(ConfigHandler, Object[], Object)
         */
        private Object executeDefaultMethod(Object proxy, Method method, Object[] args) throws NoSuchMethodException {
            //TODO consider refactoring
            Method interfaceMethod = viewInterface.getMethod(method.getName(), method.getParameterTypes());
            Class<?> declaringClass = method.getDeclaringClass();
            log.trace("Invoking default method {} from interface {}", method.getName(), declaringClass);
            try {
                //HACK! Does not work in Java 9 and 10
                Constructor<MethodHandles.Lookup> constructor =
                        MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                constructor.setAccessible(true);
                Object result = constructor.newInstance(declaringClass)
                        .in(declaringClass)
                        .unreflectSpecial(interfaceMethod, interfaceMethod.getDeclaringClass())
                        .bindTo(proxy)
                        .invokeWithArguments(args);
                return wrapResult(method, interfaceMethod, result);
            } catch (Throwable throwable) {
                throw new UnsupportedOperationException(String.format("Method %s is not supported in view interfaces", method.getName()), throwable);
            }
        }

        /**
         * Checks if a method's invocation can be delegated to a class.
         * @param delegateFromMethod Method to be invoked.
         * @param delegateToClass Candidate class.
         * @return Method instance if the class contain its definition.
         */
        private Method getDelegateMethodCandidate(Method delegateFromMethod, Class<?> delegateToClass) {
            try {
                Method entityMethod = delegateToClass.getMethod(delegateFromMethod.getName(), delegateFromMethod.getParameterTypes());

                if (delegateFromMethod.getReturnType().isAssignableFrom(entityMethod.getReturnType()))
                    return entityMethod;

                if (isWrappable(delegateFromMethod, entityMethod))
                    return entityMethod;

                return null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        /**
         * Checks if a method's invocation result can be wrapped into entity view.
         * @param viewMethod Method to check.
         * @param entityMethod Effective entity method to be invoked.
         * @return True if invocation result can be wrapped.
         */
        private boolean isWrappable(Method viewMethod, Method entityMethod) {
            return Entity.class.isAssignableFrom(entityMethod.getReturnType())
                    && BaseEntityView.class.isAssignableFrom(viewMethod.getReturnType());
        }

        /**
         * Wraps method invocation result into entity view if needed.
         * @param method Method to be invoked.
         * @param entityMethod Effective entity method to be wrapped.
         * @param result Invocation result.
         * @return Wrapped result.
         */
        private Object wrapResult(Method method, Method entityMethod, Object result) {
            if (result == null) {
                return null;
            }
            if (isWrappable(method, entityMethod)) {
                //noinspection unchecked
                return wrap((Entity) result, (Class) method.getReturnType());
            } else {
                return result;
            }
        }
    }
}
