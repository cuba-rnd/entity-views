package com.company.playground.views.factory;


import com.company.playground.views.sample.BaseEntityView;
import com.company.playground.views.scan.ViewsConfiguration;
import com.haulmont.cuba.core.config.ConfigHandler;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.View;
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
 * Created by Aleksey Stukalov on 22/08/2018.
 */
public class EntityViewWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityViewWrapper.class);

    public static <E extends Entity, V extends BaseEntityView<E>> V wrap(E entity, Class<V> viewInterface) {
        log.trace("Wrapping entity: {} to view: {}", entity.getInstanceName(), viewInterface);
        ViewInterfaceInvocationHandler interfaceInvocationHandler = new ViewInterfaceInvocationHandler<>(entity, viewInterface);
        //noinspection unchecked
        return (V) Proxy.newProxyInstance(entity.getClass().getClassLoader()
                , new Class<?>[]{viewInterface}
                , interfaceInvocationHandler);
    }

    static class ViewInterfaceInvocationHandler <E extends Entity, V extends BaseEntityView<E>> implements InvocationHandler, Serializable {

        private final E entity;
        private final Class<V>viewInterface;

        public ViewInterfaceInvocationHandler(E entity, Class<V> viewInterface) {
            this.entity = entity;
            this.viewInterface = viewInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

            final String methodName = method.getName();

            //Check if we should execute base interface method
            Method baseEntityViewMethod = getDelegateMethodCandidate(method, BaseEntityView.class);
            if (baseEntityViewMethod != null) {
                return executeBaseEntityMethod(proxy, args, baseEntityViewMethod);
            }

            //Check if we should execute entity method
            Method entityMethod = getDelegateMethodCandidate(method, entity.getClass());
            if (entityMethod != null) {
                return executeEntityMethod(method, args, methodName, entityMethod);
            }


            //It is an interface default method - should be executed
            return executeDefaultMethod(proxy, method, args, methodName);
        }

        //TODO We'd better create BaseEntityViewImpl and implement its methods there like in JPA Interfaces
        private Object executeBaseEntityMethod(Object proxy, Object[] args, Method method) throws InvocationTargetException, IllegalAccessException {
            String methodName = method.getName();
            log.trace("Invoking method {} from BaseEntityView", methodName);
            if ("getOrigin".equals(methodName)) {
                return entity;
            } else if ("transform".equals(methodName)) {
                //noinspection unchecked
                return transform(viewInterface, (Class) args[0], proxy);
            } else if ("getInterfaceClass".equals(methodName)){
                return viewInterface;
            } else {
                return method.invoke(entity, args);
            }
        }

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
                e = dm.reload(entity, newView);
            }
            //noinspection unchecked
            return (T) wrap(e, newViewInterface);
        }



        private Object executeEntityMethod(Method method, Object[] args, String methodName, Method entityMethod) throws IllegalAccessException, InvocationTargetException {
            log.trace("Invoking method {} from Entity class: {} name: {}", methodName, entity.getClass(), entity.getInstanceName());
            Object result = entityMethod.invoke(entity, args);
            return wrapResult(method, entityMethod, result);
        }

        /**
         * Default interface method invocation. Refactor this and ensure that we have the same code as in.
         * @see com.haulmont.cuba.core.config.ConfigDefaultMethod#invoke(ConfigHandler, Object[], Object)
         * @link https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
         *
         *
         * @param proxy
         * @param method
         * @param args
         * @param methodName
         * @return
         * @throws NoSuchMethodException
         */
        private Object executeDefaultMethod(Object proxy, Method method, Object[] args, String methodName) throws NoSuchMethodException {
            //TODO consider refactoring
            Method interfaceMethod = viewInterface.getMethod(methodName, method.getParameterTypes());
            Class<?> declaringClass = method.getDeclaringClass();
            log.trace("Invoking default method {} from interface {}", methodName, declaringClass);
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
                throw new UnsupportedOperationException(String.format("Method %s is not supported in view interfaces", methodName), throwable);
            }
        }

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

        private boolean isWrappable(Method viewMethod, Method entityMethod) {
            return Entity.class.isAssignableFrom(entityMethod.getReturnType())
                    && BaseEntityView.class.isAssignableFrom(viewMethod.getReturnType());
        }

        private Object wrapResult(Method method, Method entityMethod, Object result) {
            if (result == null){
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
