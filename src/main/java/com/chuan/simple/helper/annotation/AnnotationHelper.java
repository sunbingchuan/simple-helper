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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.chuan.simple.helper.method.MethodHelper;


/**
 * Tools for resolving Annotation.
 */
public final class AnnotationHelper {

    private static final Map<AnnotatedElement, Annotation[]> declaredAnnotationsCache =
            new ConcurrentHashMap<>(256);

    private AnnotationHelper() {
    }

    /**
     * Get all the {@code Annotation}s of the element.
     * @param element
     *            an {@code AnnotatedElement}
     */
    public static Annotation[] getDeclaredAnnotations(
            AnnotatedElement element) {
        return declaredAnnotationsCache.computeIfAbsent(element,
                AnnotatedElement::getDeclaredAnnotations);
    }

    public static boolean isInJavaLangAnnotationPackage(String annotationType) {
        return annotationType != null
                && annotationType.startsWith("java.lang.annotation");
    }

    /**
     * Find the {@code Annotation} with the type annotationType. This method
     * will search all the super classes and interfaces or the overridden
     * methods (if the {@code annotatedElement} is a method) until the first
     * {@code Annotation} with the type annotationType was found. of the
     * annotatedElement.
     * @param annotatedElement
     * @param annotationType
     * @param <A>
     * @return
     */
    public static <A extends Annotation> A findAnnotation(
            AnnotatedElement annotatedElement, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        A result = null;
        if (annotatedElement instanceof Method) {
            result = findAnnotation((Method) annotatedElement, null,
                    annotationType);
        } else if (annotatedElement instanceof Class) {
            result = findAnnotation((Class<?>) annotatedElement,
                    annotationType);
        } else {
            result = annotatedElement.getDeclaredAnnotation(annotationType);
        }
        return result;
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz,
            Class<A> annotationType) {
        return findAnnotation(clazz, annotationType, new HashSet<>());
    }

    private static <A extends Annotation> A findAnnotation(Class<?> clazz,
            Class<A> annotationType, Set<Annotation> visited) {
        A result = clazz.getDeclaredAnnotation(annotationType);
        if (result != null) {
            return result;
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            result = findAnnotation(superclass, annotationType, visited);
        }
        if (result != null) {
            return result;
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            result = findAnnotation(ifc, annotationType, visited);
            if (result != null) {
                return result;
            }
        }
        return result;
    }

    private static <A extends Annotation> A findAnnotation(Method method,
            Class<?> clazz, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        if (clazz == null) {
            clazz = method.getDeclaringClass();
        }
        Method resolvedMethod = MethodHelper.findOriginalMethod(method);
        A result = resolvedMethod.getDeclaredAnnotation(annotationType);
        if (result == null&&clazz != null) {
            if ((clazz = clazz.getSuperclass())==null) {
				return result;
			}
            if (clazz != Object.class) {
                Method m = MethodHelper.findSameMethod(method, clazz);
                if (m != null) {
                    result = findAnnotation(m, annotationType);
                    if (result != null) {
                        return result;
                    }
                }
            }
            for (Class<?> c : clazz.getInterfaces()) {
                Method m = MethodHelper.findSameMethod(method, c);
                if (m != null) {
                    result = findAnnotation(m, annotationType);
                    if (result != null) {
                        return result;
                    }
                }
            }

        }
        return result;
    }

    public static void clear() {
        declaredAnnotationsCache.clear();
    }
    
}
