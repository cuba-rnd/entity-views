package com.haulmont.addons.cuba.entity.views.factory;


import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfiguration;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.Loader;
import javassist.NotFoundException;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that "wraps" entity into Entity view by creating a proxy class that implements entity view interface
 * contract. Please note that class uses application context despite on the fact that all methods are static.
 */
public class EntityViewWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityViewWrapper.class);

    /**
     * Wraps entity instance into entity view interface.
     *
     * @param entity        Entity instance to be wrapped.
     * @param viewInterface Entity View Interface class.
     * @param <E>           Entity Class
     * @param <V>           Effective entity view interface class.
     * @param <K>           Entity ID key class.
     * @return Proxy that implements entity view interface of class <code>V</code>
     */
    public static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V wrap(E entity, Class<V> viewInterface) {
        if (entity == null) {
            return null;
        }
        log.trace("Wrapping entity: {} to view: {}", entity, viewInterface);
        Class<? extends BaseEntityView> effectiveView = AppBeans.get(ViewsConfiguration.class).getEffectiveView(viewInterface);
        log.trace("Effective view: {}", effectiveView);

        try {
            V wrappedEntity = (V) wrapEntity(entity, effectiveView);
            log.info(String.valueOf(wrappedEntity.getId()));
            return wrappedEntity;
        } catch (Exception e) {
            throw new RuntimeException("Cannot wrap entity into the View", e);
        }
    }

    private static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V wrapEntity(E entity, Class<V> viewInterface)
            throws NotFoundException, IOException, CannotCompileException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {

        Type[] actualTypeArguments = ((ParameterizedType) (((Class<?>) (viewInterface.getGenericInterfaces()[0])).getGenericInterfaces()[0])).getActualTypeArguments();

        List<Class<?>> typeArguments = Collections.EMPTY_LIST;

        typeArguments = Arrays.stream(actualTypeArguments).map((arg) -> {
            try {
                return Class.forName(arg.getTypeName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        typeArguments.add(1, viewInterface);

        List<Method> methodList = Arrays.stream(viewInterface.getMethods())
                .filter((method) -> MethodUtils.getMatchingMethod(BaseEntityViewImpl.class, method.getName(), method.getParameterTypes()) == null)
                .collect(Collectors.toList());

        ClassPool pool = ClassPool.getDefault();
        CtClass baseClass = pool.get("com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper$BaseEntityViewImpl");
        CtClass viewIf = pool.get(viewInterface.getName());
        CtClass wrapper = pool.makeClass("com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper" + viewInterface.getSimpleName(), baseClass);
        wrapper.addInterface(viewIf);
        wrapper.writeFile();
        Class<?> aClass = wrapper.toClass();
        Constructor<?> constructor = aClass.getConstructors()[0];
        return (V) constructor.newInstance(entity, viewInterface);
    }


    /**
     * Checks if a method's invocation can be delegated to a class.
     *
     * @param delegateFromMethod Method to be invoked.
     * @param delegateToClass    Candidate class.
     * @return Method instance if the class contain its definition.
     */
    static Method getDelegateMethodCandidate(Method delegateFromMethod, Class<?> delegateToClass) {
        String fromMethodName = delegateFromMethod.getName();
        Class<?>[] parameterTypes = delegateFromMethod.getParameterTypes();

        Method entityMethod = MethodUtils.getAccessibleMethod(delegateToClass, fromMethodName, parameterTypes);

        if (entityMethod != null &&
                (delegateFromMethod.getReturnType().isAssignableFrom(entityMethod.getReturnType())
                        || isWrappable(delegateFromMethod, entityMethod))) {
            return entityMethod;
        } else if (isSetterWithView(delegateFromMethod, parameterTypes)) {
            entityMethod = Arrays.stream(delegateToClass.getMethods())
                    .filter(method -> method.getName().equals(fromMethodName) && method.getParameterCount() == 1)
                    .findFirst().orElse(null);
            return entityMethod;
        }

        return null;
    }


    /**
     * Returns actual method return type or collection parameter type for one-to-many
     * relation attributes. Used for building CUBA views based on entity views.
     *
     * @param viewMethod method to be used in CUBA view.
     * @return type that will be used in CUBA view.
     */
    public static Class<?> getReturnViewType(Method viewMethod) {
        Class<?> returnType = viewMethod.getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            Type genericReturnType = viewMethod.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericReturnType;
                List<Class<?>> collectionTypes = Arrays.stream(type.getActualTypeArguments())
                        .map(t -> ReflectionHelper.getClass(t.getTypeName())).collect(Collectors.toList());
                //TODO make this code a bit more accurate
                if (collectionTypes.stream().anyMatch(BaseEntityView.class::isAssignableFrom)) {
                    return collectionTypes.stream().filter(BaseEntityView.class::isAssignableFrom).findFirst().orElseThrow(RuntimeException::new);
                } else {
                    return collectionTypes.stream().findFirst().orElseThrow(RuntimeException::new);
                }
            }
        }
        return returnType;
    }


    /**
     * Checks if a method is setter method that has only one parameter of a type BaseEntityView.
     *
     * @param method Method to be verified.
     * @param args   Method's args.
     * @return True if is's a proper setter.
     */
    private static boolean isSetterWithView(Method method, Object[] args) {
        return method.getReturnType().equals(Void.TYPE)
                && method.getName().startsWith("set")
                && (args.length == 1)
                && (BaseEntityView.class.isAssignableFrom((Class<?>) args[0]));
    }

    /**
     * Checks if a method's invocation result can be wrapped into entity view.
     *
     * @param viewMethod   Method to check.
     * @param entityMethod Effective entity method to be invoked.
     * @return True if invocation result can be wrapped.
     */
    static boolean isWrappable(Method viewMethod, Method entityMethod) {
        return Entity.class.isAssignableFrom(entityMethod.getReturnType())
                && BaseEntityView.class.isAssignableFrom(viewMethod.getReturnType());
    }

    public static abstract class BaseEntityViewImpl<E extends Entity<K>, V extends BaseEntityView<E, K>, K> implements BaseEntityView<E, K> {

        protected final E entity;
        private boolean needReload;
        private final Class<V> viewInterface;
        private View view;

        public BaseEntityViewImpl(E entity, Class<V> viewInterface) {
            this.entity = entity;
            this.viewInterface = viewInterface;
        }

        public boolean isNeedReload() {
            return needReload;
        }

        public void setNeedReload(boolean needReload) {
            this.needReload = needReload;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        @Override
        public E getOrigin() {
            return entity;
        }

        @Override
        public Class<V> getInterfaceClass() {
            return viewInterface;
        }

        @Override
        public <V extends BaseEntityView<E, K>> V reload(Class<V> targetView) {
            return null;
        }

        @Override
        public K getId() {
            return entity.getId();
        }

        @Override
        public MetaClass getMetaClass() {
            return entity.getMetaClass();
        }

        @Override
        @Deprecated
        public String getInstanceName() {
            return entity.getInstanceName();
        }

        @Override
        @Nullable
        public <T> T getValue(String name) {
            return entity.getValue(name);
        }

        @Override
        public void setValue(String name, Object value) {
            entity.setValue(name, value);
        }

        @Override
        @Nullable
        public <T> T getValueEx(String propertyPath) {
            return entity.getValueEx(propertyPath);
        }

        @Override
        @Nullable
        public <T> T getValueEx(BeanPropertyPath propertyPath) {
            return entity.getValueEx(propertyPath);
        }

        @Override
        public void setValueEx(String propertyPath, Object value) {
            entity.setValueEx(propertyPath, value);
        }

        @Override
        public void setValueEx(BeanPropertyPath propertyPath, Object value) {
            entity.setValueEx(propertyPath, value);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            entity.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            entity.removePropertyChangeListener(listener);
        }

        @Override
        public void removeAllListeners() {
            entity.removeAllListeners();
        }
    }
}
