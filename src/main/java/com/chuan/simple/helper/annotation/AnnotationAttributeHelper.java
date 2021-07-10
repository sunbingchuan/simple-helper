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
package com.chuan.simple.helper.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chuan.simple.annotation.SameAs;
import com.chuan.simple.helper.common.ObjectHelper;
import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.method.MethodHelper;

/**
 * Tools for resolving Annotation attributes.
 */
@SuppressWarnings("unchecked")
public final class AnnotationAttributeHelper {


    private static final Log log = LogFactory.getLog(AnnotationAttributeHelper.class);

    private static final Map<Class<? extends Annotation>, List<Method>> attributeMethodsCache =
            new ConcurrentHashMap<>();

    private static final Map<Method, Map<Class<? extends Annotation>, Set<String>>> aliasCache =
            new ConcurrentHashMap<>();

    private static final Map<AnnotatedElement, Map<Class<? extends Annotation>, AnnotationAttribute>> annotationMapCache =
            new ConcurrentHashMap<>();

    private AnnotationAttributeHelper() {
    }
    
    /**
     * Get all the annotation attributes of the {@link AnnotatedElement}. This
     * method will traverse all the super classes and interfaces or the
     * overridden methods (if the {@code annotatedElement} is a method)
     * @param annotatedElement
     *            an {@link AnnotatedElement}
     * @return a {@link Map} contains {@link AnnotationAttribute} mapped to the
     *         key of {@link Annotation} class
     * @see AnnotationAttribute
     */
    public static Map<Class<? extends Annotation>, AnnotationAttribute> from(
            AnnotatedElement annotatedElement) {
        Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes =
                annotationMapCache.get(annotatedElement);
        if (annotationAttributes == null) {
            annotationAttributes =
                    new HashMap<>();
            from(annotatedElement, annotationAttributes, new HashSet<>());
            annotationMapCache.put(annotatedElement, annotationAttributes);
        }
        return annotationAttributes;
    }

    private static void from(AnnotatedElement annotatedElement,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes,
            Set<Annotation> visited) {
        for (Annotation anno : AnnotationHelper
                .getDeclaredAnnotations(annotatedElement)) {
            if (visited.add(anno)
                    && !AnnotationHelper.isInJavaLangAnnotationPackage(
                            anno.annotationType().getName())) {
                fromAnnotation(anno, annotationAttributes, visited);
            }
        }
        if (annotatedElement instanceof Method) {
            Method method = (Method) annotatedElement;
            fromMethod(method, null, annotationAttributes, visited);
        }
        if (annotatedElement instanceof Class) {
            fromClass((Class<?>) annotatedElement, annotationAttributes,
                    visited);
        }
    }

