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

import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.NumNode;

public class NumCombiner extends Combiner {

    public NumNode combine(Node... nodes) {
        if (!validate(nodes)) {
            return null;
        }
        StringBuffer content = new StringBuffer();
        NumNode num = null;
        for (Node node : nodes) {
            content.append(node.getContent());
            if (num == null && node instanceof NumNode) {
                num = (NumNode) node;
            }
        }
        num.setContent(content);
        combineNode(num, nodes);
        return num;
    }

}
