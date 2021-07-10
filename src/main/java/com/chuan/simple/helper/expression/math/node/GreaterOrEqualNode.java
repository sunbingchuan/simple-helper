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

public class GreaterOrEqualNode extends Node {

    public GreaterOrEqualNode() {
    }

    public GreaterOrEqualNode(StringBuffer content) {
        this.setContent(content);
    }

    @Override
    public boolean is(char c) {
        return prev instanceof GreaterNode && c == '=';
    }

    @Override
    public boolean is(StringBuffer s) {
        return false;
    }
    
    @Override
    protected Node doCombine() {
        return this.context.combine(this.getPrev());
    }

    @Override
    public GreaterOrEqualNode born(Node prev, char c) {
        StringBuffer content = prev.getContent();
        content.append(c);
        GreaterOrEqualNode greaterOrEqualNode = born(content);
        greaterOrEqualNode.setPrev(prev.getPrev());
        return greaterOrEqualNode;
    }

    @Override
    public Node born(char c) {
        return null;
    }

    @Override
    public GreaterOrEqualNode born(StringBuffer content) {
        return new GreaterOrEqualNode(content);
    }

    @Override
    public Integer locate(Node node) {
        if (node instanceof EqualNode) {
            return 0;
        }
        return null;
    }

}
