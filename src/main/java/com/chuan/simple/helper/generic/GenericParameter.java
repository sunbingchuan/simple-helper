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

import java.security.InvalidParameterException;

/**
 * To identify a generic param of a specific class.
 */
public class GenericParameter {
    private final Class<?> clazz;
    private final int index;

    public GenericParameter(Class<?> clazz, int index) {
        if (clazz == null) {
            throw new InvalidParameterException(
                    "param clazz shuld not be null");
        }
        this.clazz = clazz;
        this.index = index;
    }

    @Override
    public int hashCode() {
        return this.clazz.hashCode() ^ this.index;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericParameter) {
            GenericParameter cgp = (GenericParameter) obj;
            return cgp.clazz.equals(this.clazz) && cgp.index == this.index;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.clazz.toString() + "." + this.index;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public int getIndex() {
        return index;
    }

}
