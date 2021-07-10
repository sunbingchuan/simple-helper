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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.chuan.simple.helper.clazz.BuilderNameHelper;
import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.method.MethodHelper;

/**
 * Proxy by means of {@code extends} and {@code implements}.
 */
public class InstanceProxy extends BaseProxy {

    private static final Log log = LogFactory.getLog(InstanceProxy.class);

    public InstanceProxy() {
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[], Class[])
     */
    public Object instance(InvocationHandler handler, Class<?> parent) {
        return instance(handler, parent, parent.getDeclaredMethods(),
                new Class[0]);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[], Class[])
     */
    public Object instance(InvocationHandler handler, Class<?> parent,
            Method[] overrides) {
        return instance(handler, parent, overrides, new Class[0]);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[], Class[])
     */
    public Object instance(InvocationHandler handler,
            Class<?>[] interfaceClasses) {
        return instance(handler, Object.class, null, interfaceClasses);
    }

    /**
     * @see InstanceProxy#instance(InvocationHandler, Class, Method[], Class[])
     */
    public Object instance(InvocationHandler handler, Class<?> parent,
            Class<?>[] interfaceClasses) {
        return instance(handler, parent, null, interfaceClasses);
    }

    /**
     * <p>
     * Create instance which extends {@code parent} and implements
     * {@code interfaces}.
     * <p>
     * Redirect methods {@code overrides} of {@code parent} to {@code handler}.
     * <p>
     * Implements {@code interfaces} by {@code handler} (static method
     * excluded).
     * @param handler
     *            the InvocationHandler to redirect to.
     * @param parent
     *            the class to be extended
     * @param overrides
     *            the method of {@code parent} to be overridden/intercepted. The
     *            {@code final,static,private,native} method in
     *            {@code overrides} will be ineffectual.
     * @param interfaces
     *            the interface to be implements
     * @return the instance
     */
    public Object instance(InvocationHandler handler, Class<?> parent,
            Method[] overrides, Class<?>[] interfaces) {
        try {
            String className = generateClassName(parent, handler);
            Class<?> clazz = classCache.get(className);
            if (clazz==null){
                byte[] bytes = generateClass(Template.class, className, parent,
                        overrides, interfaces);
                clazz = defineClass(BuilderNameHelper.toClassName(className),
                        bytes, 0, bytes.length, null);
                setHandler(clazz, handler);
                classCache.put(className, clazz);
            }
            Object bean = clazz.newInstance();
            return bean;
        } catch (Exception e) {
            log.error("Instance by proxy failed", e);
        }
        return null;
    }

    protected byte[] generateClass(Class<?> initializer, String className,
            Class<?> parent, Method[] overrides, Class<?>[] interfaceClasses)
            throws IOException {
        String[] interfaces =StringHelper.ARRAY_EMPTY;
        if (interfaceClasses != null && interfaceClasses.length > 0) {
            interfaces = getInternalNames(interfaceClasses);
        }
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(initializer.getName()).setClassName(className)
                .setMethodFilter(new BaseProxy.TemplateMethodFilter())
                .accept(writer, ClassReader.SKIP_DEBUG);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null,
                Type.getInternalName(parent), interfaces);
        modifyConstructor(writer, parent, className);
        implInterfaces(writer, interfaceClasses, className);
        if (overrides != null && overrides.length > 0) {
            overrideMethods(writer, overrides, className);
        }
        generalHandling(writer, className, false);
        return writer.toByteArray();
    }

    private void implInterfaces(ClassWriter writer, Class<?>[] interfaceClasses,
            String className) {
        if (interfaceClasses == null) {
            return;
        }
        for (Class<?> clazz : interfaceClasses) {
            Collection<Method> methods = MethodHelper.getMethods(clazz,false);
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.isDefault()) {
                    overrideMethod(writer, method, className);
                    continue;
                }
                String[] exceptions = getExceptionsDesc(method);
                MethodVisitor methodVisitor = writer.visitMethod(
                        Opcodes.ACC_PUBLIC, method.getName(),
                        Type.getMethodDescriptor(method), null, exceptions);
                proxyMethod(writer, method, methodVisitor, className);
            }
        }
    }

    protected void overrideMethods(ClassWriter writer, Method[] methods,
            String className) {
        for (Method method : methods) {
            overrideMethod(writer, method, className);
        }
    }

    private void overrideMethod(ClassWriter writer, Method method,
            String className) {
        int modifiers = method.getModifiers();
        if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers)
                || Modifier.isNative(modifiers)
                || Modifier.isStatic(modifiers)) {
            return;
        }
        int overrideModifiers = extendModifiers(method);
        String[] exceptions = getExceptionsDesc(method);
        MethodVisitor methodVisitor =
                writer.visitMethod(overrideModifiers, method.getName(),
                        Type.getMethodDescriptor(method), null, exceptions);
        proxyMethod(writer, method, methodVisitor, className);
        if (Modifier.isAbstract(modifiers)) {
            return;
        }
        MethodVisitor methodVisitorSuper = writer.visitMethod(overrideModifiers,
                method.getName() + PROXY_SUFFIX,
                Type.getMethodDescriptor(method), null, exceptions);
        redirectSuper(method, methodVisitorSuper);
    }
    
    private void redirectSuper(Method method, MethodVisitor methodVisitor) {
        int size = Type.getType(method).getArgumentsAndReturnSizes() >> 2;
        Class<?>[] parameterTypes = method.getParameterTypes();
        methodVisitor.visitMaxs(size, size);
        for (int count = -1, i = 0; i < size; i++, count++) {
            int opcode = Opcodes.ALOAD;
            if (i > 0) {
                opcode = Type.getType(parameterTypes[count])
                        .getOpcode(Opcodes.ILOAD);
            }
            methodVisitor.visitVarInsn(opcode, i);
            if (i > 0 && isWideType(parameterTypes[count])) {
                i++;
            }
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                Type.getInternalName(method.getDeclaringClass()),
                method.getName(), Type.getMethodDescriptor(method), false);
        methodReturn(method, methodVisitor);
    }

}
