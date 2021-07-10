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
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.chuan.simple.helper.clazz.BuilderNameHelper;
import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.field.FieldHelper;
import com.chuan.simple.helper.method.MethodHelper;

/**
 * Proxy by means of wrapping
 */
public class WrapProxy extends BaseProxy {

	private static final Log log = LogFactory.getLog(WrapProxy.class);

	public WrapProxy() {
	}

	/**
	 * @see #wrap(Class, Object, InvocationHandler)
	 */
	public Object wrap(Class<?> target, Object instance) {
		try {
			Class<?> clazz = wrapClass(target, instance, null);
			if (clazz != null)
				return clazz.newInstance();
		} catch (Exception e) {
			log.error("Wrap proxy failed", e);
		}
		return null;
	}

    /**
     * Wrap {@code instance} with class {@code target}
     * @return the result match {@code target}
     * @see #wrapClass(Class, Object, InvocationHandler)
     */
    public Object wrap(Class<?> target, Object instance,
            InvocationHandler handler) {
        try {
            Class<?> clazz = wrapClass(target, instance, handler);
            Object bean = clazz.newInstance();
            return bean;
        } catch (Exception e) {
            log.error("Wrap proxy failed", e);
        }
        return null;
    }

	/**
	 * Wrap {@code instance} with class {@code target}
	 * 
	 * @param target
	 *            the target class
	 * @param instance
	 *            the instance will be wrapped
	 * @param handler
	 *            the handler which will be invoked before the invocation of
	 *            instance's methods
	 * @return the class match {@code target}
	 */
	public Class<?> wrapClass(Class<?> target, Object instance,
			InvocationHandler handler) {
		Class<?> clazz = null;
		try {
			String className = generateClassName(target, instance, handler);
			clazz = classCache.get(className);
			if (clazz == null) {
				byte[] bytes = generateWrapClass(target, className, instance);
				clazz = defineClass(BuilderNameHelper.toClassName(className),
						bytes, 0, bytes.length, null);
				setTarget(clazz, instance);
				setMethodField(clazz, target, instance);
				setHandler(clazz, handler);
				classCache.put(className, clazz);
			}
		} catch (IOException e) {
			log.error("Wrap proxy failed", e);
		}
		return clazz;
	}

    public void setMethodField(Class<?> proxy, Class<?> target,
            Object instance) {
        for (Method method : target.getMethods()) {
            if (Modifier.isFinal(method.getModifiers())) {
                continue;
            }
            String fieldName = getMethodFieldName(method, StringHelper.EMPTY);
            if (!method.getDeclaringClass().isInstance(instance)) {
                method = MethodHelper.findSameMethod(method,
                        instance.getClass());
            }
            FieldHelper.setFieldValue(null, proxy, fieldName, method);
        }
    }

    public void wrapMethods(ClassWriter writer, Method[] methods,
            String className, Object instance) {
        for (Method method : methods) {
            wrapMethod(writer, method, className, instance);
        }
    }

    public void wrapMethod(ClassWriter writer, Method method, String className,
            Object instance) {
        if (Modifier.isFinal(method.getModifiers())) {
            return;
        }
        int size = Type.getType(method).getArgumentsAndReturnSizes() >> 2;
        String[] exceptions = getExceptionsDesc(method);
        int modifiers = method.getModifiers() & ~Opcodes.ACC_ABSTRACT
                & ~Opcodes.ACC_NATIVE;
        MethodVisitor methodVisitor =
                writer.visitMethod(modifiers, method.getName(),
                        Type.getMethodDescriptor(method), null, exceptions);
        methodVisitor.visitMaxs(4, size + 3);
        createMethodField(writer, method, StringHelper.EMPTY);
        proxyExecutable(method, methodVisitor, className, size, instance);
        methodCastReturn(method, methodVisitor);
    }

    @Override
    protected void aloadOwner(MethodVisitor methodVisitor, boolean isStatic,
            Object instance, String className) {
        if (isStatic) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className,
                    WRAPPER_TARGET, Type.getDescriptor(instance.getClass()));
        }
    }

    @Override
    protected void getMethod(Executable executable, MethodVisitor methodVisitor,
            String className) {
        String fieldName = getMethodFieldName(executable, StringHelper.EMPTY);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className, fieldName,
                Type.getDescriptor(Method.class));
    }

    private byte[] generateWrapClass(Class<?> target, String className,
        	Object instance) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(Template.class.getName()).setClassName(className)
        		.setMethodFilter(new BaseProxy.TemplateMethodFilter())
        		.accept(writer, ClassReader.SKIP_DEBUG);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null,
        		Type.getInternalName(target), StringHelper.ARRAY_EMPTY);
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
        		WRAPPER_TARGET, Type.getDescriptor(instance.getClass()), null,
        		null);
        modifyConstructor(writer, target, className);
        wrapMethods(writer, target.getMethods(), className, instance);
        generalHandling(writer, className, false);
        return writer.toByteArray();
    }

}
