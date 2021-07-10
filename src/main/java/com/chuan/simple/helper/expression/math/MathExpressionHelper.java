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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.exception.SimpleHelperException;
import com.chuan.simple.helper.expression.math.node.Node;

/**
 * <p>
 * Tools for parsing math expression.
 * <p>
 * Can be extended by
 * <pre>
 * {@code
 *  MathExpressionHelper.getContext().addNode(node ...);
 *  MathExpressionHelper.getContext().
 *  addCombiner(combiner,node class,node class ... );
 * }
 * </pre>
 *
 * <p>
 * The combining order of node depends on {@link Node#locate(Node)}
 * <p>
 * The creating priority depends on {@link Node#bornAt(Node)}
 *
 *
 * <p>
 * Math operation:+,-,*,/,%
 * <p>
 * Bit operation:~,|,^,&,<<,>>,>>>
 * <p>
 * Compare operation:>,<,>=,<=,!=,==
 * <p>
 * Bool operation:&,&&,|,||,^,!
 * <p>
 * The rationale of parsing math expression is:
 * <pre>
 *       +-----------+
 *       |           |
 *       |  NewNode  |
 *       |           |
 *       +-----^-----+
 *             |
 *       +-----+-----+
 *       |           |
 *       | Combiner  |
 *       |           |
 *       +-^-------^-+
 *         |       |
 * +-------+--+  +-+--------+
 * |          |  |          |
 * |   Node  XXXXXX  Node   |
 * |          |  |          |
 * +----------+  +----------+
 * </pre>
 *
 * @see MathExpressionContext#addCombiner
 * @see MathExpressionContext#addNode
 * @see MathExpressionContext#init
 * @see Node#locate(Node)
 * @see Node#bornAt(Node)
 *
 */
public final class MathExpressionHelper {

    private static final String EXPRESSION_FINDER =
            "([\\d\\+\\-\\*\\/\\^\\=\\!\\)\\(%><\\|\\&]+)([^\\d\\+\\-\\*\\/\\^\\=\\!\\)\\(%><\\|\\&]*)";

    public static MathExpressionContext context = new MathExpressionContext();

    /**
     * parse expression which contains math expressions.
     */
    public static Object parseComplexExpression(String expression) {
        Pattern p = Pattern.compile(EXPRESSION_FINDER);
        Matcher m = p.matcher(expression);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
        	String parsedResult = parseExpression(m.group(1));
        	if (StringHelper.isNotEmpty(parsedResult)) {
        		 result.append(parsedResult);
			}
            result.append(m.group(2));
        }
        return result.toString();
    }

    /**
     * parse math expression.
     */
    public static String parseExpression(String expression) {
        if (StringHelper.isEmpty(expression)) {
            throw new IllegalArgumentException(
                    "Invalid parameter " + expression);
        }
        try {
            Node node = null, prev = null;
            StringBuffer skipped = new StringBuffer();
            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if (skipped.length() > 0) {
                    skipped.append(c);
                }
                node = context.switchNodes(skipped, c, prev);
                if (node == null) {
                    if (skipped.length() == 0) {
                        skipped.append(c);
                    }
                    continue;
                }
                prev = node;
            }
            String result = null;
            if (node != null) {
                Node resultNode = node.combine();
                if (resultNode != null) {
                    result = resultNode.getContent().toString();
                }
            }
            return result;
        } catch (Exception e) {
            throw new SimpleHelperException(
                    "Parsing error in math expression " + expression, e);
        }

    }

    public static MathExpressionContext getContext() {
        return context;
    }

    public static void setContext(MathExpressionContext context) {
        MathExpressionHelper.context = context;
    }
    
    private MathExpressionHelper() {
    }


}
