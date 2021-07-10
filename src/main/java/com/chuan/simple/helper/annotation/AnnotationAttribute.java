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
import java.util.HashMap;
import java.util.Map;

/**
 * An attributes collection of the {@link #annotationType}.
 */
public class AnnotationAttribute {

    private final Map<String, Object> attributes =
            new HashMap<String, Object>();

    private Class<? extends Annotation> annotationType;

    public AnnotationAttribute(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public String toString() {
        return annotationType + "=>" + this.attributes;
    }

}
