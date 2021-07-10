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

public class BracketNode extends Node {

    private static final String brackets = "()";
    private int index = -1;
    private boolean begin = false;

    public BracketNode(char c) {
        this.index = brackets.indexOf(c);
        this.begin = index % 2 == 0;
        content.append(c);
    }
    
    @Override
    public boolean is(char c) {
        return brackets.indexOf(c) >= 0;
    }

    public boolean isBegin() {
        return begin;
    }

    @Override
    public Node doCombine() {
        Node opposite = null;
        if (this.isBegin()) {
            if ((opposite = this.getNext().getNext()) instanceof BracketNode
                    && !((BracketNode) opposite).isBegin()) {
                return deleteBracket(this, this.getNext(),
                        (BracketNode) opposite);
            }
            this.getNext().setCombined(true);
            return opposite.combine();
        }
        if ((opposite = this.getPrev().getPrev()) instanceof BracketNode
                && ((BracketNode) opposite).isBegin()) {
            return deleteBracket((BracketNode) opposite, this.getPrev(), this);
        }
        this.getPrev().setCombined(true);
        return opposite.combine();
    }

    private Node deleteBracket(BracketNode start, Node content,
            BracketNode end) {
        content.setPrev(start.getPrev());
        content.setNext(end.getNext());
        return content;
    }

    public BracketNode() {
    }

    @Override
    public Node born(char c) {
        return new BracketNode(c);
    }

}