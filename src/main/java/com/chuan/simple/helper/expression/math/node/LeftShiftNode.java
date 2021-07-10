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

public class LeftShiftNode extends Node {

    public LeftShiftNode() {
    }

    public LeftShiftNode(StringBuffer content) {
        this.setContent(content);
    }

    @Override
    public boolean is(char c) {
        return this.prev instanceof LessNode && c == '<';
    }


    @Override
    public Node born(Node prev, char c) {
        StringBuffer content = prev.getContent();
        content.append(c);
        LeftShiftNode leftShiftNode = born(content);
        leftShiftNode.setPrev(prev.getPrev());
        return leftShiftNode;
    }
    
    @Override
    protected Node doCombine() {
        return this.context.combine(this.getPrev());
    }

    @Override
    public Node born(char c) {
        return born(prev, c);
    }

    @Override
    public LeftShiftNode born(StringBuffer content) {
        return new LeftShiftNode(content);
    }

}
