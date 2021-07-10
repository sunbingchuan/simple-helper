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

import java.util.ArrayList;
import java.util.List;

import com.chuan.simple.helper.expression.math.node.Node;

public class CombinerIndex {

	private final List<CombinerIndex> combinerIndexes = new ArrayList<>();

	private Class<? extends Node> index;

    private Combiner combiner;

    public CombinerIndex(Class<? extends Node> index) {
        this.index = index;
    }

    public Class<? extends Node> getIndex() {
        return index;
    }

    public void setIndex(Class<? extends Node> index) {
        this.index = index;
    }

    public List<CombinerIndex> getCombinerIndexes() {
        return combinerIndexes;
    }

    public void addCombinerIndex(CombinerIndex... indexes) {
        for (CombinerIndex index : indexes) {
            combinerIndexes.add(index);
        }
    }

    public Combiner getCombiner() {
        return combiner;
    }

    public void setCombiner(Combiner combiner) {
        this.combiner = combiner;
    }

}
