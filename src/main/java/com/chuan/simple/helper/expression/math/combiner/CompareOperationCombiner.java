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

import com.chuan.simple.helper.expression.math.node.BoolNode;
import com.chuan.simple.helper.expression.math.node.EqualNode;
import com.chuan.simple.helper.expression.math.node.GreaterNode;
import com.chuan.simple.helper.expression.math.node.GreaterOrEqualNode;
import com.chuan.simple.helper.expression.math.node.LessNode;
import com.chuan.simple.helper.expression.math.node.LessOrEqualNode;
import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.NotEqualNode;
import com.chuan.simple.helper.expression.math.node.NumNode;

public class CompareOperationCombiner extends Combiner {

    @Override
    public BoolNode combine(Node... nodes) {
        BoolNode bool = new BoolNode();
        NumNode a = (NumNode) nodes[0];
        NumNode b = (NumNode) nodes[2];
        Node operation = nodes[1];
        BigDecimal numberA = a.getBigDecimal();
        BigDecimal numberB = b.getBigDecimal();
        int compareResult = numberA.compareTo(numberB);
        if (operation instanceof GreaterNode) {
            bool.setValue(compareResult > 0);
        } else if (operation instanceof GreaterOrEqualNode) {
            bool.setValue(compareResult >= 0);
        } else if (operation instanceof LessNode) {
            bool.setValue(compareResult < 0);
        } else if (operation instanceof LessOrEqualNode) {
            bool.setValue(compareResult <= 0);
        } else if (operation instanceof NotEqualNode) {
            bool.setValue(compareResult != 0);
        } else if (operation instanceof EqualNode) {
            bool.setValue(compareResult == 0);
        } else {
            return null;
        }
        combineNode(bool, nodes);
        return bool;
    }

}
