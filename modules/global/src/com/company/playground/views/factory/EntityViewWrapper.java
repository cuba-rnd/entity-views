package com.company.playground.views.factory;


import com.company.playground.views.sample.BaseEntityView;
import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Aleksey Stukalov on 22/08/2018.
 */
public class EntityViewWrapper {

    public static <E extends Entity, V extends BaseEntityView<E>> V wrap(E entity, Class<V> viewInterface) {
        //noinspection unchecked
        return (V) Proxy.newProxyInstance(entity.getClass().getClassLoader(), new Class<?>[] {viewInterface}
                , new ViewInterfaceInvocationHandler(entity));
    }

    static class ViewInterfaceInvocationHandler implements InvocationHandler, Serializable {

        private Entity entity;

        public ViewInterfaceInvocationHandler(Entity entity) {
            this.entity = entity;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Set<Method> baseEntityViewMethods = Arrays.stream(BaseEntityView.class.getMethods()).collect(Collectors.toSet());

            //TODO implement separately
            if (baseEntityViewMethods.contains(method))
                return null;

            //TODO check and implement setters

            Method entityMethod = entity.getClass().getMethod(method.getName(), method.getParameterTypes());

            if (Entity.class.isAssignableFrom(entityMethod.getReturnType())){
                Entity e = (Entity) entityMethod.invoke(entity, args);
                //noinspection unchecked
                return wrap(e, (Class) method.getReturnType());
            } else
                return entityMethod.invoke(entity, args);
        }
    }
}
