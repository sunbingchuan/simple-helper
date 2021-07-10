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
package com.chuan.simple.helper.expression.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chuan.simple.helper.common.ObjectHelper;
import com.chuan.simple.helper.expression.math.combiner.BitOperationCombiner;
import com.chuan.simple.helper.expression.math.combiner.BoolOperationCombiner;
import com.chuan.simple.helper.expression.math.combiner.Combiner;
import com.chuan.simple.helper.expression.math.combiner.CombinerIndex;
import com.chuan.simple.helper.expression.math.combiner.CompareOperationCombiner;
import com.chuan.simple.helper.expression.math.combiner.MathOperationCombiner;
import com.chuan.simple.helper.expression.math.combiner.NumCombiner;
import com.chuan.simple.helper.expression.math.node.AndNode;
import com.chuan.simple.helper.expression.math.node.BitNotNode;
import com.chuan.simple.helper.expression.math.node.BoolNode;
import com.chuan.simple.helper.expression.math.node.BoolNotNode;
import com.chuan.simple.helper.expression.math.node.BracketNode;
import com.chuan.simple.helper.expression.math.node.DivideNode;
import com.chuan.simple.helper.expression.math.node.EqualNode;
import com.chuan.simple.helper.expression.math.node.GreaterNode;
import com.chuan.simple.helper.expression.math.node.GreaterOrEqualNode;
import com.chuan.simple.helper.expression.math.node.LeftShiftNode;
import com.chuan.simple.helper.expression.math.node.LessNode;
import com.chuan.simple.helper.expression.math.node.LessOrEqualNode;
import com.chuan.simple.helper.expression.math.node.MinusNode;
import com.chuan.simple.helper.expression.math.node.ModNode;
import com.chuan.simple.helper.expression.math.node.MultiplyNode;
import com.chuan.simple.helper.expression.math.node.Node;
import com.chuan.simple.helper.expression.math.node.NotEqualNode;
import com.chuan.simple.helper.expression.math.node.NumNode;
import com.chuan.simple.helper.expression.math.node.OrNode;
import com.chuan.simple.helper.expression.math.node.PlusNode;
import com.chuan.simple.helper.expression.math.node.PointNode;
import com.chuan.simple.helper.expression.math.node.RightShiftNode;
import com.chuan.simple.helper.expression.math.node.SignNode;
import com.chuan.simple.helper.expression.math.node.XorNode;

/**
 * The context of math expression parser.
 */
public class MathExpressionContext {

    private static final Log LOG = LogFactory.getLog(MathExpressionContext.class);

    private final List<Object> nodes = new ArrayList<>();

    private final List<CombinerIndex> combinerIndexes = new ArrayList<>();

    private final Map<Class<?>, Integer> levelCache = new ConcurrentHashMap<>();

    public MathExpressionContext() {
        init();
    }

