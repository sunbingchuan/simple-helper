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
import com.chuan.simple.helper.expression.math.node.BitNotNode;
import com.chuan.simple.helper.expression.math.node.LeftShiftNode;
import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.NumNode;
import com.chuan.simple.helper.expression.math.node.OrNode;
import com.chuan.simple.helper.expression.math.node.RightShiftNode;
import com.chuan.simple.helper.expression.math.node.XorNode;

public class BitOperationCombiner extends Combiner {

    @Override
    public NumNode combine(Node... nodes) {
        if (nodes[0] instanceof BitNotNode) {
            NumNode num = (NumNode) nodes[1];
            Integer inumber = num.getInteger();
            if (inumber!=null) {
            	  inumber = ~inumber;
                  num.setContent(inumber.toString());
                  combineNode(num, nodes);
                  return num;
			}
			Long number = num.getLong();
			number = ~number;
            num.setContent(number.toString());
            combineNode(num, nodes);
            return num;
          
        }
        NumNode a = (NumNode) nodes[0];
        NumNode b = (NumNode) nodes[2];
        Node operation = nodes[1];
        Long numberA = a.getLong();
        Integer inumberA = a.getInteger();
        Long numberB = b.getLong();
        Integer inumberB = b.getInteger();
        if (operation instanceof OrNode) {
            numberA = (inumberA != null ? inumberA : numberA) | 
            		(inumberB != null ? inumberB : numberB);
        } else if (operation instanceof XorNode) {
            numberA = (inumberA != null ? inumberA : numberA)
                    ^ (inumberB != null ? inumberB : numberB);
        } else if (operation instanceof AndNode) {
            numberA = (inumberA != null ? inumberA : numberA)
                    & (inumberB != null ? inumberB : numberB);
        } else if (operation instanceof LeftShiftNode) {
            numberA = (inumberA != null ? inumberA : numberA) << 
            		(inumberB != null ? inumberB : numberB);
        } else if (operation instanceof RightShiftNode) {
            RightShiftNode rightShiftNode = (RightShiftNode) operation;
            if (rightShiftNode.isUnSigned()) {
                numberA = (inumberA != null ? inumberA : numberA) >>> 
            				(inumberB != null ? inumberB : numberB);
            }
            numberA = (inumberA != null ? inumberA : numberA) >> 
            			(inumberB != null ? inumberB : numberB);
        } else {
            return null;
        }
        a.setContent(numberA.toString());
        combineNode(a, nodes);
        return a;
    }

}
