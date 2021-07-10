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
package org.objectweb.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A MethodVisitor that can save constructor initialization codes until super
 * &ltinit&gt.
 */
public class InitCodeholder extends MethodVisitor {

    private final List<Function<MethodVisitor, Object>> codes =
            new ArrayList<Function<MethodVisitor, Object>>();
	
    private int news = 0;

    private int maxStack;

    private int maxLocals;

    public InitCodeholder(MethodVisitor methodVisitor) {
        super(methodVisitor.api, methodVisitor);
    }

    public List<Function<MethodVisitor, Object>> getCodes() {
        return codes;
    }

    public void addCode(Function<MethodVisitor, Object> code) {
        codes.add(code);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitFrame(type, nLocal, local, nStack, stack);
                    return null;
                }
            });
        else
            super.visitFrame(type, nLocal, local, nStack, stack);
    }


    @Override
    public void visitInsn(int opcode) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitInsn(opcode);
                    return null;
                }
            });
        else
            super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitVarInsn(opcode, var);
                    return null;
                }
            });
        else
            super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitJumpInsn(opcode, label);
                    return null;
                }
            });
        else
            super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitIincInsn(var, increment);
                    return null;
                }
            });
        else
            super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label... labels) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitTableSwitchInsn(min, max, dflt, labels);
                    return null;
                }
            });
        else
            super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitLookupSwitchInsn(dflt, keys, labels);
                    return null;
                }
            });
        else
            super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitIntInsn(opcode, operand);
                    return null;
                }
            });
        else
            super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitLdcInsn(value);
                    return null;
                }
            });
        else
            super.visitLdcInsn(value);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
            String descriptor) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitFieldInsn(opcode, owner, name, descriptor);
                    return null;
                }
            });
        else
            super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String descriptor, boolean isInterface) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitMethodInsn(opcode, owner, name, descriptor,
                            isInterface);
                    return null;
                }
            });
        else
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (opcode == Opcodes.INVOKESPECIAL) {
            news--;
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor,
            Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitInvokeDynamicInsn(name, descriptor,
                            bootstrapMethodHandle, bootstrapMethodArguments);
                    return null;
                }
            });
        else
            super.visitInvokeDynamicInsn(name, descriptor,
                    bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.NEW && news >= 0) {
            news++;
        }
        if (news >= 0)
            addCode(new Function<MethodVisitor, Object>() {
                @Override
                public Object apply(MethodVisitor t) {
                    t.visitTypeInsn(opcode, type);
                    return null;
                }
            });
        else
            super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        super.visitMaxs(maxStack, maxLocals);
    }

    public int getMaxStack() {
        return maxStack;
    }

    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }

}
