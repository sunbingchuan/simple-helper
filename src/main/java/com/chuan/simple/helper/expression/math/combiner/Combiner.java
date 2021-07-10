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
package com.chuan.simple.helper.expression.math.combiner;

import java.math.BigDecimal;

import com.chuan.simple.helper.expression.math.node.Node;

public abstract class Combiner {

    protected static final int DEFAULT_SCALE = 32;

    protected int scale = DEFAULT_SCALE;

    protected int roundingMode = BigDecimal.ROUND_HALF_UP;

    public abstract Node combine(Node... nodes);

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(int roundingMode) {
        this.roundingMode = roundingMode;
    }

    protected boolean validate(Node... nodes) {
        return nodes != null && nodes.length > 1;
    }

    protected void combineNode(Node result, Node... nodes) {
        if (!validate(nodes)) {
            return;
        }
        Node first = nodes[0];
        Node last = nodes[nodes.length - 1];
        Node prev = first.getPrev();
        result.setPrev(prev);
        Node next = last.getNext();
        result.setNext(next);
    }
}
