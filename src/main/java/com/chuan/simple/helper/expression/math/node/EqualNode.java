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

public class EqualNode extends Node {

    private static final String EQUAL = "==";

    public EqualNode(StringBuffer content) {
        this.setContent(content);
    }

    public EqualNode() {
    }

    @Override
    public boolean is(char c) {
        return false;
    }

    @Override
    public boolean is(StringBuffer s) {
        return EQUAL.equals(s.toString());
    }
    
    @Override
    protected Node doCombine() {
        return this.context.combine(this.getPrev());
    }

    @Override
    public EqualNode born(StringBuffer content) {
        return new EqualNode(content);
    }

    @Override
    public Node born(char c) {
        return null;
    }

    @Override
    public Integer locate(Node node) {
        if (node instanceof NotEqualNode) {
            return 0;
        }
        return null;
    }
}
