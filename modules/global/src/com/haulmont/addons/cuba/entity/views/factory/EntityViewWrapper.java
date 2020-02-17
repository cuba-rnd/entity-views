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
import javassist.CtMember;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.SignatureAttribute;
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
import java.util.stream.Stream;

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
            throws Exception {

        String wrapperName = "com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper" + viewInterface.getSimpleName();
        ClassPool pool = ClassPool.getDefault();

        CtClass wrapper = pool.getOrNull(wrapperName);
        Class<?> aClass = null;
        if (wrapper == null) {
            CtClass baseClass = pool.get("com.haulmont.addons.cuba.entity.views.factory.EntityViewWrapper$BaseEntityViewImpl");
            CtClass viewIf = pool.get(viewInterface.getName());
            CtClass wrapperClass = pool.makeClass(wrapperName, baseClass);
            wrapperClass.addInterface(viewIf);

            String[] genericTypesList = getInterfaceGenerics(viewIf).toArray(new String[0]);
            CtClass[] genericClasses = pool.get(genericTypesList);

            CtMethod getOrigin = CtNewMethod.make(genericClasses[0], "getOrigin", null, null, "return (" + genericClasses[0].getName() + ")entity;", wrapperClass);
            wrapperClass.addMethod(getOrigin);


            wrapperClass.debugWriteFile("c:/temp");

            //Get ViewInterface signature

            //Add generic signature
            List<Method> methodList = Arrays.stream(viewInterface.getMethods())
                    .filter(method -> MethodUtils.getMatchingMethod(BaseEntityViewImpl.class, method.getName(), method.getParameterTypes()) == null
                            && !method.isDefault())
                    .collect(Collectors.toList());
            methodList.forEach(m -> addDelegateMethod(wrapperClass, m, pool));
            wrapperClass.writeFile();
            wrapper = wrapperClass;
            aClass = wrapper.toClass();
        } else {
            aClass = Class.forName(wrapperName);
        }
        Constructor<?> constructor = aClass.getConstructors()[0];
        return (V) constructor.newInstance(entity, viewInterface);
    }

    private static List<String> getInterfaceGenerics(CtClass viewIf) throws BadBytecode, NotFoundException {
        String genericSignature = viewIf.getGenericSignature();
        while (genericSignature == null && viewIf.getInterfaces().length > 0) {
            viewIf = viewIf.getInterfaces()[0];
            genericSignature = viewIf.getGenericSignature();
        }
        SignatureAttribute.ClassSignature classSignature = SignatureAttribute.toClassSignature(viewIf.getGenericSignature());
        SignatureAttribute.ClassType[] interfaces = classSignature.getInterfaces();
        if (interfaces.length != 1) {
            throw new IllegalArgumentException("You should implement only one Entity View interface");
        }
        SignatureAttribute.TypeArgument[] typeArguments = interfaces[0].getTypeArguments();
        if (typeArguments.length != 2) {
            throw new IllegalArgumentException("There must be two type arguments in the Entity View interface");
        }
        return Arrays.stream(typeArguments).map(type -> type.getType().jvmTypeName()).collect(Collectors.toList());
    }

    private static void addDelegateMethod(CtClass wrapper, Method m, ClassPool pool) {
        try {
            CtClass[] paramTypes = pool.get(Arrays.stream(m.getParameterTypes())
                    .map(Class::getName)
                    .collect(Collectors.toList())
                    .toArray(new String[0]));

            CtClass[] exceptionTypes = pool.get(Arrays.stream(m.getExceptionTypes())
                    .map(Class::getName)
                    .collect(Collectors.toList())
                    .toArray(new String[0]));

            String body = "throw new IllegalArgumentException(\"Only setters and getters are supported. Use default methods in Views if needed\");";

            if (m.getName().startsWith("set")) {
                if (BaseEntityView.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    body = "getOrigin()." + m.getName() + "($1.getOrigin());";
                } else {
                    body = "getOrigin()." + m.getName() + "($1);";
                }
            } else if (m.getName().startsWith("get")) {
                if (BaseEntityView.class.isAssignableFrom(m.getReturnType())) {
                    body = "return " + EntityViewWrapper.class.getName() + ".wrap(getOrigin()." + m.getName() + "(), " + m.getReturnType().getName() + ".class);";
                } else {
                    body = "return getOrigin()." + m.getName() + "();";
                }
            }

            CtMethod newMethod = CtNewMethod.make(m.getModifiers(),
                    pool.get(m.getReturnType().getName()),
                    m.getName(),
                    paramTypes,
                    exceptionTypes,
                    "{" + body + "}",
                    wrapper);

            wrapper.addMethod(newMethod);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot add method " + m.getName() + " to a generated wrapper", e);
        }
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
