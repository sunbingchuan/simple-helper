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

public class PointNode extends Node {

    public PointNode(char c) {
        content.append(c);
    }

    public PointNode() {
    }

    @Override
    public boolean is(char c) {
        return c == '.';
    }

    @Override
    public Node born(char c) {
        return new PointNode(c);
    }

    @Override
    public Integer locate(Node node) {
        if (node instanceof NumNode) {
            return 0;
        }
        return null;
    }

    @Override
    protected Node doCombine() {
        if (this.isCombined()) {
            return this;
        }
        Node node = this;
        while (node!=null&&node.getPrev() != null
                && node.getPrev().getLevel() == node.getLevel()) {
            node = node.context.combine(this.prev);
        }
        while (node!=null&&node.getNext() != null
                && node.getNext().getLevel() == node.getLevel()) {
            node = node.context.combine(this);
        }
        if (node!=null) {
        	node.setCombined(true);
		}
        return node;
    }

}
