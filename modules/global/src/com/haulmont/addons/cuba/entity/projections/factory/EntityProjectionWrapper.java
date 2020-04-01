package com.haulmont.addons.cuba.entity.projections.factory;


import com.haulmont.addons.cuba.entity.projections.Projection;
import com.haulmont.addons.cuba.entity.projections.ProjectionImpl;
import com.haulmont.addons.cuba.entity.projections.scan.ProjectionsConfiguration;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class that "wraps" entity into projection by creating a proxy class that implements projection interface
 * contract. Please note that class uses application context despite on the fact that all methods are static.
 */
public class EntityProjectionWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityProjectionWrapper.class);

    /**
     * Wraps entity instance into projection.
     *
     * @param entity        Entity instance to be wrapped.
     * @param projectionInterface Projection class.
     * @param <E>           Entity Class
     * @param <V>           Effective projection interface class.
     * @param <K>           Entity ID key class.
     * @return Proxy that implements projection interface of class <code>V</code>
     */
    @SuppressWarnings("unchecked")
    public static <E extends Entity<K>, V extends Projection<E, K>, K> V wrap(E entity, Class<V> projectionInterface) {
        if (entity == null) {
            return null;
        }
        log.trace("Wrapping entity: {} to projection: {}", entity, projectionInterface);
        Class<? extends Projection> effectiveProjection = AppBeans.get(ProjectionsConfiguration.class).getEffectiveProjection(projectionInterface);
        log.trace("Effective projection: {}", effectiveProjection);

        try {
            Class<?> aClass = getProjectionClass(effectiveProjection);
            Constructor<?> constructor = aClass.getConstructors()[0];
            return (V) constructor.newInstance(entity, effectiveProjection);
        } catch (Exception e) {
            throw new RuntimeException("Cannot wrap entity " + entity + "into View " + effectiveProjection.getName(), e);
        }
    }

    public static Class<?> getProjectionClass(Class<? extends Projection> effectiveProjection) throws NotFoundException, CannotCompileException, IOException {
        if (effectiveProjection == null || !Projection.class.isAssignableFrom(effectiveProjection)) {
            throw new IllegalArgumentException(String.format("Projection interface must not be null and should implement %s", Projection.class.getName()));
        }
        String wrapperName = createWrapperClassName(effectiveProjection);
        Class<?> aClass = null;
        try {
            aClass = Class.forName(wrapperName);
        } catch (ClassNotFoundException e) {
            log.debug("Implementation class for projection " + effectiveProjection.getName() + " is not created. Trying to create it.");
        }
        if (aClass == null) {
            aClass = createWrapperImplementation(effectiveProjection, wrapperName);
        }
        return aClass;
    }

    private static String createWrapperClassName(Class<?> effectiveView) {
        return String.format("%sWrapperImpl", effectiveView.getName());
    }

    private static <E extends Entity<K>, V extends Projection<E, K>, K> Class<?> createWrapperImplementation(Class<V> viewInterface, String wrapperName) throws NotFoundException, CannotCompileException, IOException {
        log.info("Creating dynamic implementation {} for {}", wrapperName, viewInterface);
        ClassPool pool = ClassPool.getDefault();
        CtClass baseClass = pool.get(ProjectionImpl.class.getName());
        CtClass wrappingEntityClass = pool.get(getProjectionEntityClassName(viewInterface));
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

        List<Method> entityViewMethods = getEntityProjectionMethods(viewInterface);

        entityViewMethods.forEach(m -> {
            try {
                wrapperClass.addMethod(createDelegateMethod(wrapperClass, m, pool, wrappingEntityClass));
            } catch (NotFoundException | CannotCompileException e) {
                throw new IllegalArgumentException("Cannot add method " + m.getName() + " to wrapper class " + wrapperName, e);
            }
        });
        wrapperClass.writeFile();
        Class<?> aClass = wrapperClass.toClass();
        return aClass;
    }

    private static <E extends Entity<K>, V extends Projection<E, K>, K> List<Method> getEntityProjectionMethods(Class<V> viewInterface) {
        return Arrays.stream(viewInterface.getMethods())
                .filter(method -> MethodUtils.getMatchingMethod(ProjectionImpl.class, method.getName(), method.getParameterTypes()) == null
                        && !method.isDefault())
                .collect(Collectors.toList());
    }

    private static CtMethod createDelegateMethod(CtClass wrapper, Method m, ClassPool pool, CtClass wrappingEntityClass) throws NotFoundException, CannotCompileException {
        Class<?>[] parameterTypes = m.getParameterTypes();

        List<String> paramDelegatesList = IntStream.range(0, parameterTypes.length)
                .mapToObj(i -> createParameterDelegateString(i + 1, parameterTypes[i]))
                .collect(Collectors.toList());

        String paramDelegatesInvoke = String.join(",", paramDelegatesList);

        String methodDelegateInvocation = "instance."+m.getName()+"("+paramDelegatesInvoke+")";

        Class<?> returnType = m.getReturnType();
        String returnTypeName = returnType.getName();
        String body = appendResultReturnCode(m, returnType, returnTypeName, methodDelegateInvocation);

        CtClass[] paramTypes = pool.get(Arrays.stream(parameterTypes)
                .map(Class::getName).toArray(String[]::new));

        CtClass[] exceptionTypes = pool.get(Arrays.stream(m.getExceptionTypes())
                .map(Class::getName).toArray(String[]::new));

        return CtNewMethod.make(m.getModifiers(),
                pool.get(returnTypeName),
                m.getName(),
                paramTypes,
                exceptionTypes,
                "{doReload();\n"
                        + wrappingEntityClass.getName()+ " instance = getOrigin();"
                        + body
                        + "}",
                wrapper);
    }

    private static String createParameterDelegateString(int parameterNum, Class<?> parameterType) {
        if (Projection.class.isAssignableFrom(parameterType)) {
            String paramTypeName = getProjectionEntityClassName((Class<? extends Projection>)parameterType);
            return "("+paramTypeName+")($"+parameterNum+".getOrigin())";
        } else {
            return "$"+parameterNum;
        }
    }

    private static String appendResultReturnCode(Method m, Class<?> returnType, String returnTypeName, String body) {
        if (Collection.class.isAssignableFrom(returnType)) {
            Class<?> collectionGenericType = getMethodReturnType(m);
            return "\nreturn new "+ WrappingList.class.getName()+"("+body+", " + collectionGenericType.getName() + ".class);";
        } else if (Projection.class.isAssignableFrom(returnType)) {
            return "\nreturn " + EntityProjectionWrapper.class.getName() + ".wrap("+body+", " + returnTypeName + ".class);";
        } else if (!returnType.equals(Void.TYPE)) {
            return "\nreturn "+body+";";
        } else {
            return body+";";
        }
    }

    private static String getProjectionEntityClassName(Class<?> effectiveProjection) {

        List<Class<?>> implementedInterfaces = ClassUtils.getAllInterfaces(effectiveProjection);
        implementedInterfaces.add(effectiveProjection);

        for (Class<?> intf : implementedInterfaces) {
            Set<ParameterizedType> candidateTypes = Arrays.stream(intf.getGenericInterfaces())
                    .filter(type -> type instanceof ParameterizedType)
                    .map(type -> ((ParameterizedType) type))
                    .filter(parameterizedType -> Projection.class.getName().equals(parameterizedType.getRawType().getTypeName()))
                    .collect(Collectors.toSet());

            if (candidateTypes.size() == 1) {
                ParameterizedType baseProjectionIntf = candidateTypes.iterator().next();
                Type entityType = Arrays.asList(baseProjectionIntf.getActualTypeArguments()).get(0);
                return entityType.getTypeName();
            }
        }
        throw new IllegalArgumentException("Cannot get generic entity type parameter for projection "+effectiveProjection.getName());
    }


    /**
     * Returns actual method return type or collection parameter type for one-to-many
     * relation attributes. Used for building CUBA views based on entity views.
     *
     * @param viewMethod method to be used in CUBA view.
     * @return type that will be used in CUBA view.
     */
    public static Class<?> getMethodReturnType(Method viewMethod) {
        Class<?> returnType = viewMethod.getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            Type genericReturnType = viewMethod.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericReturnType;
                List<Class<?>> collectionTypes = Arrays.stream(type.getActualTypeArguments())
                        .map(t -> ReflectionHelper.getClass(t.getTypeName())).collect(Collectors.toList());
                //TODO make this code a bit more accurate
                if (collectionTypes.stream().anyMatch(Projection.class::isAssignableFrom)) {
                    return collectionTypes.stream().filter(Projection.class::isAssignableFrom).findFirst().orElseThrow(RuntimeException::new);
                } else {
                    return collectionTypes.stream().findFirst().orElseThrow(RuntimeException::new);
                }
            }
        }
        log.trace("Method {} return type {}", viewMethod.getName(), returnType);
        return returnType;
    }

}
