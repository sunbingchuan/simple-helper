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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.InitCodeholder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.chuan.simple.helper.clazz.BuilderNameHelper;
import com.chuan.simple.helper.clazz.ClassHelper;
import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.field.FieldHelper;
import com.chuan.simple.helper.method.MethodHelper;

/**
 * Create proxy class by java bytecode technology.
 */
public class BaseProxy {

    private static final Log LOG = LogFactory.getLog(BaseProxy.class);

    private static final Method classLoaderDefineClassMethod;
    private static final ClassLoader defaultClassLoader =
            ClassHelper.getDefaultClassLoader();
    public static final String LINK_STR = "$$";
    public static final String PROXY_SUFFIX = "$ORIGINAL";
    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_ANONYMOUS_NAME = "<clinit>";
    public static final String STATIC_ANONYMOUS_PRETTY_NAME = "CLINIT";
    public static final String CONSTRUCTOR_PRETTY_NAME = "INIT";
    public static final String PROXY_CONSTRUCTOR_NAME =
            CONSTRUCTOR_PRETTY_NAME + PROXY_SUFFIX;
    public static final String METHOD_TEMPLATE_INVOKE = "invoke$TEMPLATE";
    public static final String METHOD_TEMPLATE_INIT = "init$TEMPLATE";
    public static final String WRAPPER_TARGET = "TARGET";
    public static final String VOID_METHOD_NAME = "()V";

