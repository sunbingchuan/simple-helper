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
package com.chuan.simple.helper.clazz;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.method.MethodHelper;

/**
 * Tools for class.
 */
public final class ClassHelper {

    private static final Log LOG = LogFactory.getLog(ClassHelper.class);

    /** Suffix for array class names: {@code "[]"}. */
    public static final String ARRAY_SUFFIX = "[]";

    /** Prefix for internal array class names: {@code "["}. */
    private static final String INTERNAL_ARRAY_PREFIX = "[";

    /** Prefix for internal non-primitive array class names: {@code "[L"}. */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

    /** The package separator character: {@code '.'}. */
    private static final char PACKAGE_SEPARATOR = '.';

    /** The inner class separator character: {@code '$'}. */
    private static final char INNER_CLASS_SEPARATOR = '$';

    /** The ".class" file suffix. */
    public static final String CLASS_FILE_SUFFIX = ".class";

    private static final Map<Class<?>, Class<?>> wrapperToPrimitiveTypeMap =
            new IdentityHashMap<>();

    private static final Map<Class<?>, Class<?>> primitiveToWrapperTypeMap =
            new IdentityHashMap<>();

    public static final Map<String, Class<?>> primitiveTypeNameMap =
            new HashMap<>();

    public static final Map<String, Class<?>> classCache = new HashMap<>();

    private static final Map<String, Integer> modifierFlags = new HashMap<>();

    private ClassHelper() {
    }

    /**
     * Transform string modifier flag to integer flag.
     * @see Modifier#toString(int)
     */
    public static Integer getModifier(String flag) {
        return modifierFlags.get(flag);
    }

    static {
        initModifierFlags();
        initPrimitiveMap();
    }

    private static void initModifierFlags() {
        int flag = 1;
        while (flag <= Modifier.STRICT) {
            String flagName = Modifier.toString(flag);
            modifierFlags.put(flagName, flag);
            flag <<= 1;
        }
        modifierFlags.put("synthetic",0x1000 /* Modifier.SYNTHETIC */);
    }

    private static void initPrimitiveMap() {
        wrapperToPrimitiveTypeMap.put(Boolean.class, boolean.class);
        wrapperToPrimitiveTypeMap.put(Byte.class, byte.class);
        wrapperToPrimitiveTypeMap.put(Character.class, char.class);
        wrapperToPrimitiveTypeMap.put(Double.class, double.class);
        wrapperToPrimitiveTypeMap.put(Float.class, float.class);
        wrapperToPrimitiveTypeMap.put(Integer.class, int.class);
        wrapperToPrimitiveTypeMap.put(Long.class, long.class);
        wrapperToPrimitiveTypeMap.put(Short.class, short.class);
        for (Map.Entry<Class<?>, Class<?>> entry : wrapperToPrimitiveTypeMap
                .entrySet()) {
            primitiveToWrapperTypeMap.put(entry.getValue(), entry.getKey());
        }
        Set<Class<?>> primitiveTypes = new HashSet<>();
        primitiveTypes.addAll(wrapperToPrimitiveTypeMap.values());
        Collections.addAll(primitiveTypes, boolean[].class, byte[].class,
                char[].class, double[].class, float[].class, int[].class,
                long[].class, short[].class, void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }
    }

    /**
     * Transform the primitive type to wrapper type if necessary.
     */
    public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
        return clazz.isPrimitive() && clazz != void.class
                ? primitiveToWrapperTypeMap.get(clazz)
                : clazz;
    }

    /**
     * @see ClassHelper#forName(String, ClassLoader)
     */
    public static Class<?> forName(String name) {
        return forName(name, getDefaultClassLoader());
    }

    /**
     * @see ClassHelper#forName(String, ClassLoader)
     */
    public static Class<?>[] forName(String... names) {
        Class<?>[] classes = new Class<?>[names.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = forName(names[i]);
        }
        return classes;
    }

    /**
     * Load class by class name using the specified {@link ClassLoader}.
     */
    public static Class<?> forName(String name, ClassLoader classLoader) {
        Class<?> clazz = null;
        if(StringHelper.isEmpty(name)){
        	return null;
        }
        if (name.length() <= 8) {
            clazz = primitiveTypeNameMap.get(name);
        }
        if (clazz == null) {
            clazz = classCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }
        clazz = arrayClass(name, classLoader);
        if (clazz==null) {
            clazz = loadClass(name, classLoader);
        }
        if (clazz==null) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String innerClassName = name.substring(0, lastDotIndex)
                        + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                clazz = loadClass(innerClassName, classLoader);
            }
        }
        if (clazz!=null) {
            classCache.put(name, clazz);
        }
        return clazz;
    }

    private static Class<?> arrayClass(String name, ClassLoader classLoader) {
        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName =
                    name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String eleName = name.substring(
                    NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(eleName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String eleName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(eleName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }
        return null;
    }

    private static Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return classLoader != null ? classLoader.loadClass(name)
                    : Class.forName(name);
        } catch (ClassNotFoundException e) {
            LOG.error("Load class '" + name + "' failed", e);
            return null;
        }
    }
    
    /**
     * Get default {@link ClassLoader}.
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Exception e) {
            LOG.debug("Get current thread context classLoader failed", e);
            try {
                cl = ClassHelper.class.getClassLoader();
            } catch (Exception e1) {
                LOG.debug("Get class ClassHelper's classLoader failed", e1);
                    try {
                        cl = ClassLoader.getSystemClassLoader();
                    } catch (Exception e2) {
                        LOG.debug("Get system classLoader failed", e2);
                    }
            }
        }
        return cl;
    }

    private static Field loadedClass;

    /**
     * Get class loaded.
     */
    public static List<Class<?>> getLoadedClass() {
        try {
            if (loadedClass == null) {
            	synchronized (ClassHelper.class) {
            		if (loadedClass == null) {
            			loadedClass = ClassLoader.class.getDeclaredField("classes");
                        loadedClass.setAccessible(true);
            		}
				}
            }
            @SuppressWarnings("unchecked")
            List<Class<?>> classes = (List<Class<?>>) loadedClass
                    .get(ClassLoader.getSystemClassLoader());
            return classes;
        } catch (Exception e) {
            LOG.debug("Get loaded classes failed", e);
            return null;
        }
    }

    /**
     * Declare  the {@code classes} by parameters so which will be loaded.
     */
    public static void declareClass(Class<?>... classes) {
    }

    /**
     * Get input stream of class file.
     */
    public static InputStream getClassInputStream(Class<?> clazz) {
        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        String classFileName =
                className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
        return clazz.getResourceAsStream(classFileName);
    }

    public static Class<?> getCurrentClass() {
        return getCurrentClass(ClassHelper.getDefaultClassLoader());
    }

    public static Class<?> getCurrentClass(ClassLoader classLoader) {
        StackTraceElement stackTraceElement =
                MethodHelper.getStackTraceElement(2);
        return forName(stackTraceElement.getClassName(),classLoader);
    }

    
    public static void clear() {
        classCache.clear();
    }
}
