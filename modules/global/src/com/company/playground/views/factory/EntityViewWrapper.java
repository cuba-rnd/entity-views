package com.company.playground.views.factory;


import com.company.playground.views.sample.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

/**
 * Created by Aleksey Stukalov on 22/08/2018.
 */
public class EntityViewWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityViewWrapper.class);

    public static <E extends Entity, V extends BaseEntityView<E>> V wrap(E entity, Class<V> viewInterface) {

        ViewInterfaceInvocationHandler interfaceInvocationHandler = new ViewInterfaceInvocationHandler(entity, viewInterface);
        //noinspection unchecked
        return (V) Proxy.newProxyInstance(entity.getClass().getClassLoader()
                , new Class<?>[]{viewInterface}
                , interfaceInvocationHandler);
    }

    static class ViewInterfaceInvocationHandler implements InvocationHandler, Serializable {

        private Entity entity;
        private Class<? extends BaseEntityView> viewInterface;

        public ViewInterfaceInvocationHandler(Entity entity, Class<? extends BaseEntityView> viewInterface) {
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
                log.trace("Invoking method {} from BaseEntityView", methodName);
                if ("getOrigin".equals(methodName)) {
                    return entity;
                }
                //TODO implement transform()
                throw new UnsupportedOperationException(String.format("Method %s is not supported in view interfaces", methodName));
            }

            //TODO check and implement setters

            //Check if we should execute entity method
            Method entityMethod = getDelegateMethodCandidate(method, entity.getClass());
            if (entityMethod != null) {
                log.trace("Invoking method {} from Entity class: {} name: {}", methodName, entity.getClass(), entity.getInstanceName());
                Object result = entityMethod.invoke(entity, args);
                return wrapResult(method, entityMethod, result);
            }

            //It is an interface default method - should be executed
            //TODO issues with calling default methods using reflection
            // @see https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
            Method interfaceMethod = viewInterface.getMethod(methodName, method.getParameterTypes());
            log.trace("Invoking default method {} from interface {}", methodName, method.getDeclaringClass());
            try {
                //HACK! Does not work in Java 9 and 10
                Constructor<MethodHandles.Lookup> constructor =
                        MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                constructor.setAccessible(true);
                Object result = constructor.newInstance(viewInterface)
                        .in(viewInterface)
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

                if (entityMethod == null)
                    return null;

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
            if (isWrappable(method, entityMethod)) {
                //noinspection unchecked
                return wrap((Entity) result, (Class) method.getReturnType());
            } else {
                return result;
            }
        }
    }
}
