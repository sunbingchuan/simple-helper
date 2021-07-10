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

import java.awt.Component;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Predicate;

import com.chuan.simple.constant.Constant;
import com.chuan.simple.helper.annotation.AnnotationAttribute;
import com.chuan.simple.helper.annotation.AnnotationAttributeHelper;
import com.chuan.simple.helper.common.StringHelper;

/**
 * Tools for builder,bean,class name.
 */
public final class BuilderNameHelper {

    public static final String PROXY_CLASS_SEPARATOR = "$$";

    public static final String BEAN_NAME_SEPARATOR = "#";

    private static final char PACKAGE_SEPARATOR = '.';

    private static final char PATH_SEPARATOR = '/';

    private static final char INNER_CLASS_SEPARATOR = '$';
    
    private BuilderNameHelper() {
    }

    /**
     * Transform resource path into class name.
     */
    public static String toClassName(String resourcePath) {
        return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
    }

    /**
     * Transform class name into class name resource path.
     */
    public static String toResourcePath(String className) {
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    /**
     * Generate annotated builder name by class name.
     */
    public static String generateAnnotatedBuilderName(Class<?> beanClass) {
        if (beanClass != null) {
            Map<Class<? extends Annotation>, AnnotationAttribute> attrs =
                    AnnotationAttributeHelper.from(beanClass);
            String beanName = null;
            AnnotationAttribute attr = attrs.get(Component.class);
            if (attr != null) {
                beanName = StringHelper
                        .toString(attr.getAttribute(Constant.ATTR_VALUE));
            }
            if (StringHelper.hasText(beanName)) {
                // Explicit bean name found.
                return beanName;
            }
            return generateBuilderName(beanClass.getName());
        }
        return null;
    }

    /**
     * Generate builder name by class name.
     */
    public static String generateBuilderName(String className) {
        return beautifyName(getShortName(className));
    }

    private static String getShortName(String className) {
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        int nameEndIndex = className.indexOf(PROXY_CLASS_SEPARATOR);
        if (nameEndIndex == -1) {
            nameEndIndex = className.length();
        }
        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
        shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
        return shortName;
    }

    /**
     * Xxx=>xxx,XXx not change
     */
    public static String beautifyName(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * Generate the name which predicate is satisfied.
     */
    public static String satisfiedName(String name,
            Predicate<String> predicate) {
        int i = 0;
        String result = name;
        while (!predicate.test(result)) {
        	result = name + BEAN_NAME_SEPARATOR + i++;
        }
        return result;
    }
}
