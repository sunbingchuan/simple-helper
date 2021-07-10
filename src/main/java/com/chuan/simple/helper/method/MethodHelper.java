/*
 * Copyright 2018-2021 Bingchuan Sun.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chuan.simple.helper.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chuan.simple.helper.clazz.ClassHelper;
import com.chuan.simple.helper.common.ObjectHelper;
import com.chuan.simple.helper.generic.GenericType;
import com.chuan.simple.helper.generic.GenericTypeHelper;

/**
 * Tools for {@link Method}.
 */
public final class MethodHelper {

    private static final Log LOG = LogFactory.getLog(MethodHelper.class);

    public static final String INIT_METHOD = "<init>";

    private static final Map<Method, Method> bridgeCache =
            new HashMap<Method, Method>();

    private static final Map<String, Executable> currentMethodCache =
            new HashMap<>();

    private static final Map<Class<?>, Collection<Constructor<?>>> constructorCache =
            new HashMap<>();

    private static final Map<Class<?>, ClassMethodEntry> methodCache =
            new HashMap<>();
    
    private static Method getStackTraceElement;
    static {
        try {
            getStackTraceElement = Throwable.class
                    .getDeclaredMethod("getStackTraceElement", int.class);
            getStackTraceElement.setAccessible(true);
        } catch (Exception e) {
            LOG.error("MethodHelper initialize failed", e);
        }
    }

    private MethodHelper() {
    }

    /**
     * Get the method who invoke {@link #getCurrentMethod(Object...)}.
     * @param params
     *            the parameters of the method who invoke
     *            {@link #getCurrentMethod(Object...)}.
     */
    public static Executable getCurrentMethod(Object... params) {
        StackTraceElement stack = getStackTraceElement(2);
        String methodName = stack.getMethodName();
        Class<?> clazz = ClassHelper.forName(stack.getClassName(), null);
        Class<?>[] paramTypes = null;
        if (params != null) {
            paramTypes = ParameterHelper.getParameterTypes(params);
        }
        String key = generateKey(clazz, methodName, paramTypes);
        Executable result  = currentMethodCache.get(key);
        if (result != null) {
            return result;
        }
        if (INIT_METHOD.equals(methodName)) {
            result = findConstructor(clazz, paramTypes);
        } else {
            result = findMethod(clazz, methodName, paramTypes);
        }
        currentMethodCache.put(key, result);
        return result;
    }

    /**
     * Get the specific {@link StackTraceElement} of current stack trace.
     * @param depth
     *            the depth of the specific {@link StackTraceElement}
     */
    public static StackTraceElement getStackTraceElement(int depth) {
        try {
            return (StackTraceElement) getStackTraceElement
                    .invoke(new Throwable(), depth);
        } catch (Exception e) {
            return Thread.currentThread().getStackTrace()[depth];
        }
    }

    private static String generateKey(Class<?> owner,
            String methodName, Class<?>... paramTypes) {
        StringBuffer sb = new StringBuffer();
        sb.append(owner.getName());
        sb.append(methodName);
        if (paramTypes!=null) 
        for (Class<?> clazz : paramTypes) {
            sb.append(clazz.getName());
        }
        return sb.toString();
    }

    /**
     * Get the constructor of class {@code clazz} with parameter type
     * {@code paramTypes}.
     */
    public static Constructor<?> findConstructor(Class<?> clazz,
            Class<?>... paramTypes) {
        Constructor<?> result = null;
        for (Constructor<?> constructor : getConstructors(clazz)) {
            if (ParameterHelper.paramsFit(paramTypes,
                    constructor.getParameterTypes())) {
                if (result != null) {
                    bestConstructor(result, constructor);
                } else {
                    result = constructor;
                }
            }
        }
        return result;
    }

    /**
     * @see #findMethod(Class, String, boolean, Class[])
     */
    public static Method findMethod(Class<?> clazz, String name,
            Class<?>... paramTypes) {
        return findMethod(clazz, name, true, paramTypes);
    }

