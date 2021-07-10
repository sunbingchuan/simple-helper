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
package com.chuan.simple.helper.common;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Tools for other functions.
 */
public final class ObjectHelper {

    public static final Object EMPTY = new Object();


    /**
     * @see System#identityHashCode(Object)
     */
    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }


    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Object object) {
        return !isEmpty(object);
    }
    
    public static boolean isEmpty(Object object) {
        if (object==null) {
            return true;
        }
        if (object.getClass().isArray()) {
            return Array.getLength(object)==0;
        }else if(object instanceof Collection) {
            return isEmpty((Collection<?>)object);
        }else if(object instanceof Map) {
            return isEmpty((Map<?,?>)object);
        }
        return false;
    }
    
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean hasEmpty(Object[] array) {
        if (!isEmpty(array)) {
            for (Object o : array) {
                if (o == null || (o instanceof String
                        && StringHelper.isEmpty((String) o))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code based on the contents of the specified array(if it
     * is).
     * @see Arrays#hashCode
     */
    public static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return Arrays.hashCode((Object[]) obj);
            }
            if (obj instanceof boolean[]) {
                return Arrays.hashCode((boolean[]) obj);
            }
            if (obj instanceof byte[]) {
                return Arrays.hashCode((byte[]) obj);
            }
            if (obj instanceof char[]) {
                return Arrays.hashCode((char[]) obj);
            }
            if (obj instanceof double[]) {
                return Arrays.hashCode((double[]) obj);
            }
            if (obj instanceof float[]) {
                return Arrays.hashCode((float[]) obj);
            }
            if (obj instanceof int[]) {
                return Arrays.hashCode((int[]) obj);
            }
            if (obj instanceof long[]) {
                return Arrays.hashCode((long[]) obj);
            }
            if (obj instanceof short[]) {
                return Arrays.hashCode((short[]) obj);
            }
        }
        return obj.hashCode();
    }

    /**
     * Get the nature {@link Object#toString()} result of {@code obj}.
     */
    public static String toObjectString(Object obj) {
        if (obj == null) {
            return StringHelper.EMPTY;
        }
        return obj.getClass().getName() + "@" + getIdentityHexString(obj);
    }

    /**
     * Get a string contains the elements of {@code objects}.
     */
    public static String toString(Object[] objects) {
        if (objects == null) {
            return null;
        }
        StringBuffer result = new StringBuffer("[");
        for (int i = 0; i < objects.length; i++) {
            if (i != 0) {
                result.append(",");
            }
            result.append(objects[i]);
        }
        result.append("]");
        return result.toString();
    }

    /**
     * @return true if simply equal or each element of the array(if it is)
     *         equal.
     */
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            return arrayEquals(a, b);
        }
        return false;
    }

    private static boolean arrayEquals(Object a, Object b) {
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.equals((Object[]) a, (Object[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[]) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if (a instanceof double[] && b instanceof double[]) {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if (a instanceof float[] && b instanceof float[]) {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[]) {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if (a instanceof short[] && b instanceof short[]) {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        return false;
    }

    /**
     * Create a new array with the {@code componentType} and values of
     * {@code array}.
     */
    public static Object newArray(Class<?> componentType, Object array) {
        int length = Array.getLength(array);
        Object newArray =
                Array.newInstance(componentType, Array.getLength(array));
        for (int i = 0; i < length; i++) {
            Array.set(newArray, i, Array.get(array, i));
        }
        return newArray;
    }
    
    private ObjectHelper() {
    }

}
