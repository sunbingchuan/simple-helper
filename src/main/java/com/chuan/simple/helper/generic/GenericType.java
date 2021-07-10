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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains a set of {@link GenericParameter} of the same
 * {@link GenericType#actualType}.
 */
public class GenericType {

    private final Set<GenericParameter> corresponds = new HashSet<>();

    private final Class<?> actualType;

    private ParameterizedType parameterizedType;

    private Type type;

    public GenericType(Class<?> actualType) {
        this.actualType = actualType;
    }

    public Class<?> getActualType() {
        return actualType;
    }

    public Set<GenericParameter> getCorresponds() {
        return corresponds;
    }

    public void addCorresponds(GenericParameter... genericParameters) {
        for (GenericParameter parameter : genericParameters) {
            corresponds.add(parameter);
        }
    }

    public boolean math(Class<?> clazz, int index) {
        return corresponds.contains(new GenericParameter(clazz, index));
    }

    public ParameterizedType getParameterizedType() {
        return parameterizedType;
    }

    public void setParameterizedType(ParameterizedType parameterizedType) {
        this.parameterizedType = parameterizedType;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return actualType + "<=" + corresponds;
    }

}
