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
package com.chuan.simple.helper.method;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.chuan.simple.annotation.Param;
import com.chuan.simple.helper.clazz.ClassHelper;
import com.chuan.simple.helper.clazz.Null;
import com.chuan.simple.helper.common.ObjectHelper;
import com.chuan.simple.helper.common.StringHelper;

/**
 * Tools for {@link Parameter}.
 */
public final class ParameterHelper {

    private static final Log LOG = LogFactory.getLog(ParameterHelper.class);

    private static final Map<Class<?>, Map<Member, String[]>> parameterNamesCache =
            new HashMap<>(32);

    private static final Map<Executable, Parameter[]> parameterCache =
            new HashMap<>();

    public static boolean paramsFit(Class<?>[] current, Class<?>[] restrict) {
        if ((current == null || current.length == 0)
                && (restrict == null || restrict.length == 0)) {
            return true;
        }
        if ((current == null && restrict.length > 0)
                || (restrict == null && current.length > 0)) {
            return false;
        }
        if (current.length != restrict.length) {
            return false;
        }
        for (int i = 0; i < restrict.length; i++) {
            if (!ClassHelper.resolvePrimitiveIfNecessary(restrict[i])
                    .isAssignableFrom(
                            ClassHelper.resolvePrimitiveIfNecessary(current[i]))
                    && current[i] != Null.class) {
                return false;
            }
        }
        return true;
    }

    public static boolean paramsFit(Object[] current, Class<?>[] restrict) {
        Class<?>[] currentParamTypes = getParameterTypes(current);
        return paramsFit(currentParamTypes, restrict);
    }

    public static boolean paramsEqual(Class<?>[] a, Class<?>[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("params couldn't  be null");
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < b.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    public static Class<?>[] getParameterTypes(Object... params) {
        List<Class<?>> list = new ArrayList<>();
        for (Object param : params) {
            if (param == null) {
                list.add(Null.class);
                continue;
            }
            list.add(param.getClass());
        }
        return list.toArray(new Class<?>[0]);

    }

    public static Parameter[] getParameters(Executable executable) {
        Parameter[] parameters = parameterCache.get(executable);
        if (parameters == null) {
            parameters = executable.getParameters();
            parameterCache.put(executable, parameters);
        }
        return parameters;
    }

    public static String[] getParameterTypeNames(String methodDesc) {
        Type[] types = Type.getArgumentTypes(methodDesc);
        String[] parameterTypeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            parameterTypeNames[i] = type.getClassName();
        }
        return parameterTypeNames;
    }

    public static String[] getParameterNames(Executable executable) {
        if (executable.getParameterCount() == 0) {
            return StringHelper.ARRAY_EMPTY;
        }
        Parameter[] parameters = getParameters(executable);
        Class<?> owner = executable.getDeclaringClass();
        String[] parameterNames = null;
        Map<Member, String[]> map =
                parameterNamesCache.get(executable.getDeclaringClass());
        if (map != null) {
            parameterNames = map.get(executable);
        }
        if (parameterNames != null) {
            return parameterNames;
        }
        parameterNames = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            parameterNames[i] = getPresentName(param);
        }
        if (ObjectHelper.hasEmpty(parameterNames)) {
            try {
                Map<Member, String[]> cache = new HashMap<>();
                ClassReader reader =
                        new ClassReader(ClassHelper.getClassInputStream(owner));
                reader.accept(new ParamterClassVisitor(Opcodes.ASM8, cache, owner),
                        0);
                mergeName(parameterNames, cache.get(executable));
                parameterNamesCache.put(owner, cache);
            } catch (Exception e) {
                LOG.debug("Resolve parameter name by class file failed", e);
            }
        }
        return parameterNames;
    }

    public static String getParameterName(Parameter parameter) {
        return getParameterNames(
                parameter.getDeclaringExecutable())[getParameterIndex(
                        parameter)];
    }

