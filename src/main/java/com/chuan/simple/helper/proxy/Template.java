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
package com.chuan.simple.helper.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.chuan.simple.helper.clazz.ClassHelper;
import com.chuan.simple.helper.exception.SimpleProxyException;
import com.chuan.simple.helper.field.FieldHelper;

public class Template {

    private static InvocationHandler invocationHandler$TEMPLATE = null;

    public Template() {
    }

    public static void init$TEMPLATE() {
        Class<?> thiz = ClassHelper.getCurrentClass(ProxyHelper.getClassLoader());
        Method[] methods = thiz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().indexOf(BaseProxy.PROXY_SUFFIX) >= 0) {
                String fieldName = FieldHelper.getProxyMethodFieldName(method);
                FieldHelper.setFieldValue(null, thiz, fieldName, method);
            }
        }
    }

    public static void setInvocationHandler$TEMPLATE(
            InvocationHandler invocationHandler) {
        invocationHandler$TEMPLATE = invocationHandler;
    }

    public static InvocationHandler getInvocationHandler$TEMPLATE() {
        return invocationHandler$TEMPLATE;
    }

    public static Object invoke$TEMPLATE(Object object, Method method,
            Object... args) {
        try {
            if (invocationHandler$TEMPLATE != null) {
                return invocationHandler$TEMPLATE.invoke(object, method, args);
            }
            if (method == null) {
                throw new SimpleProxyException(
                        "Unimplemented method without handler");
            }
            return method.invoke(object, args);
        } catch (Throwable e) {
            throw new SimpleProxyException("Method invoke failed", e);
        }
    }

}