    private static void fromClass(Class<?> clazz,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes,
            Set<Annotation> visited) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            from(superclass, annotationAttributes, visited);
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            from(ifc, annotationAttributes, visited);
        }
    }

    private static void fromMethod(Method method, Class<?> clazz,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes,
            Set<Annotation> visited) {
        Method resolvedMethod = MethodHelper.findOriginalMethod(method);
        if (clazz == null) {
            clazz = resolvedMethod.getDeclaringClass();
        }
        clazz = clazz.getSuperclass();
        if (clazz == null || clazz == Object.class) {
            return;
        }
        Method m = MethodHelper.findSameMethod(resolvedMethod, clazz);
        if (m != null) {
            from(m, annotationAttributes, visited);
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            Method mm = MethodHelper.findSameMethod(resolvedMethod, ifc);
            if (mm != null) {
                from(mm, annotationAttributes, visited);
            }
        }
    }

    private static void fromAnnotation(Annotation anno,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes,
            Set<Annotation> visited) {
        Class<?> clazz = anno.annotationType();
        fromAnnotationMethod(anno, annotationAttributes);
        from(clazz, annotationAttributes, visited);
    }

    private static void fromAnnotationMethod(Annotation anno,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes) {
        Class<? extends Annotation> clazz = anno.annotationType();
        createAttributeIfAbsent(clazz, annotationAttributes);
        for (Method method : getAttributeMethods(clazz)) {
            try {
                Object value = method.invoke(anno);
                if (value == null) {
                    value = method.getDefaultValue();
                }
                if (value == null) {
                    continue;
                }
                Map<Class<? extends Annotation>, Set<String>> alias =
                        fromSameAs(method);
                for (Entry<Class<? extends Annotation>, Set<String>> entry : alias
                        .entrySet()) {
                    Class<? extends Annotation> annotationClass =
                            entry.getKey();
                    Set<String> attributes = entry.getValue();
                    AnnotationAttribute aliasAttribute =
                            createAttributeIfAbsent(annotationClass, annotationAttributes);
                    for (String attribute : attributes) {
                        Object oldValue = aliasAttribute.getAttribute(attribute);
                        if (ObjectHelper.isEmpty(oldValue)) {
                            aliasAttribute.setAttribute(attribute, value);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Parse attribute " + method.getName()
                        + " of annotation " + anno + " failed", e);
            }
        }
    }

    private static AnnotationAttribute createAttributeIfAbsent(
            Class<? extends Annotation> annotationClass,
            Map<Class<? extends Annotation>, AnnotationAttribute> annotationAttributes) {
        AnnotationAttribute annotationAttribute =
                annotationAttributes.get(annotationClass);
        if (annotationAttribute == null) {
            annotationAttribute =
                    new AnnotationAttribute(annotationClass);
            annotationAttributes.put(annotationClass,
                    annotationAttribute);
        }
        return annotationAttribute;
    }
    
    /**
     * Get the {@link SameAs} attributes of the {@link Method} attribute.
     * @return A map with the key of {@link Annotation} class and value of a set
     *         of attribute names.
     */
    @SuppressWarnings("rawtypes")
    public static Map<Class<? extends Annotation>, Set<String>> fromSameAs(
            Method attribute) {
        Map descriptor = aliasCache.get(attribute);
        if (descriptor != null) {
            return descriptor;
        }
        descriptor = new HashMap<>();
        fromSameAs(attribute, descriptor);
        aliasCache.put(attribute, descriptor);
        return descriptor;
    }

    private static void fromSameAs(Method attribute,
            Map<Class<?>, Set<String>> descriptor) {
        Set<String> set = descriptor.get(attribute.getDeclaringClass());
        if (set == null) {
            set = new HashSet<String>();
            descriptor.put(attribute.getDeclaringClass(), set);
        }
        if (!set.add(attribute.getName())) {
            return;
        }
        SameAs sameAs = attribute.getAnnotation(SameAs.class);
        if (sameAs == null) {
            return;
        }
        Class<?> declaringClass = attribute.getDeclaringClass();
        Class<?> aliasForType =
        		sameAs.annotation()==Annotation.class ? declaringClass
                        : sameAs.annotation();
        String aliasForValue = sameAs.attribute();
        if (StringHelper.isEmpty(aliasForValue)) {
            aliasForValue = sameAs.value();
        }
        if (StringHelper.isEmpty(aliasForValue)) {
            aliasForValue = attribute.getName();
        }
        try {
            Method method = aliasForType.getDeclaredMethod(aliasForValue);
            fromSameAs(method, descriptor);
        } catch (Exception e) {
            log.debug("Resolve SameAs annotation failed", e);
            return;
        }
    }

    private static List<Method> getAttributeMethods(
            Class<? extends Annotation> annotationType) {
        List<Method> methods = attributeMethodsCache.get(annotationType);
        if (methods != null) {
            return methods;
        }
        methods = new ArrayList<>();
        for (Method method : MethodHelper.getMethods(annotationType, false)) {
            method.setAccessible(true);
            methods.add(method);
        }
        attributeMethodsCache.put(annotationType, methods);
        return methods;
    }

    public static void clear() {
        aliasCache.clear();
        annotationMapCache.clear();
        attributeMethodsCache.clear();
    }
    
}
