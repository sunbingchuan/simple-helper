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

import java.math.BigDecimal;

public class NumNode extends Node {

    private static final String radixChars = "abcdefxld";

    private boolean isLong;

    private static final String HEX_PREFIX = "0x";

    private static final String OCT_PREFIX = "0";

    private static final String BIN_PREFIX = "0b";

    private static final char LONG_SIFFIX = 'l';

    private Boolean radixChanged = false;

    public NumNode(char c) {
        content.append(c);
    }

    public NumNode() {
    }

    @Override
    protected Node doCombine() {
        if (this.isCombined()) {
            return this;
        }
        Node node = this;
        while (node!=null&&node.getPrev() != null
                && node.getPrev().getLevel() == node.getLevel()) {
            node = node.context.combine(node.getPrev());
        }
        while (node!=null&&node.getNext() != null
                && node.getNext().getLevel() == node.getLevel()) {
            node = node.context.combine(node);
        }
        if (node!=null) {
            node.setCombined(true);
		}
        return node;
    }

    @Override
    public boolean is(char c) {
        if (Character.isDigit(c)) {
            return true;
        }
        if (radixChars.indexOf(Character.toLowerCase(c)) >= 0
                && this.getPrev() instanceof NumNode) {
            return true;
        }
        return false;
    }

    @Override
    public Node born(char c) {
        return new NumNode(c);
    }

    public BigDecimal getBigDecimal() {
        changeRadix();
        return new BigDecimal(this.content.toString());
    }

    public Long getLong() {
        changeRadix();
        return Long.valueOf(this.content.toString());
    }

    public Integer getInteger() {
        changeRadix();
        if (isLong) {
            return null;
        }
        Long l = Long.valueOf(this.content.toString());
        return matchInteger(l);
    }

    private Integer matchInteger(Long number) {
        Integer i = null;
        if (Integer.MIN_VALUE <= number && number <= Integer.MAX_VALUE) {
            i = number.intValue();
        }
        return i;
    }

    private void changeRadix() {
        dealSuffix();
        if (radixChanged) {
            return;
        }
        synchronized (radixChanged) {
            if (radixChanged) {
                return;
            }
            if (this.content.length() < 2) {
                radixChanged = true;
                return;
            }
            String prefix = this.content.substring(0, 2);
            if (HEX_PREFIX.equalsIgnoreCase(prefix)) {
                this.content.delete(0, 2);
                Long result = Long.parseLong(this.content.toString(), 16);
                setContent(result.toString());
            } else if (BIN_PREFIX.equalsIgnoreCase(prefix)) {
                this.content.delete(0, 2);
                Long result = Long.parseLong(this.content.toString(), 2);
                setContent(result.toString());
            } else if (this.content.indexOf(OCT_PREFIX) == 0) {
                this.content.delete(0, 1);
                Long result = Long.parseLong(this.content.toString(), 8);
                setContent(result.toString());
            }
            radixChanged = true;
        }
    }

    private void dealSuffix() {
        char suffix = this.content.charAt(this.content.length() - 1);
        if (Character.isDigit(suffix)) {
            return;
        }
        if (LONG_SIFFIX == Character.toLowerCase(suffix)) {
            this.isLong = true;
        }
        this.content.deleteCharAt(this.content.length() - 1);
    }

    public boolean isLong() {
        return isLong;
    }

    public void setLong(boolean isLong) {
        this.isLong = isLong;
    }

}
