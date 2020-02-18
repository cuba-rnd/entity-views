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
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
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
    @SuppressWarnings("unchecked")
    public static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V wrap(E entity, Class<V> viewInterface) {
        if (entity == null) {
            return null;
        }
        log.trace("Wrapping entity: {} to view: {}", entity, viewInterface);
        Class<? extends BaseEntityView> effectiveView = AppBeans.get(ViewsConfiguration.class).getEffectiveView(viewInterface);
        log.trace("Effective view: {}", effectiveView);

        try {
            Class<?> aClass = getWrapperClass(effectiveView);
            Constructor<?> constructor = aClass.getConstructors()[0];
            return (V) constructor.newInstance(entity, effectiveView);
        } catch (Exception e) {
            throw new RuntimeException("Cannot wrap entity " + entity + "into View " + effectiveView.getName(), e);
        }
    }

    public static Class<?> getWrapperClass(Class<? extends BaseEntityView> effectiveView) throws NotFoundException, CannotCompileException, IOException {
        String wrapperName = createWrapperClassName(effectiveView);
        Class<?> aClass = null;
        try {
            aClass = Class.forName(wrapperName);
        } catch (ClassNotFoundException e) {
            log.debug("Wrapper implementation class for view " + effectiveView.getName() + " is not created. Trying to create it.");
        }
        if (aClass == null) {
            aClass = createWrapperImplementation(effectiveView, wrapperName);
        }
        return aClass;
    }

    private static String createWrapperClassName(Class<?> effectiveView) {
        return String.format("%sWrapperImpl", effectiveView.getName());
    }

    private static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> Class<?> createWrapperImplementation(Class<V> viewInterface, String wrapperName) throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass baseClass = pool.get(BaseEntityViewImpl.class.getName());
        CtClass wrappingEntityClass = pool.get(getViewEntityClassName(viewInterface));
        CtClass viewIf = pool.get(viewInterface.getName());

        CtClass wrapperClass = pool.makeClass(wrapperName, baseClass);
        wrapperClass.addInterface(viewIf);

        CtMethod getOrigin = CtNewMethod.make(wrappingEntityClass,
                "getOrigin",
                null,
                null,
                "return (" + wrappingEntityClass.getName() + ")entity;",
                wrapperClass);
        wrapperClass.addMethod(getOrigin);

        List<Method> entityViewMethods = getEntityViewMethods(viewInterface);

        entityViewMethods.forEach(m -> {
            try {
                wrapperClass.addMethod(createDelegateMethod(wrapperClass, m, pool));
            } catch (NotFoundException | CannotCompileException e) {
                throw new IllegalArgumentException("Cannot add method " + m.getName() + " to wrapper class " + wrapperName, e);
            }
        });
        wrapperClass.writeFile();
        Class<?> aClass = wrapperClass.toClass();
        wrapperClass.debugWriteFile("c:/temp");
        return aClass;
    }

    private static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> List<Method> getEntityViewMethods(Class<V> viewInterface) {
        return Arrays.stream(viewInterface.getMethods())
                .filter(method -> MethodUtils.getMatchingMethod(BaseEntityViewImpl.class, method.getName(), method.getParameterTypes()) == null
                        && !method.isDefault())
                .collect(Collectors.toList());
    }

    private static CtMethod createDelegateMethod(CtClass wrapper, Method m, ClassPool pool) throws NotFoundException, CannotCompileException {
        CtClass[] paramTypes = pool.get(Arrays.stream(m.getParameterTypes())
                .map(Class::getName).toArray(String[]::new));

        CtClass[] exceptionTypes = pool.get(Arrays.stream(m.getExceptionTypes())
                .map(Class::getName).toArray(String[]::new));

        String body = "throw new IllegalArgumentException(\"Only setters and getters are supported. Use default methods in Views if needed\");";

        if (m.getName().startsWith("set")) {
            Class<?> parameterType = m.getParameterTypes()[0];
            if (BaseEntityView.class.isAssignableFrom(parameterType)) {
                String paramTypeName = getViewEntityClassName((Class<? extends BaseEntityView>)parameterType);
                body = "getOrigin()." + m.getName() + "(("+paramTypeName+")($1.getOrigin()));";
            } else {
                body = "getOrigin()." + m.getName() + "($1);";
            }
        } else if (m.getName().startsWith("get")) {
            if (Collection.class.isAssignableFrom(m.getReturnType())) {
                Class<?> collectionGenericType = getReturnViewType(m);
                body = "return new "+WrappingList.class.getName()+"(getOrigin()." + m.getName() + "(), " + collectionGenericType.getName() + ".class);";
            } else if (BaseEntityView.class.isAssignableFrom(m.getReturnType())) {
                body = "return " + EntityViewWrapper.class.getName() + ".wrap(getOrigin()." + m.getName() + "(), " + m.getReturnType().getName() + ".class);";
            } else {
                body = "return getOrigin()." + m.getName() + "();";
            }
        }

        return CtNewMethod.make(m.getModifiers(),
                pool.get(m.getReturnType().getName()),
                m.getName(),
                paramTypes,
                exceptionTypes,
                "{" + body + "}",
                wrapper);
    }

    private static String getViewEntityClassName(Class<? extends BaseEntityView> effectiveView)  {
        Type[] genericInterfaces = effectiveView.getGenericInterfaces();

        while ((genericInterfaces instanceof Class<?>[])) {
            genericInterfaces = ((Class<?>[])genericInterfaces)[0].getGenericInterfaces();
        }
        return ((ParameterizedType) (genericInterfaces[0])).getActualTypeArguments()[0].getTypeName();
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
        public <U extends BaseEntityView<E, K>> U reload(Class<U> targetView) {
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
