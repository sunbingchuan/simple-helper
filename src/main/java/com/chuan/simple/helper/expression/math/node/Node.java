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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.chuan.simple.helper.expression.math.MathExpressionContext;
import com.chuan.simple.helper.expression.math.MathExpressionHelper;

public abstract class Node {

    protected MathExpressionContext context = MathExpressionHelper.getContext();

    protected StringBuffer content = new StringBuffer();

    protected Node prev, next;

    protected volatile Boolean combined = false;

    protected int level = -1;

    protected Node() {
    }

    public abstract boolean is(char c);

    public boolean is(StringBuffer s) {
        return false;
    }

    public Node combine() {
        List<Node> nodes = sortAdjacentNode(this);
        for (Node node : nodes) {
            if (node.isCombined()) {
                continue;
            }
            return combineNode(node);
        }
        return this;
    }

    protected abstract Node doCombine();

    private Node combineNode(Node node) {
        if (this==node) {
            node = node.doCombine();
        }
        return node.combine();
    }
    
    private List<Node> sortAdjacentNode(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        if (node.prev != null) {
            nodes.add(node.prev);
        }
        if (node.next != null) {
            nodes.add(node.next);
        }
        Collections.sort(nodes, new NodeComparator());
        return nodes;
    }
    
 

    /**
     * @param node
     *            the referential node
     * @return level offset (0 means same level/{@code null} means can't be
     *         referenced by this {@code node})
     */
    public Integer locate(Node node) {
        return null;
    }

    /**
     * Each node can override this method to control the born order where more
     * than one {@link Node#is } match. If has conflict ,the first node present
     * will be born.
     * @param node
     * @return 1 means before,0 means same , -1 after
     */
    public Integer bornAt(Node node) {
        return 0;
    }

    public int getLevel() {
        if (level < 0) {
            level = this.context.getLevel(this.getClass());
        }
        return level;
    }

    public Node getPrev() {
        return prev;
    }

    public void setPrev(Node prev) {
        this.prev = prev;
        if (prev != null) {
            prev.next = this;
        }
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
        if (next != null) {
            next.prev = this;
        }
    }

    public Node born(Node prev, char c) {
        Node node = born(c);
        node.setPrev(prev);
        return node;
    }

    public Node born(Node prev, StringBuffer content) {
        Node node = born(content);
        node.setPrev(prev);
        return node;
    }

    protected abstract Node born(char c);

    protected Node born(StringBuffer content) {
        return null;
    }


	protected Node doCombinePrefixNode( Class<? extends Node>...
    		skippedPrifixNodeClasses) {
        for (Node node = this.next; node != null; node = node.next) {
            if (!isInstanceOf(node, skippedPrifixNodeClasses)) {
                if (node.isCombined()) {
                    return this.context.combine(node.prev);
                }
                return node.combine();
            }
        }
        return null;
    }
    
    private boolean isInstanceOf(Node node ,Class<? extends Node>[] classes){
    	for (Class<? extends Node> clazz : classes) {
    		if (clazz.isInstance(node)) {
			return true;
		}
	}
    	return false;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + content.toString();
    }

    public StringBuffer getContent() {
        return content;
    }

    public void setContent(StringBuffer content) {
        this.content.delete(0, this.content.length());
        this.content.append(content);
    }

    public void setContent(String content) {
        this.content.delete(0, this.content.length());
        this.content.append(content);
    }

    public Boolean isCombined() {
        return combined;
    }

    public void setCombined(Boolean combined) {
        this.combined = combined;
    }

    private static class NodeComparator implements Comparator<Node>{
        @Override
        public int compare(Node a, Node b) {
            return a.getLevel() - b.getLevel();
        }
        
    }
    
}
