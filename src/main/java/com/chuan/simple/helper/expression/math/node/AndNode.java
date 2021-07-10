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

public class AndNode extends Node {

    private boolean pair = false;

    public AndNode(char c) {
        content.append(c);
    }
    
    public AndNode() {
    }

    
    @Override
    public boolean is(char c) {
        return c == '&';
    }

    @Override
    public Node born(Node prev, char c) {
        if (prev instanceof AndNode) {
            setPair(true);
        }
        return super.born(prev, c);
    }

    @Override
    public Node born(char c) {
        return new AndNode(c);
    }

    public boolean isPair() {
        return pair;
    }

    public void setPair(boolean pair) {
        this.pair = pair;
    }

    @Override
    protected Node doCombine() {
        return this.context.combine(this.prev);
    }

}