    public void addNode(Node... nodes) {
        levelCache.clear();
        for (Node node : nodes) {
            addNode(node);
        }
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public void addNode(Node node) {
        int index = nodes.size();
        x: for (int i = 0; i < nodes.size(); i++) {
            Object o = nodes.get(i);
            if (o instanceof List) {
                List<Node> layer = (List<Node>) o;
                for (Node lnd : layer) {
                    Integer locate = node.locate(lnd);
                    if (locate != null) {
                        if (locate == 0) {
                            layer.add(node);
                            return;
                        } 
                        index = i + locate;
                        break x;
                    }
                }
            } else {
                Node nd = (Node) o;
                Integer locate = node.locate(nd);
                if (locate != null) {
                    if (locate == 0) {
                        List<Node> list = new ArrayList<>();
                        list.add(nd);
                        list.add(node);
                        nodes.set(index = i, list);
                        return;
                    }
                    index = i + locate;
                    break x;
                }
            }
        }
        index = safeIndex(index);
        nodes.add(index, node);
    }

    private int safeIndex(int index) {
        if (index < 0) {
            index = 0;
        } else if (index > nodes.size()) {
            index = nodes.size();
        }
        return index;
    }

    public void addCombiner(Combiner combiner,
            @SuppressWarnings("unchecked") Class<? extends Node>... indexes) {
        CombinerIndex combinerIndex = null;
        List<CombinerIndex> tmpCombinerIndexes = combinerIndexes;
        for (int i = 0; i < indexes.length; i++) {
            Class<? extends Node> index = indexes[i];
            combinerIndex = getCombinerIndex(tmpCombinerIndexes, index);
            if (combinerIndex == null) {
                combinerIndex = new CombinerIndex(index);
                tmpCombinerIndexes.add(combinerIndex);
            }
            tmpCombinerIndexes = combinerIndex.getCombinerIndexes();
        }
        if (combinerIndex != null) {
            combinerIndex.setCombiner(combiner);
        }
    }

    public Node combine(Node node) {
        int i = 0, pi = 0;
        Combiner combiner = null;
        Node tmpNode = node;
        List<CombinerIndex> tmpCombinerIndexes = combinerIndexes;
        for ( CombinerIndex combinerIndex = null, prevCombinerIndex = null;
        		 tmpNode != null; i++, tmpNode = tmpNode.getNext()) {
            Class<? extends Node> index = tmpNode.getClass();
            combinerIndex = getCombinerIndex(tmpCombinerIndexes, index);
            if (combinerIndex == null) {
                break;
            }
            if (combinerIndex.getCombiner() != null) {
                prevCombinerIndex = combinerIndex;
                combiner = prevCombinerIndex.getCombiner();
                pi = i;
            }
            tmpCombinerIndexes = combinerIndex.getCombinerIndexes();
        }
        if (combiner != null) {
            if(LOG.isDebugEnabled())
            LOG.debug("Use combiner " + combiner + " to combine nodes "
                    + ObjectHelper.toString(getNodes(node, pi)));
            return combiner.combine(getNodes(node, pi));
        }
        return null;
    }

    private Node[] getNodes(Node node, int i) {
        Node[] nodes = new Node[i + 1];
        for (int j = 0; j < nodes.length; j++, node = node.getNext()) {
            nodes[j] = node;
        }
        return nodes;
    }

    private CombinerIndex getCombinerIndex(List<CombinerIndex> combinerIndexes,
            Class<? extends Node> index) {
        for (CombinerIndex i : combinerIndexes) {
            if (i.getIndex().equals(index)) {
                return i;
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked" })
    public Node switchNodes(StringBuffer skipped, char c, Node prev) {
        List<Node> matchNodes = new ArrayList<>();
        for (Object order : nodes) {
            if (order instanceof List) {
                List<Node> nodes = (List<Node>) order;
                for (Node node : nodes) {
                    node.setPrev(prev);
                    if (nodeIs(node, skipped, c)) {
                        matchNodes.add(node);
                    }
                }
            } else {
                Node node = (Node) order;
                node.setPrev(prev);
                if (nodeIs(node, skipped, c)) {
                    matchNodes.add(node);
                }
            }
        }
        Node nextBorn = nextBorn(matchNodes);
        if (nextBorn == null) {
            return null;
        }
        return born(nextBorn, skipped, prev, c);
    }
    
    private Node nextBorn(List<Node> matchNodes) {
        Node nextBorn = null;
        for (Node node : matchNodes) {
            if (nextBorn == null) {
                nextBorn = node;
            } else {
                if (node.bornAt(nextBorn) - nextBorn.bornAt(node) > 0) {
                    nextBorn = node;
                }
            }
        }
        return nextBorn;
    }

    private static boolean nodeIs(Node node, StringBuffer skipped, char c) {
        if (skipped.length() > 0) {
            return node.is(skipped);
        }
        return node.is(c);
    }

    private static Node born(Node node, StringBuffer skipped, Node prev,
            char c) {
        Node result = null;
        if (skipped.length() > 0) {
            result = node.born(prev, skipped);
            skipped.delete(0, skipped.length());
        } else {
            result = node.born(prev, c);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void init() {
        addNode(new BracketNode());

        addNode(new NumNode(), new PointNode());

        addNode(new BitNotNode(), new SignNode());
        addNode(new MultiplyNode(), new DivideNode(), new ModNode());
        addNode(new PlusNode(), new MinusNode());
        addNode(new LeftShiftNode(), new RightShiftNode());

        addNode(new NotEqualNode(), new EqualNode(), new GreaterOrEqualNode(),
                new GreaterNode(), new LessOrEqualNode(), new LessNode());

        addNode(new BoolNode());
        addNode(new BoolNotNode());
        addNode(new AndNode(), new XorNode(), new OrNode());

        // Combiner
        // Num
        addCombiner(new NumCombiner(), NumNode.class, NumNode.class);
        addCombiner(new NumCombiner(), NumNode.class, PointNode.class);
        addCombiner(new NumCombiner(), PointNode.class, NumNode.class);
        // Math
        addCombiner(new MathOperationCombiner(), SignNode.class, NumNode.class);
        addCombiner(new MathOperationCombiner(), NumNode.class, PlusNode.class,
                NumNode.class);
        addCombiner(new MathOperationCombiner(), NumNode.class, MinusNode.class,
                NumNode.class);
        addCombiner(new MathOperationCombiner(), NumNode.class,
                MultiplyNode.class, NumNode.class);
        addCombiner(new MathOperationCombiner(), NumNode.class,
                DivideNode.class, NumNode.class);
        addCombiner(new MathOperationCombiner(), NumNode.class, ModNode.class,
                NumNode.class);
        // Bit
        addCombiner(new BitOperationCombiner(), BitNotNode.class,
                NumNode.class);
        addCombiner(new BitOperationCombiner(), NumNode.class, AndNode.class,
                NumNode.class);
        addCombiner(new BitOperationCombiner(), NumNode.class, OrNode.class,
                NumNode.class);
        addCombiner(new BitOperationCombiner(), NumNode.class, XorNode.class,
                NumNode.class);
        addCombiner(new BitOperationCombiner(), NumNode.class,
                LeftShiftNode.class, NumNode.class);
        addCombiner(new BitOperationCombiner(), NumNode.class,
                RightShiftNode.class, NumNode.class);
        // Bool
        addCombiner(new BoolOperationCombiner(), BoolNotNode.class,
                BoolNode.class);
        addCombiner(new BoolOperationCombiner(), BoolNode.class, AndNode.class,
                BoolNode.class);
        addCombiner(new BoolOperationCombiner(), BoolNode.class, OrNode.class,
                BoolNode.class);
        addCombiner(new BoolOperationCombiner(), BoolNode.class, XorNode.class,
                BoolNode.class);
        // Compare
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                GreaterNode.class, NumNode.class);
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                GreaterOrEqualNode.class, NumNode.class);
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                LessNode.class, NumNode.class);
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                LessOrEqualNode.class, NumNode.class);
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                NotEqualNode.class, NumNode.class);
        addCombiner(new CompareOperationCombiner(), NumNode.class,
                EqualNode.class, NumNode.class);

    }

    @SuppressWarnings("rawtypes")
    public int getLevel(Class<?> nodeClass) {
        Integer level = levelCache.get(nodeClass);
        if (level != null) {
            return level;
        }
        level = 0;
        a: for (int i = 0; i < nodes.size(); i++) {
            Object node = nodes.get(i);
            if (node.getClass().equals(nodeClass)) {
                level = i;
                break;
            } else if (node instanceof List) {
                for (Object nd : (List) node) {
                    if (nd.getClass().equals(nodeClass)) {
                        level = i;
                        break a;
                    }
                }
            }
        }
        levelCache.put(nodeClass, level);
        return level;
    }

    public void setScale(int scale) {
        for (CombinerIndex index : combinerIndexes) {
            setScale(index, scale);
        }
    }

    private void setScale(CombinerIndex index, int scale) {
        if (index.getCombiner() != null) {
            index.getCombiner().setScale(scale);
        }
        for (CombinerIndex i : index.getCombinerIndexes()) {
            setScale(i, scale);
        }
    }

    public void setRoundingMode(int roundingMode) {
        for (CombinerIndex index : combinerIndexes) {
            setRoundingMode(index, roundingMode);
        }
    }

    private void setRoundingMode(CombinerIndex index, int roundingMode) {
        if (index.getCombiner() != null) {
            index.getCombiner().setRoundingMode(roundingMode);
        }
        for (CombinerIndex i : index.getCombinerIndexes()) {
            setRoundingMode(i, roundingMode);
        }
    }
}