    public static int getParameterIndex(Parameter parameter) {
        Executable executable = parameter.getDeclaringExecutable();
        Parameter[] parameters = getParameters(executable);
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(parameter)) {
                return i;
            }
        }
        return -1;
    }

    private static void mergeName(String[] parameterNames, String[] other) {
        if (other != null) {
            for (int i = 0; i < other.length; i++) {
                if (parameterNames[i] == null && other[i] != null) {
                    parameterNames[i] = other[i];
                }
            }
        }
    }

    private static String getPresentName(Parameter parameter) {
        String name = null;
        Param param = parameter.getAnnotation(Param.class);
        if (param != null) {
            if (StringHelper.isNotEmpty(name = param.value())) {
                return name;
            }
        }
        if (parameter.isNamePresent()) {
            name = parameter.getName();
        }
        return name;
    }

    private ParameterHelper() {
    }

    private static class ParamterClassVisitor extends ClassVisitor {

        private Map<Member, String[]> cache;
        private Class<?> owner;

        public ParamterClassVisitor(int api, Map<Member, String[]> cache,
                Class<?> owner) {
            super(api);
            this.cache = cache;
            this.owner = owner;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName,
                String descriptor, String signature, String[] exceptions) {
            Type[] args = Type.getArgumentTypes(descriptor);
            String[] paras = new String[args.length];
            int[] localVariableTableSlotIndex = computelocalVariableTableSlotIndices(
                    (access & Opcodes.ACC_STATIC) > 0, args);
            return new ParamterMethodVisitor(Opcodes.ASM8, localVariableTableSlotIndex, paras, methodName, args);
        }

        private int[] computelocalVariableTableSlotIndices(boolean isStatic,
                Type[] paramTypes) {
            int[] localVariableTableIndex = new int[paramTypes.length];
            int nextIndex = (isStatic ? 0 : 1);
            for (int i = 0; i < paramTypes.length; i++) {
                localVariableTableIndex[i] = nextIndex;
                if (isWideType(paramTypes[i])) {
                    nextIndex += 2;
                } else {
                    nextIndex++;
                }
            }
            return localVariableTableIndex;
        }

        private boolean isWideType(Type aType) {
            // float is not a wide type
            return (aType == Type.LONG_TYPE || aType == Type.DOUBLE_TYPE);
        }

        private  class ParamterMethodVisitor extends MethodVisitor{

            private static final String CONSTRUCTOR = "<init>";
            private static final String CLINIT = "<clinit>";

            private int[] localVariableTableSlotIndex;
            private String[] paras;
            private String methodName;
            private Type[] args;
            
            public ParamterMethodVisitor(int api,int[] localVariableTableSlotIndex,String[] paras,
                    String methodName,Type[] args) {
                super(api);
                this.localVariableTableSlotIndex = localVariableTableSlotIndex;
                this.paras=paras;
                this.methodName=methodName;
                this.args=args;
            }
            
            @Override
            public void visitLocalVariable(String name, String descriptor,
                    String signature, Label start, Label end, int index) {
                for (int i = 0; i < localVariableTableSlotIndex.length; i++) {
                    if (localVariableTableSlotIndex[i] == index) {
                        paras[i] = name;
                    }
                }

            }

            @Override
            public void visitEnd() {
                if (!methodName.equals(CLINIT)) {
                    cache.put(resolveMember(), paras);
                }
            }

            private Member resolveMember() {
                ClassLoader loader = owner.getClassLoader();
                Class<?>[] argTypes = new Class<?>[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = ClassHelper
                            .forName(args[i].getClassName(), loader);
                }
                try {
                    if (CONSTRUCTOR.equals(methodName)) {
                        return owner.getDeclaredConstructor(argTypes);
                    }
                    return owner.getDeclaredMethod(methodName, argTypes);
                } catch (NoSuchMethodException ex) {
                    LOG.debug("Method [" + methodName
                            + "] was discovered in the .class file but cannot be resolved in the class object",
                            ex);
                    return null;
                }
            }
        
        }
        
    }

    public static void clear() {
        parameterCache.clear();
        parameterNamesCache.clear();
    }
    
}
