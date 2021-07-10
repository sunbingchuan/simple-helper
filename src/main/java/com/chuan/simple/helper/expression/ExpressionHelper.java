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
package com.chuan.simple.helper.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chuan.simple.helper.common.StringHelper;

/**
 * Tools for resolving expressions such as place holder.
 */
public final class ExpressionHelper {

    /** Prefix for system property placeholders: "${". */
    public static final String PLACEHOLDER_PREFIX_DOLLAR_BRACES = "${";

    /** Suffix for system property placeholders: "}". */
    public static final String PLACEHOLDER_SUFFIX_BRACES = "}";

    private ExpressionHelper() {
    }
    /**
     * @see #resolvePlaceholders(String, Map, String, String)
     */
    public static String resolvePlaceholders(String text,
            Map<?, ?> properties) {
        return resolvePlaceholders(text, properties,
                PLACEHOLDER_PREFIX_DOLLAR_BRACES, PLACEHOLDER_SUFFIX_BRACES);
    }

    /**
     * Resolve placeholders like ${xxx} which will be replace by values in
     * {@code properties}.
     * @param text
     *            the text contains placeholders
     * @param properties
     *            properties map
     * @param placeholderPrefix
     *            placeholder prefix like
     *            {@link #PLACEHOLDER_PREFIX_DOLLAR_BRACES}
     * @param placeholderSuffix
     *            placeholder suffix like {@link #PLACEHOLDER_SUFFIX_BRACES}
     */
    public static String resolvePlaceholders(String text, Map<?, ?> properties,
            String placeholderPrefix, String placeholderSuffix) {
        if (StringHelper.isEmpty(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        int startIndex = -1, endIndex = -1;
        while ((startIndex =
                result.indexOf(placeholderPrefix, endIndex)) >= 0) {
            endIndex = findMatchEndIndex(result,
                    startIndex + placeholderPrefix.length(), placeholderPrefix,
                    placeholderSuffix);
            if (endIndex < 0) {
                throw new IllegalArgumentException(text);
            }
            String placeHolder = result.substring(
                    startIndex + placeholderPrefix.length(), endIndex);
            placeHolder = resolvePlaceholders(placeHolder, properties,
                    placeholderPrefix, placeholderSuffix);
            String value = (String) properties.get(placeHolder);
            if (StringHelper.isNotEmpty(value)) {
                result.replace(startIndex,
                        endIndex + placeholderSuffix.length(), value);
                endIndex = startIndex + value.length();
            } else {
                result.replace(startIndex + placeholderPrefix.length(),
                        endIndex, placeHolder);
                endIndex  += placeholderSuffix.length();
            }
        }
        return result.toString();
    }

    /**
     * @see #resolvePlaceholders(String, Map)
     */
    public static List<String> resolvePlaceholders(List<String> texts,
            Map<?, ?> properties) {
        List<String> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(resolvePlaceholders(text, properties));
        }
        return result;
    }

    /**
     * @see #resolvePlaceholders(String, Map)
     */
    public static String[] resolvePlaceholders(String[] texts,
            Map<?, ?> properties) {
        String[] result = new String[texts.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = resolvePlaceholders(texts[i], properties);
        }
        return result;
    }

    private static int findMatchEndIndex(CharSequence str, int startIndex,
            String placeholderPrefix, String placeholderSuffix) {
        for (int i = startIndex,subPlaceholderCount = 0; i < str.length(); i++) {
            if (subStringMatch(str, i, placeholderPrefix)) {
                subPlaceholderCount++;
            }
            if (subStringMatch(str, i, placeholderSuffix)) {
                if (subPlaceholderCount == 0) {
                    return i;
                }
                subPlaceholderCount--;
            }
        }
        return -1;
    }

    private static boolean subStringMatch(CharSequence s, int fromIndex,
            CharSequence substring) {
        if (fromIndex + substring.length() > s.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (s.charAt(fromIndex + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

}
