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

public class SignNode extends Node {

    public SignNode(char c) {
        content.append(c);
    }

    public SignNode() {
    }

    @Override
    public boolean is(char c) {
        return (c == '+' || c == '-')
                && (prev == null || (!(prev instanceof NumNode)
                        && !((prev instanceof BracketNode)
                                && (!((BracketNode) prev).isBegin()))));
    }

    @Override
    public Node born(char c) {
        return new SignNode(c);
    }

    @Override
    public Integer locate(Node node) {
        if (node instanceof BitNotNode) {
            return 0;
        }
        return null;
    }

    @Override
    protected Node doCombine() {
    	return doCombinePrefixNode(BitNotNode.class,SignNode.class);
    }

}
