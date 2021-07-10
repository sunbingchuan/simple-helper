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

import com.chuan.simple.helper.expression.math.node.DivideNode;
import com.chuan.simple.helper.expression.math.node.MinusNode;
import com.chuan.simple.helper.expression.math.node.ModNode;
import com.chuan.simple.helper.expression.math.node.MultiplyNode;
import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.NumNode;
import com.chuan.simple.helper.expression.math.node.PlusNode;
import com.chuan.simple.helper.expression.math.node.SignNode;

public class MathOperationCombiner extends Combiner {

    @Override
    public NumNode combine(Node... nodes) {
        if (nodes[0] instanceof SignNode) {
            SignNode sign = (SignNode) nodes[0];
            NumNode num = (NumNode) nodes[1];
            BigDecimal numberA = num.getBigDecimal();
            numberA = numberA.multiply(new BigDecimal(sign.getContent() + "1"));
            num.setContent(numberA.toString());
            combineNode(num, nodes);
            return num;
        }
        NumNode a = (NumNode) nodes[0];
        NumNode b = (NumNode) nodes[2];
        Node operation = nodes[1];
        BigDecimal numberA = a.getBigDecimal();
        BigDecimal numberB = b.getBigDecimal();
        if (operation instanceof PlusNode) {
            numberA = numberA.add(numberB);
        } else if (operation instanceof MinusNode) {
            numberA = numberA.subtract(numberB);
        } else if (operation instanceof MultiplyNode) {
            numberA = numberA.multiply(numberB);
        } else if (operation instanceof DivideNode) {
            numberA = numberA.divide(numberB, scale, roundingMode);
        } else if (operation instanceof ModNode) {
            numberA = numberA.remainder(numberB);
        } else {
            return null;
        }
        a.setContent(numberA.toString());
        combineNode(a, nodes);
        return a;
    }

}
