package com.company.playground.views.factory;


import com.company.playground.views.sample.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

            //Check if we should execute base interface method
            Set<Method> baseEntityViewMethods = Arrays.stream(BaseEntityView.class.getMethods())
                    .filter((m) -> m.getName().equals(method.getName()))
                    .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(baseEntityViewMethods)) {
                if ("getOrigin".equals(method.getName())) {
                    return entity;
                }
                //TODO implement transform()
                throw new UnsupportedOperationException(String.format("Method %s is not supported in view interfaces", method.getName()));
            }

            //TODO check and implement setters

            //Check if we should execute entity method
            Set<Method> entityMethods = Arrays.stream(entity.getClass().getMethods())
                    .filter((m) -> m.getName().equals(method.getName()))
                    .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(entityMethods)) {
                Method entityMethod = entity.getClass().getMethod(method.getName(), method.getParameterTypes());
                Object result = entityMethod.invoke(entity, args);
                return wrapResult(method, entityMethod, result);
            }

            //It is an interface default method - should be executed
            //TODO issues with calling default methods using reflection
            // @see https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
            Method interfaceMethod = viewInterface.getMethod(method.getName(), method.getParameterTypes());
            try {
                Object result = MethodHandles.lookup()
                        .in(viewInterface)
                        .unreflectSpecial(interfaceMethod, interfaceMethod.getDeclaringClass())
                        .bindTo(proxy)
                        .invokeWithArguments(args);
                return wrapResult(method, interfaceMethod, result);
            } catch (Throwable throwable) {
                throw new UnsupportedOperationException(String.format("Method %s is not supported in view interfaces", method.getName()), throwable);
            }
        }

        private Object wrapResult(Method method, Method entityMethod, Object result) {
            if (Entity.class.isAssignableFrom(entityMethod.getReturnType())
                    && BaseEntityView.class.isAssignableFrom(method.getReturnType())) {
                Entity e = (Entity) result;
                //noinspection unchecked
                return wrap(e, (Class) method.getReturnType());
            } else {
                return result;
            }
        }
    }
}