    /**
     * Get the method {@code name} of class {@code clazz} with parameter type
     * {@code paramTypes}.
     * @param inherited
     *            include inherited method.
     */
    public static Method findMethod(Class<?> clazz, String name,
            boolean inherited, Class<?>... paramTypes) {
        Method result = null;
        for (Method method : getMethods(clazz, inherited)) {
            if (name.equals(method.getName()) && (ParameterHelper
                    .paramsFit(paramTypes, method.getParameterTypes()))) {
                if (result != null) {
                    result = bestMethod(result, method);
                } else {
                    result = method;
                }
            }
        }
        if (result == null) {
            LOG.debug("Couldn't find method " + name + " with parameter types "
                    + ObjectHelper.toString(paramTypes));
        }
        return result;
    }
    /**
     * Get the constructor of class {@code clazz}.
     */
    public static Collection<Constructor<?>> getConstructors(Class<?> clazz) {
        Collection<Constructor<?>> constructors = constructorCache.get(clazz);
        if (constructors==null) {
            constructors = Arrays.asList(clazz.getDeclaredConstructors());
            constructors = Collections.unmodifiableCollection(constructors);
            constructorCache.put(clazz, constructors);
        }
        return constructors;
    }

    
    /**
     * Get the methods of class {@code clazz}.
     */
    public static Collection<Method> getMethods(Class<?> clazz) {
        return getMethods(clazz, true);
    }

    /**
     * Get all the methods of class.
     * @param inherited
     *            include inherited method.
     */
    public static Collection<Method> getMethods(Class<?> clazz,
            boolean inherited) {
        ClassMethodEntry classMethodEntry = methodCache.get(clazz);
        if (classMethodEntry==null) {
            classMethodEntry=new ClassMethodEntry(clazz);
            methodCache.put(clazz, classMethodEntry);
        }
        return classMethodEntry.getMethods(inherited);
    }

    private static Method bestMethod(Method a, Method b) {
        Method best = a;
        if (((a.getModifiers() ^ b.getModifiers()) & Modifier.ABSTRACT) > 0) {
            if (Modifier.isAbstract(a.getModifiers())) {
                best = b;
            }
        }
        if (((a.getModifiers() ^ b.getModifiers()) & Modifier.PUBLIC) > 0) {
            if (Modifier.isPublic(b.getModifiers())) {
                best = b;
            }
        }
        if (!ParameterHelper.paramsEqual(b.getParameterTypes(),
                a.getParameterTypes())
                && ParameterHelper.paramsFit(b.getParameterTypes(),
                        a.getParameterTypes())) {
            best = b;
        }
        return best;
    }

    private static Constructor<?> bestConstructor(Constructor<?> a,
            Constructor<?> b) {
        Constructor<?> best = a;
        if (((a.getModifiers() ^ b.getModifiers()) & Modifier.PUBLIC) > 0) {
            if (Modifier.isPublic(b.getModifiers())) {
                best = b;
            }
        }
        if (ParameterHelper.paramsFit(b.getParameterTypes(),
                a.getParameterTypes())) {
            best = b;
        }
        return best;
    }