    protected static final Map<String, Class<?>> classCache =
            new ConcurrentHashMap<>(256);
    private ClassLoader classLoader = defaultClassLoader;
    static {
        Method classLoaderDefineClass = null;
        try {
            classLoaderDefineClass = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, Integer.TYPE,
                    Integer.TYPE, ProtectionDomain.class);
            classLoaderDefineClass.setAccessible(true);
        } catch (Throwable t) {
            LOG.error("Proxy initialize failed", t);
        }
        classLoaderDefineClassMethod = classLoaderDefineClass;
    }

    protected BaseProxy() {
    }

    protected String generateClassName(Class<?> clazz, Object... entrys) {
        StringBuilder identityCode = new StringBuilder();
        for (Object entry : entrys) {
            if (entry != null)
                identityCode.append(Integer.toHexString(entry.hashCode()));
        }
        String originalClassName = clazz.getName();
        if (originalClassName.startsWith("java.")) {
            originalClassName = "proxy." + originalClassName;
        }
        return BuilderNameHelper.toResourcePath(originalClassName) + LINK_STR
                + identityCode;
    }

    protected String[] getInternalNames(Class<?>[] classes) {
        String[] internalNames = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            internalNames[i] = Type.getInternalName(classes[i]);
        }
        return internalNames;
    }

    protected void proxyMethods(ClassWriter writer, Class<?> target,
            String className) {
        for (Method method : MethodHelper.getMethods(target, false)) {
            if (Modifier.isAbstract(method.getModifiers())
                    || Modifier.isNative(method.getModifiers())) {
                continue;
            }
            String[] exceptions = getExceptionsDesc(method);
            int modifiers = extendModifiers(method);
            MethodVisitor methodVisitor =
                    writer.visitMethod(modifiers, method.getName(),
                            Type.getMethodDescriptor(method), null, exceptions);
            proxyMethod(writer, method, methodVisitor, className);
        }
    }

    protected void proxyConstructors(ClassWriter writer, Class<?> target,
            String className, Map<String, InitCodeholder> initCodeMap) {
        for (Constructor<?> constructor : target.getDeclaredConstructors()) {
            String[] exceptions = getExceptionsDesc(constructor);
            String descriptor = Type.getConstructorDescriptor(constructor);
            int modifiers = extendModifiers(constructor);
            MethodVisitor methodVisitor = writer.visitMethod(modifiers,
                    CONSTRUCTOR_NAME, descriptor, null, exceptions);
            int size =
                    Type.getType(constructor).getArgumentsAndReturnSizes() >> 2;
            InitCodeholder initCodeholder = initCodeMap.get(descriptor);
            int maxStack = Math.max(initCodeholder.getMaxStack(), 4);
            int maxLocals = Math.max(initCodeholder.getMaxLocals(), size + 3);
            methodVisitor.visitMaxs(maxStack, maxLocals);
            for (Function<MethodVisitor, Object> code : initCodeholder
                    .getCodes()) {
                code.apply(methodVisitor);
            }
            createOriginalMethodField(writer, constructor);
            proxyExecutable(constructor, methodVisitor, className, size, null);
            methodVisitor.visitInsn(Opcodes.RETURN);
        }
    }

    public void setHandler(Class<?> clazz, InvocationHandler invoker) {
        try {
            if (invoker == null) {
                return;
            }
            Method method =
                    clazz.getDeclaredMethod("setInvocationHandler$TEMPLATE",
                            new Class<?>[] { InvocationHandler.class });
            method.invoke(null, invoker);
        } catch (Exception e) {
            LOG.error("Set invoker failed", e);
        }
    }

    public void setTarget(Class<?> clazz, Object target) {
        try {
            Field field = clazz.getDeclaredField(WRAPPER_TARGET);
            FieldHelper.setFieldValue(null, field, target);
        } catch (Exception e) {
            LOG.error("Set wrapper target failed", e);
        }
    }

    protected String[] getExceptionsDesc(Executable method) {
        Class<?>[] classes = method.getExceptionTypes();
        String[] descriptors = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            descriptors[i] = Type.getInternalName(classes[i]);
        }
        return descriptors;
    }

    protected int extendModifiers(Executable executable) {
        int modifiers = executable.getModifiers();
        modifiers &= ~Opcodes.ACC_ABSTRACT;
        modifiers &= ~Opcodes.ACC_PRIVATE;
        modifiers &= ~Opcodes.ACC_PROTECTED;
        modifiers |= Opcodes.ACC_PUBLIC;
        return modifiers;
    }

    protected int extendModifiers(int modifiers) {
        modifiers &= ~Opcodes.ACC_ABSTRACT;
        modifiers &= ~Opcodes.ACC_PRIVATE;
        modifiers &= ~Opcodes.ACC_PROTECTED;
        modifiers |= Opcodes.ACC_PUBLIC;
        return modifiers;
    }

    protected void modifyConstructor(ClassWriter writer, Class<?> parent,
            String className) {
        MethodVisitor methodVisitor =
                writer.visitMethod(Opcodes.ACC_PUBLIC, CONSTRUCTOR_NAME,
                        VOID_METHOD_NAME, null, StringHelper.ARRAY_EMPTY);
        methodVisitor.visitMaxs(2, 1);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                Type.getInternalName(parent), CONSTRUCTOR_NAME,
                VOID_METHOD_NAME, false);
        methodVisitor.visitInsn(Opcodes.RETURN);
    }

    protected void generalHandling(ClassWriter writer, String className,
            boolean hasClinit) {
        createClinitMethod(writer, className, hasClinit);
        visitSource(writer);
    }

    private void createClinitMethod(ClassWriter writer, String className,
            boolean hasClinit) {
        MethodVisitor clinit =
                writer.visitMethod(Opcodes.ACC_STATIC, STATIC_ANONYMOUS_NAME,
                        VOID_METHOD_NAME, null, StringHelper.ARRAY_EMPTY);
        clinit.visitMaxs(1, 1);
        clinit.visitInsn(Opcodes.ACONST_NULL);
        clinit.visitFieldInsn(Opcodes.PUTSTATIC, className,
                "invocationHandler$TEMPLATE",
                Type.getDescriptor(InvocationHandler.class));
        clinit.visitMethodInsn(Opcodes.INVOKESTATIC, className,
                METHOD_TEMPLATE_INIT, VOID_METHOD_NAME, false);
        if (hasClinit) {
            clinit.visitMethodInsn(Opcodes.INVOKESTATIC, className,
                    STATIC_ANONYMOUS_PRETTY_NAME, VOID_METHOD_NAME, false);
        }
        clinit.visitInsn(Opcodes.RETURN);
    }

    protected void visitSource(ClassWriter writer) {
        writer.visitSource("generated", null);
    }

    protected boolean isWideType(Class<?> type) {
        return type == long.class || type == double.class;
    }

    protected void proxyMethod(ClassWriter writer, Method method,
            MethodVisitor methodVisitor, String className) {
        int size = Type.getType(method).getArgumentsAndReturnSizes() >> 2;
        methodVisitor.visitMaxs(4, size + 3);
        createOriginalMethodField(writer, method);
        proxyExecutable(method, methodVisitor, className, size, null);
        methodCastReturn(method, methodVisitor);
    }

    protected void createOriginalMethodField(ClassWriter writer,
            Executable executable) {
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                getOriginalMethodFieldName(executable),
                Type.getDescriptor(Method.class), null, null);
    }

    protected void methodCastReturn(Method method,
            MethodVisitor methodVisitor) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.TYPE)) {
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            checkCast(methodVisitor, returnType);
            methodVisitor.visitInsn(
                    Type.getType(returnType).getOpcode(Opcodes.IRETURN));
        }
        methodVisitor.visitEnd();
    }

    protected void createMethodField(ClassWriter writer, Executable executable,
            String suffix) {
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                getMethodFieldName(executable, suffix),
                Type.getDescriptor(Method.class), null, null);
    }

    protected void checkCast(MethodVisitor methodVisitor, Class<?> type) {
        if (type.isPrimitive()) {
            Class<?> wrapType = ClassHelper.resolvePrimitiveIfNecessary(type);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    Type.getInternalName(wrapType));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(wrapType), type.getName() + "Value",
                    "()" + Type.getDescriptor(type), false);
        } else {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    Type.getInternalName(type));
        }
    }

    protected void methodReturn(Method method, MethodVisitor methodVisitor) {
        if (method.getReturnType().equals(Void.TYPE)) {
            methodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            methodVisitor.visitInsn(Type.getType(method.getReturnType())
                    .getOpcode(Opcodes.IRETURN));
        }
    }

    protected void proxyExecutable(Executable executable,
            MethodVisitor methodVisitor, String className, int size,
            Object instance) {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        int count = executable.getParameterCount();
        boolean isStatic = Modifier.isStatic(executable.getModifiers());
        int thiz = isStatic ? -1 : 0;
        if (count == 1 && parameterTypes[0].isArray()
                && !parameterTypes[0].getComponentType().isPrimitive()) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, thiz + 1);
            getMethod(executable, methodVisitor, className);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, thiz + 2);
            aloadOwner(methodVisitor, isStatic, instance, className);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, thiz + 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, thiz + 1);
            templateInvoke(methodVisitor, className);
        } else {
            newArray(executable, methodVisitor, count, size, thiz);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, size + 1);
            getMethod(executable, methodVisitor, className);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, size + 2);
            aloadOwner(methodVisitor, isStatic, instance, className);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, size + 2);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, size + 1);
            templateInvoke(methodVisitor, className);
        }
    }

    protected void aloadOwner(MethodVisitor methodVisitor, boolean isStatic,
            Object instance, String className) {
        if (isStatic) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        } else {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        }
    }

    protected void getMethod(Executable executable, MethodVisitor methodVisitor,
            String className) {
        String fieldName = getOriginalMethodFieldName(executable);
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className, fieldName,
                Type.getDescriptor(Method.class));
    }

    protected String getOriginalMethodFieldName(Executable executable) {
        return getMethodFieldName(executable, PROXY_SUFFIX);
    }

    protected String getMethodFieldName(Executable executable, String suffix) {
        String desc = null, name = null;
        if (executable instanceof Constructor) {
            desc = Type.getConstructorDescriptor((Constructor<?>) executable);
            name = CONSTRUCTOR_PRETTY_NAME + suffix;
        } else if (executable instanceof Method) {
            desc = Type.getMethodDescriptor((Method) executable);
            name = executable.getName() + suffix;
        }
        return FieldHelper.getProxyMethodFieldName(name, desc);
    }

    protected void templateInvoke(MethodVisitor methodVisitor,
            String className) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className,
                METHOD_TEMPLATE_INVOKE,
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                false);
    }

    protected void newArray(Executable method, MethodVisitor methodVisitor,
            int length, int size, int thiz) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, length);
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        methodVisitor.visitVarInsn(Opcodes.ASTORE, size + 1);
        for (int s = thiz + 1, i = 0; i < length; i++, s++) {
            Class<?> type = parameterTypes[i];
            methodVisitor.visitVarInsn(Opcodes.ALOAD, size + 1);
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, i);
            if (type.isPrimitive()) {
                Class<?> wrapType =
                        ClassHelper.resolvePrimitiveIfNecessary(type);
                methodVisitor.visitVarInsn(
                        Type.getType(type).getOpcode(Opcodes.ILOAD), s);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(wrapType), "valueOf",
                        "(" + Type.getDescriptor(type) + ")"
                                + Type.getDescriptor(wrapType),
                        false);
            } else {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, s);
            }
            methodVisitor.visitInsn(Opcodes.AASTORE);
            if (isWideType(type)) {
                s++;
            }
        }
    }

    @Deprecated
    protected void getCurrentMethod(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                Type.getInternalName(MethodHelper.class), "getCurrentMethod",
                "([Ljava/lang/Object;)Ljava/lang/reflect/Executable;", false);
    }

    protected Class<?> defineClass(String name, byte[] b, int off, int len,
            ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) classLoaderDefineClassMethod.invoke(classLoader,
                    name, b, off, len, protectionDomain);
        } catch (Exception e) {
            LOG.error("Define class failed", e);
        }
        return null;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    protected class TemplateMethodFilter extends ClassReader.MethodFilter {
        TemplateMethodFilter() {
        }

        @Override
        public MethodVisitor visitMethod(ClassReader classReader,
                ClassVisitor classVisitor, int access, String name,
                String descriptor, String signature, String[] exceptions) {
            if (CONSTRUCTOR_NAME.equals(name)
                    || STATIC_ANONYMOUS_NAME.equals(name)) {
                return null;
            }
            return classVisitor.visitMethod(access, name, descriptor, signature,
                    exceptions);
        }
    }

    protected class ProxyMethodFilter extends ClassReader.MethodFilter {

        private boolean hasClinit = false;

        private Map<String, InitCodeholder> initCodeMap;

        protected ProxyMethodFilter(Map<String, InitCodeholder> initCodeMap) {
            this.initCodeMap = initCodeMap;
        }

        @Override
        public MethodVisitor visitMethod(ClassReader classReader,
                ClassVisitor classVisitor, int access, String name,
                String descriptor, String signature, String[] exceptions) {
            if (StringHelper.equals(name, STATIC_ANONYMOUS_NAME)) {
                name = STATIC_ANONYMOUS_PRETTY_NAME;
                hasClinit = true;
            } else if (!Modifier.isNative(access)
                    && !Modifier.isAbstract(access)) {
                name = name.replace(CONSTRUCTOR_NAME, CONSTRUCTOR_PRETTY_NAME)
                        + PROXY_SUFFIX;
                access = extendModifiers(access);
            }
            MethodVisitor methodVisitor = classVisitor.visitMethod(access, name,
                    descriptor, signature, exceptions);
            if (PROXY_CONSTRUCTOR_NAME.equals(name)) {
                InitCodeholder initCodeholder =
                        new InitCodeholder(methodVisitor);
                initCodeMap.put(descriptor, initCodeholder);
                methodVisitor = initCodeholder;
            }
            return methodVisitor;
        }

        public boolean isHasClinit() {
            return hasClinit;
        }

    }

    public void clear() {
        classCache.clear();
    }

}
