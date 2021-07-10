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
package com.chuan.simple.helper.field;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.Type;

import com.chuan.simple.helper.common.StringHelper;

/**
 * Tools for dealing with {@link Field}.
 */
public final class FieldHelper {

    private static final Log LOG = LogFactory.getLog(FieldHelper.class);

    private static final Map<Class<?>, ClassFieldEntry> fieldCache = new HashMap<>();
    
    private FieldHelper() {
    }

    public static void setFieldValue(Object owner, Field field, Object value) {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            field.set(owner, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.debug("Set value of field '" + field + "' with '" + value
                    + "' failed", e);
        }
    }

    public static void setFieldValue(Object owner, Class<?> ownerClass,
            String fieldName, Object value) {
        if (ownerClass == null) {
        	if (owner!=null) {
        		ownerClass = owner.getClass();
			}else{
				throw new IllegalArgumentException("At last one of parameter '"+owner+"' and '"+ownerClass +"' isn't null");
			}
        }
        Field field = getField(ownerClass, fieldName);
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        try {
            field.set(owner, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.debug("Set value of field '" + field + "' with '" + value
                    + "' failed", e);
        }
    }

    public static Object getFieldValue(Class<?> clazz, Object owner,
            String fieldName) {
        Field field = getField(clazz, fieldName);
        return getFieldValue(field, owner);
    }

    public static Object getFieldValue(Field field, Object owner) {
        if (field == null) {
            return null;
        }
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        Object fieldValue = null;
        try {
            fieldValue = field.get(owner);
        } catch (Exception e) {
            LOG.debug("Get value of field '" + field + "' failed", e);
        }
        return fieldValue;
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName, true);
    }

    public static Field getField(Class<?> clazz, String fieldName,
            boolean inherited) {
        ClassFieldEntry classFieldEntry = getClassFieldEntry(clazz);
        Field field = classFieldEntry.getField(fieldName, inherited);
        if (field == null) {
            LOG.debug("Get field " + fieldName + " of class " + clazz
                    + " failed");
        }
        return field;
    }

    public static Collection<Field> getFields(Class<?> clazz) {
        return getFields(clazz, true);
    }
    
    public static Collection<Field> getFields(Class<?> clazz,
            boolean inherited) {
        ClassFieldEntry classFieldEntry =  getClassFieldEntry(clazz);
        return classFieldEntry.getFields(inherited);
    }
    
    private static ClassFieldEntry getClassFieldEntry(Class<?> clazz){
        ClassFieldEntry classFieldEntry= fieldCache.get(clazz);
        if (classFieldEntry==null) {
            classFieldEntry = new ClassFieldEntry(clazz); 
            fieldCache.put(clazz, classFieldEntry);
        } 
        return classFieldEntry;
    }
    

    public static String getProxyMethodFieldName(Method method) {
        return getProxyMethodFieldName(method.getName(),
                Type.getMethodDescriptor(method));
    }

    public static String getProxyMethodFieldName(String methodName,
            String desc) {
    	if (methodName==null||desc==null) {
    	    return null;
	}
        desc = desc.replaceAll("[\\(\\)]", "\\$");
        desc = desc.replaceAll(";", "_");
        desc = desc.replaceAll("/", "");
        return methodName + desc;
    }
    
    
    private static class ClassFieldEntry {
        
        private Map<String, Field> fieldMap = new HashMap<>();
        
        private Map<String, Field> allFieldMap = new HashMap<>();
        
        private Collection<Field> fields = new HashSet<>();
        
        private Collection<Field> allFields = new HashSet<>();

        
        private Field getField(String name,boolean inherited) {
            if (inherited) {
                return allFieldMap.get(name);
            }
            return fieldMap.get(name);
        }
        
        private Collection<Field> getFields(boolean inherited){
            if (inherited) {
                return allFields;
            }
            return fields;
        } 
        
        public ClassFieldEntry(Class<?> clazz) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
                allFields.add(field);
                fieldMap.putIfAbsent(field.getName(), field);
                allFieldMap.putIfAbsent(field.getName(), field);

            }
            for (Field field : clazz.getFields()) {
                allFields.add(field);
                allFieldMap.putIfAbsent(field.getName(), field);
            }
            for (clazz = clazz.getSuperclass();
                    clazz != null && !clazz.equals(Object.class); 
                    clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (!Modifier.isPrivate(field.getModifiers())) {
                        allFields.add(field);
                        allFieldMap.putIfAbsent(field.getName(), field);
                    }
                }
            }
            fields = Collections.unmodifiableCollection(fields);
            allFields = Collections.unmodifiableCollection(allFields);
            fieldMap = Collections.unmodifiableMap(fieldMap);
            allFieldMap = Collections.unmodifiableMap(allFieldMap);
        }
        
    }

    public static void clear() {
        fieldCache.clear();
    }
    
}
