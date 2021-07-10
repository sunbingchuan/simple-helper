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
package com.chuan.simple.helper.generic;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chuan.simple.helper.common.ObjectHelper;

/**
 * Tools for generic param type.
 */
public final class GenericTypeHelper {

    private static final Map<Object, Set<GenericType>> genericTypeCache =
            new HashMap<>();

    private GenericTypeHelper() {
    }

    /**
     * Find the generic type of {@code source} declared by {@code declare}.
     * @param source
     *            the target class
     * @param declare
     *            the class declared the generic param
     * @param index
     *            the generic params's index
     * @return GenericType
     * @see GenericType
     */
    public static GenericType getGenericType(Class<?> source, Class<?> declare,
            int index) {
        Set<GenericType> genericTypes = getGenericTypes(source);
        return getGenericType(genericTypes, declare, index);
    }

    /**
     * @see #getGenericTypes(Class)
     * @param source
     *            can be {@link Field} or {@link Parameter}
     */
    public static GenericType getGenericType(Object source, Class<?> declare,
            int index) {
        Set<GenericType> genericTypes = getGenericTypes(source);
        return getGenericType(genericTypes, declare, index);
    }

    /**
     * Search the right GenericType from a set of GenericTypes.
     * @param genericTypes
     *            a set of {@link GenericType}s
     * @param declare
     *            the class declared the generic param
     * @param index
     *            the generic params's index
     * @return
     */
    public static GenericType getGenericType(Set<GenericType> genericTypes,
            Class<?> declare, int index) {
    	if (ObjectHelper.isNotEmpty(genericTypes))
        for (GenericType genericType : genericTypes) {
            if (genericType.math(declare, index)) {
                return genericType;
            }
        }
        return null;
    }

    /**
     * @see #getGenericType(Class, Class, int)
     */
    public static Class<?> getGenericClass(Class<?> source, Class<?> declare,
            int index) {
        GenericType genericType = getGenericType(source, declare, index);
        if (genericType != null) {
            return genericType.getActualType();
        }
        return null;
    }

    /**
     * @see #getGenericType(Object, Class, int)
     */
    public static Class<?> getGenericClass(Object source, Class<?> declare,
            int index) {
        GenericType genericType = getGenericType(source, declare, index);
        if (genericType != null) {
            return genericType.getActualType();
        }
        return null;
    }

    /**
     * Get all the generic params of class {@code source} declared by class
     * {@code declare}
     */
    public static List<Class<?>> getGenericClasses(Class<?> source,
            Class<?> declare) {
        Set<GenericType> genericTypes = getGenericTypes(source);
        return getGenericClasses(genericTypes, declare);
    }

    /**
     * Get all the generic params of {@link Field}/{@link Parameter}
     * {@code source} declared by class {@code declare}
     */
    public static List<Class<?>> getGenericClasses(Object source,
            Class<?> declare) {
        Set<GenericType> genericTypes = getGenericTypes(source);
        return getGenericClasses(genericTypes, declare);
    }

    private static List<Class<?>> getGenericClasses(
            Set<GenericType> genericTypes, Class<?> declare) {
        Type[] params = declare.getTypeParameters();
        List<Class<?>> list = new ArrayList<>(params.length);
        if (ObjectHelper.isNotEmpty(genericTypes)) 
        for (int i = 0; i < params.length; i++) {
            for (GenericType genericType : genericTypes) {
                if (genericType.math(declare, i)) {
                    list.add(genericType.getActualType());
                }
            }
        }
        return list;
    }

    /**
     * Get all the {@link GenericType}s of the class source.
     */
    public static Set<GenericType> getGenericTypes(Class<?> source) {
        Set<GenericType> genericTypes = genericTypeCache.get(source);
        if (genericTypes == null) {
            genericTypes = new HashSet<>();
            walkClass(source, genericTypes);
            genericTypeCache.put(source, genericTypes);
        }
        return genericTypes;
    }

