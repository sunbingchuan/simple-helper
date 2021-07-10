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

import com.chuan.simple.helper.expression.math.node.AndNode;
import com.chuan.simple.helper.expression.math.node.BoolNode;
import com.chuan.simple.helper.expression.math.node.BoolNotNode;
import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.OrNode;
import com.chuan.simple.helper.expression.math.node.XorNode;

public class BoolOperationCombiner extends Combiner {

    @Override
    public BoolNode combine(Node... nodes) {
        if (nodes[0] instanceof BoolNotNode) {
            BoolNode bool = (BoolNode) nodes[1];
            bool.setValue(!bool.getValue());
            combineNode(bool, nodes);
            return bool;
        }
        BoolNode a = (BoolNode) nodes[0];
        BoolNode b = (BoolNode) nodes[2];
        Node operation = nodes[1];
        if (operation instanceof AndNode) {
            a.setValue(a.getValue() && b.getValue());
        } else if (operation instanceof OrNode) {
            a.setValue(a.getValue() || b.getValue());
        } else if (operation instanceof XorNode) {
            a.setValue(a.getValue() ^ b.getValue());
        } else {
            return null;
        }
        combineNode(a, nodes);
        return a;
    }

}
