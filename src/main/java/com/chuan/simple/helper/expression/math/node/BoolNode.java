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

public class BoolNode extends Node {

    public static final String TRUE = "true";

    public static final String FALSE = "false";

    public Boolean value = null;

    public BoolNode() {
    }

    public BoolNode(StringBuffer content) {
        this.setContent(content);
        if (TRUE.equals(content.toString())) {
            this.value = true;
        } else {
            this.value = false;
        }
    }

    @Override
    public boolean is(char c) {
        return false;
    }

    @Override
    public boolean is(StringBuffer s) {
        return TRUE.equals(s.toString()) || FALSE.equals(s.toString());
    }

    @Override
    public Node doCombine() {
        this.setCombined(true);
        return this;
    }

    @Override
    public Node born(char c) {
        return null;
    }

    @Override
    public Node born(StringBuffer content) {
        return new BoolNode(content);
    }

    public Boolean getValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.setContent(value.toString());
        this.value = value;
    }

}
