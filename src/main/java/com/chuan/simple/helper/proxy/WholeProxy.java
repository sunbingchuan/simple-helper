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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.InitCodeholder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.chuan.simple.helper.clazz.BuilderNameHelper;
import com.chuan.simple.helper.exception.SimpleProxyException;

/**
 * Proxy by means of redefining the whole class. It is important to note that
 * the redefined class is not match the original class,whose methods can be
 * invoked by {@link MethodHelper#invoke}
 */
public class WholeProxy extends BaseProxy {

    private static final Log log = LogFactory.getLog(WholeProxy.class);

    /**
     * @see #proxyClass(Class, InvocationHandler)
     */
    public Object proxy(Class<?> target, InvocationHandler handler) {
        try {
            Class<?> clazz = proxyClass(target, handler);
            if (clazz!=null) 
            return clazz.newInstance();
        } catch (Exception e) {
            log.error("Whole proxy failed", e);
        }
        return null;
    }

    /**
     * Redirect all the methods (contains constructor) declared by class
     * {@code target} to {@code handler}. The original method will be renamed
     * end with {@link #PROXY_SUFFIX} and transferred into {@code handler}, user
     * can invoke the original method in {@code handler} if need. Notice that
     * the class created by this method is not extends class {@code target},such
     * that if there are behaviors based on the class {@code target},the proxy
     * might fail.
     * @param target
     *            the class to be entrusted
     * @param handler
     *            the InvocationHandler to redirect to
     * @return the proxy class
     */
    public Class<?> proxyClass(Class<?> target, InvocationHandler handler) {
        if (Object.class.equals(target)) {
            throw new SimpleProxyException(
                    "Class java.lang.Object can't be proxied");
        }
        Class<?> clazz = null;
        try {
            String className = generateClassName(target, handler);
            clazz = classCache.get(className);
            if (clazz==null){
                byte[] bytes = generateProxyClass(target, className);
                clazz = defineClass(BuilderNameHelper.toClassName(className),
                        bytes, 0, bytes.length, null);
                setHandler(clazz, handler);
                classCache.put(className, clazz);
            }
        } catch (IOException e) {
            log.error("Whole proxy failed", e);
        }
        return clazz;
    }

    private byte[] generateProxyClass(Class<?> target, String className)
            throws IOException {
        String[] interfaces = getInternalNames(target.getInterfaces());
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(Template.class.getName()).setClassName(className)
                .setMethodFilter(new BaseProxy.TemplateMethodFilter())
                .accept(writer, ClassReader.SKIP_DEBUG);
        Map<String, InitCodeholder> initCodeMap = new HashMap<>();
        ProxyMethodFilter filter =
                new BaseProxy.ProxyMethodFilter(initCodeMap);
        new ClassReader(target.getName()).setClassName(className)
                .setVisitInnerClass(false).setMethodFilter(filter)
                .accept(writer, ClassReader.SKIP_DEBUG);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null,
                Type.getInternalName(target.getSuperclass()), interfaces);
        proxyMethods(writer, target, className);
        proxyConstructors(writer, target, className, initCodeMap);
        generalHandling(writer, className, filter.isHasClinit());
        return writer.toByteArray();
    }

}
