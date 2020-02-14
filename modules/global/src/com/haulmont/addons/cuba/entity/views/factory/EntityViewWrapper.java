package com.haulmont.addons.cuba.entity.views.factory;


import com.haulmont.addons.cuba.entity.views.BaseEntityView;
import com.haulmont.addons.cuba.entity.views.scan.ViewsConfiguration;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.config.ConfigHandler;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntityStates;
import com.haulmont.cuba.core.global.View;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.DefaultMethodCall;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.implementation.bytecode.assign.reference.GenericTypeAwareAssigner;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.persistence.internal.codegen.MethodDefinition;
import org.eclipse.persistence.jpa.jpql.parser.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.tools.ForwardingJavaFileManager;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytebuddy.matcher.ElementMatchers.named;

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
            V bbProxy = (V) wrapEntity(entity, effectiveView);
            log.info(String.valueOf(bbProxy.getId()));
            return bbProxy;
        } catch (Exception e) {
            throw new RuntimeException("Cannot wrap entity into the View", e);
        }
    }

    private static <E extends Entity<K>, V extends BaseEntityView<E, K>, K> V wrapEntity(E entity, Class<V> viewInterface)
            throws IllegalAccessException, InstantiationException {

        ByteBuddy bb = new ByteBuddy(ClassFileVersion.ofThisVm());

/*
        List<TypeVariable<? extends Class<?>>[]> typeVariables = ClassUtils.getAllInterfaces(viewInterface).stream()
                .filter(BaseEntityView.class::isAssignableFrom)
                .map(Class::getTypeParameters)
                .collect(Collectors.toList());
*/

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

        TypeDescription.Generic generic = TypeDescription.Generic.Builder.parameterizedType(BaseEntityViewImpl.class, typeArguments).build();

        DynamicType.Builder definition = bb.subclass(generic)
                .implement(viewInterface);

        List<Method> methodList = Arrays.stream(viewInterface.getMethods())
                .filter((method) -> MethodUtils.getMatchingMethod(BaseEntityViewImpl.class, method.getName(), method.getParameterTypes()) == null)
                .collect(Collectors.toList());

        try {

            for (Method m : methodList) {
                if (m.isDefault()) {
                    definition = definition.defineMethod(m.getName(), m.getReturnType(), m.getModifiers())
                            .withParameters(m.getParameterTypes())
                            .intercept(DefaultMethodCall.unambiguousOnly());
                } else {
                    Method methodToExecute = getDelegateMethodCandidate(m, entity.getClass());
                    if (methodToExecute != null) {
                        log.info("Delegating {} to {}", m, methodToExecute);
                        definition = definition.defineMethod(m.getName(), m.getReturnType(), m.getModifiers())
                                .withParameters(m.getParameterTypes())
                                .intercept(MethodCall.invoke(methodToExecute)
                                        .on(entity)
                                        .withAllArguments().with(new MethodCall.ArgumentLoader.Factory() {
                                            @Override
                                            public MethodCall.ArgumentLoader.ArgumentProvider make(Implementation.Target implementationTarget) {
                                                return null;
                                            }

                                            @Override
                                            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                                                return null;
                                            }
                                        }));
                    } else {
                        throw new IllegalArgumentException("Method " + m + " cannot be intercepted because there is no suitable method in class " + entity.getClass());
                    }
                }
            }

            Class<? extends BaseEntityViewImpl> loaded = definition.make().load(entity.getClass().getClassLoader()).getLoaded();

            return (V) loaded.getConstructor(Entity.class, viewInterface.getClass()).newInstance(entity, viewInterface);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot instantiate wrapper", e);
        }

    }

    enum WrappingAssigner implements Assigner {

        INSTANCE;

        @Override
        public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
            try {
                if (BaseEntityView.class.isAssignableFrom(Class.forName(source.getTypeName()))
                        && Entity.class.isAssignableFrom(Class.forName(target.getTypeName()))) {
                    log.info("Needs unwrapping: Source {}, Target {}", source, target);
                    MethodDescription getOriginMethod = new TypeDescription.ForLoadedType(BaseEntityViewImpl.class)
                            .getDeclaredMethods()
                            .filter(named("getOrigin"))
                            .getOnly();
                    return MethodInvocation.
                            invoke(getOriginMethod)
                            .dynamic(getOriginMethod.getName(), target.asErasure(), Collections.emptyList(), Collections.emptyList());
                }
                if (BaseEntityView.class.isAssignableFrom(Class.forName(target.getTypeName()))
                        && Entity.class.isAssignableFrom(Class.forName(source.getTypeName()))) {
                    log.info("Needs wrapping: Source {}, Target {}", source, target);
                    return StackManipulation.Trivial.INSTANCE;
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot create proper assignment", e);
            }
            return StackManipulation.Trivial.INSTANCE;
        }
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
