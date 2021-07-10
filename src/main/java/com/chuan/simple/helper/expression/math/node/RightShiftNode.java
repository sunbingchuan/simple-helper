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
package com.chuan.simple.helper.expression.math.node;

public class RightShiftNode extends Node {

    private boolean unSigned = false;

    public RightShiftNode() {
    }

    public RightShiftNode(StringBuffer content) {

    }

    @Override
    public boolean is(char c) {
        return (this.prev instanceof GreaterNode
                || (this.prev instanceof RightShiftNode
                        && !((RightShiftNode) this.prev).isUnSigned()))
                && c == '>';
    }

    @Override
    protected Node doCombine() {
        return this.context.combine(this.getPrev());
    }


    @Override
    public Node born(Node prev, char c) {
        StringBuffer content = prev.getContent();
        content.append(c);
        RightShiftNode rightShiftNode = born(content);
        if (prev instanceof RightShiftNode) {
            rightShiftNode.setUnSigned(true);
        }
        rightShiftNode.setPrev(prev.getPrev());
        prev.getPrev().setNext(rightShiftNode);
        return rightShiftNode;
    }

    @Override
    public RightShiftNode born(StringBuffer content) {
        return new RightShiftNode(content);
    }

    public boolean isUnSigned() {
        return unSigned;
    }

    public void setUnSigned(boolean isUnSigned) {
        this.unSigned = isUnSigned;
    }

    @Override
    public Integer locate(Node node) {
        if (node instanceof LeftShiftNode) {
            return 0;
        }
        return null;
    }

    @Override
    protected Node born(char c) {
        return null;
    }

}