    /**
     * @see #getGenericTypes(Class)
     * @param source
     *            can be {@link Field} or {@link Parameter}
     */
    public static Set<GenericType> getGenericTypes(Object source) {
        if (source == null) {
            return null;
        }
        Set<GenericType> genericTypes = genericTypeCache.get(source);
        if (genericTypes == null) {
            genericTypes = new HashSet<>();
            if (source != null) {
                if (source instanceof Field) {
                    Field f = (Field) source;
                    Type ftype = f.getGenericType();
                    if (ftype instanceof ParameterizedType) {
                        dealParameterizedType((ParameterizedType) ftype,
                                f.getType(), genericTypes);
                    }
                    genericTypes.addAll(getGenericTypes(f.getType()));
                } else if (source instanceof Parameter) {
                    Parameter p = (Parameter) source;
                    Type ptype = p.getParameterizedType();
                    if (ptype instanceof ParameterizedType) {
                        dealParameterizedType((ParameterizedType) ptype,
                                p.getType(), genericTypes);
                    }
                    genericTypes.addAll(getGenericTypes(p.getType()));
                }
            }
        }
        return genericTypes;
    }

    private static void walkClass(Class<?> source,
            Set<GenericType> genericTypes) {
        if (source == null || source.equals(Object.class)) {
            return;
        }
        Type[] interfaces = source.getGenericInterfaces();
        Type superClass = source.getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) superClass;
            handleParameterizedType(genericTypes, pt);
        }
        for (Type type : interfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                handleParameterizedType(genericTypes, pt);
            }
        }
        walkClass(source.getSuperclass(), genericTypes);
        for (Class<?> clazz : source.getInterfaces()) {
            walkClass(clazz, genericTypes);
        }

    }

    private static void handleParameterizedType(Set<GenericType> genericTypes,
            ParameterizedType pt) {
        Type rawType = pt.getRawType();
        int index = 0;
        for (Type type : pt.getActualTypeArguments()) {
            if (!(rawType instanceof Class)) {
                continue;
            }
            if (type instanceof Class) {
                GenericType genericType = new GenericType((Class<?>) type);
                genericTypes.add(genericType);
                findCorrespondParams((Class<?>) rawType, index, genericType);
            } else if (type instanceof ParameterizedType) {
                Type rt = ((ParameterizedType) type).getRawType();
                if (rt instanceof Class<?>) {
                    GenericType genericType = new GenericType((Class<?>) rt);
                    genericType.setParameterizedType((ParameterizedType) type);
                    genericTypes.add(genericType);
                    findCorrespondParams((Class<?>) rawType, index,
                            genericType);
                }
            }
            index++;
        }
    }

    private static void findCorrespondParams(Class<?> clazz, int index,
            GenericType genericType) {
        genericType.addCorresponds(new GenericParameter(clazz, index));
        Type param = clazz.getTypeParameters()[index];
        if (genericType.getType() == null) {
            genericType.setType(param);
        }
        Type[] interfaces = clazz.getGenericInterfaces();
        Type superClass = clazz.getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            dealParameterizedType(param, (ParameterizedType) superClass,
                    genericType);
        }
        for (Type itfe : interfaces) {
            if (itfe instanceof ParameterizedType) {
                dealParameterizedType(param, (ParameterizedType) itfe,
                        genericType);
            }
        }
    }

    private static void dealParameterizedType(ParameterizedType ptype,
            Class<?> clazz, Set<GenericType> genericTypes) {
        Type[] types = ptype.getActualTypeArguments();
        for (int j = 0; j < types.length; j++) {
            Type type = types[j];
            if (type instanceof Class<?>) {
                GenericType genericType = new GenericType((Class<?>) type);
                genericTypes.add(genericType);
                findCorrespondParams(clazz, j, genericType);
            }
        }
    }
    
    private static void dealParameterizedType(Type param,
            ParameterizedType ptype, GenericType genericType) {
        Type rawType = ptype.getRawType();
        int i = 0;
        for (Type type : ptype.getActualTypeArguments()) {
            if (type.equals(param) && rawType instanceof Class<?>) {
                findCorrespondParams((Class<?>) rawType, i, genericType);
            }
            i++;
        }
    }
    
    public static void clear() {
        genericTypeCache.clear();
    }
    
}