    /**
     * Find the original method of bridge method(if so).
     * @param bridgeMethod
     */
    public static Method findOriginalMethod(Method bridgeMethod) {
        if (!bridgeMethod.isBridge()) {
            return bridgeMethod;
        }
        Method originMethod = bridgeCache.get(bridgeMethod);
        if (originMethod != null) {
            return originMethod;
        }
        Class<?> owner = bridgeMethod.getDeclaringClass();
        Method declaration = findGenericDeclaration(owner, bridgeMethod);
        Set<GenericType> genericTypes =
                GenericTypeHelper.getGenericTypes(owner);
        Type[] genericParameterTypes = declaration.getGenericParameterTypes();
        Class<?>[] parameterTypes = bridgeMethod.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            if (!(type instanceof Class<?>)) {
                Class<?> actualParameterType =
                        getActualParameterType(genericTypes, type);
                if (actualParameterType != null) {
                    parameterTypes[i] = actualParameterType;
                }
            }
        }
        originMethod = searchMatchMethod(owner, bridgeMethod.getName(),
                parameterTypes);
        if (originMethod != null) {
            bridgeCache.put(bridgeMethod, originMethod);
        }
        return originMethod;
    }

    private static Class<?> getActualParameterType(
            Set<GenericType> genericTypes,
            Type genericParameterType) {
        for (GenericType genericType : genericTypes) {
            if (genericType.getType().equals(genericParameterType)) {
                return genericType.getActualType();
            }
        }
        return null;
    }

    private static Method findGenericDeclaration(Class<?> clazz,
            final Method bridgeMethod) {
        Method method = null;
        if ((clazz = clazz.getSuperclass()) != null && clazz != Object.class) {
            method = searchMatchMethod(clazz, bridgeMethod.getName(),
                    bridgeMethod.getParameterTypes());
            if (method != null && !method.isBridge()) {
                return method;
            }
        }
        if (clazz==null) {
			return null;
		}
        method = findGenericDeclaration(clazz, bridgeMethod);
        if (method != null) {
            return method;
        }
        for (Class<?> itfe : clazz.getInterfaces()) {
            method = findGenericDeclaration(itfe, bridgeMethod);
            if (method != null) {
                return method;
            }
        }
        return method;
    }

    private static Method searchMatchMethod(Class<?> type,
            String name, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            LOG.debug("Couldn't not find method '" + name + "'", ex);
            return null;
        }
    }

    /**
     * Find the method of class {@code clazz} which is the same as method
     * {@code origin}(maybe override).
     */
    public static Method findSameMethod(Method origin,
            Class<?> clazz) {
        for (Method method : getMethods(clazz,false)) {
            if (!method.getName().equals(origin.getName())) {
                continue;
            }
            if (method.getParameterCount() != origin.getParameterCount()) {
                continue;
            }
            if (ParameterHelper.paramsEqual(origin.getParameterTypes(),
                    method.getParameterTypes())) {
                return method;
            }
        }
        return null;
    }

    public static Object invoke(Object owner, String methodName,
            Object... params) {
        if (owner == null) {
            return null;
        }
        Class<?>[] parameterTypes = ParameterHelper.getParameterTypes(params);
        Method method =
                findMethod(owner.getClass(), methodName, parameterTypes);
        if (method == null) {
            LOG.error("Couldn't find method '" + methodName
                    + "' which match parameters '"
                    + ObjectHelper.toString(params) + "'");
            return null;
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        Object result = null;
        try {
            result = method.invoke(owner, params);
        } catch (Exception e) {
            LOG.error("Invoke method '" + methodName + "' with parameters '"
                    + ObjectHelper.toString(params) + "' failed", e);
        }
        return result;
    }

    public static Object invoke(Object owner, Method method,
            Object... params) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return method.invoke(owner, params);
        } catch (Exception e) {
            LOG.error("Invoke " + method + " failed", e);
            return null;
        }
    }

    public static Object invoke(Constructor<?> constructor,
            final Object... params) {
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        try {
            return constructor.newInstance(params);
        } catch (Exception e) {
            LOG.error("Invoke " + constructor + " failed", e);
            return null;
        }
    }

    private static class ClassMethodEntry{
        
        private Collection<Method> methods;
        
        private Collection<Method> allMethods;
 
        private Collection<Method> getMethods(boolean inherited){
            if (inherited) {
                return allMethods;
            }
            return methods;
        }
        
        private ClassMethodEntry(Class<?> clazz) {
            methods = new HashSet<>();
            allMethods = new HashSet<>();
            for (Method method : clazz.getDeclaredMethods()) {
                methods.add(method);
                allMethods.add(method);
            }
            for (Method method : clazz.getMethods()) {
                allMethods.add(method);
            }
            for (clazz = clazz.getSuperclass(); clazz != null; 
                    clazz =clazz.getSuperclass()) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!Modifier.isPrivate(method.getModifiers())) {
                        allMethods.add(method);
                    }
                }
            }
            methods= Collections.unmodifiableCollection(methods);
            allMethods= Collections.unmodifiableCollection(allMethods);
        }
        
    }
    
    public static void clear() {
        bridgeCache.clear();
        constructorCache.clear();
        currentMethodCache.clear();
        methodCache.clear();
    }
    
}
