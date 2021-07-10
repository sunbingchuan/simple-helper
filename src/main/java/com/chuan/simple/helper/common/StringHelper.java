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
package com.chuan.simple.helper.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class StringHelper {

    private static final Log LOG = LogFactory.getLog(StringHelper.class);

    /** Prefix for system property placeholders: "${". */
    public static final String PLACEHOLDER_PREFIX_DOLLAR_BRACES = "${";

    /** Suffix for system property placeholders: "}". */
    public static final String PLACEHOLDER_SUFFIX_BRACES = "}";

    public static final String COMMA = ",";

    public static final String VERTICAL_LINE = "|";

    public static final String EMPTY = "";
    
    public static final String[] ARRAY_EMPTY = new String[0];

    /** Suffix for array class names: {@code "[]"}. */
    public static final String ARRAY_SUFFIX = "[]";

    /** The package separator character: {@code '.'}. */
    private static final char PACKAGE_SEPARATOR = '.';

    /** The path separator character: {@code '/'}. */
    private static final char PATH_SEPARATOR = '/';

    /** The CGLIB class separator: {@code "$$"}. */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";

    /** The ".class" file suffix. */
    public static final String CLASS_FILE_SUFFIX = ".class";

    private StringHelper() {
    }

    public static boolean hasText(String str) {
        if (str != null && str.length() > 0)
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
        return false;
    }

    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }

    public static String[] splitByDelimiter(String str, String delimiter) {
        List<String> result = new ArrayList<>();
        int index = -1, pos = 0;
        if ((index = str.indexOf(delimiter)) < 0) {
            return new String[] {str};
        }
        while ((index = str.indexOf(delimiter, pos)) >= 0) {
            if (index == 0) {
                pos = index + delimiter.length();
                continue;
            }
            result.add(str.substring(pos, index));
            pos = index + delimiter.length();
        }
        if (pos <= str.length()) {
            result.add(str.substring(pos));
        }
        return result.toArray(new String[result.size()]);
    }

    public static String[] splitByDelimiters(String str, String delimiters) {
        return splitByDelimiters(str, delimiters, true, true);
    }

    public static String[] splitByDelimiters(String str, String delimiters,
            boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null) {
            return ARRAY_EMPTY;
        }
        StringTokenizer tokenizer = new StringTokenizer(str, delimiters);
        List<String> tkns = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String tkn = tokenizer.nextToken();
            if (trimTokens) {
                tkn = tkn.trim();
            }
            if (!ignoreEmptyTokens || tkn.length() > 0) {
                tkns.add(tkn);
            }
        }
        return tkns.toArray(new String[tkns.size()]);
    }


    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean match(String[] patterns, String str) {
        for (String pattern : patterns) {
            if (match(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean match(String pattern, String str) {
        return Pattern.matches(pattern, str);
    }

    public static String letter(String str) {
        if (isEmpty(str)) {
            return EMPTY;
        }
        StringBuffer sb = new StringBuffer();
        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String classNameToResourcePath(String className) {
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    public static String replace(String targetString, String oldString,
            String newString) {
        if (!hasLength(targetString) || !hasLength(oldString)
                || newString == null) {
            return targetString;
        }
        int index = targetString.indexOf(oldString);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return targetString;
        }
        StringBuilder sb = new StringBuilder();

        int pos = 0; //our position in the old string
        int patLen = oldString.length();
        while (index >= 0) {
            sb.append(targetString.substring(pos, index));
            sb.append(newString);
            pos = index + patLen;
            index = targetString.indexOf(oldString, pos);
        }

        // append any characters to the right of a match
        sb.append(targetString.substring(pos));
        return sb.toString();
    }

    public static Integer switchInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer
                || int.class.isAssignableFrom(obj.getClass())) {
            return (Integer) obj;
        }
        try {
            String objStr = obj.toString();
            if (isNotEmpty(objStr)) {
                return Integer.parseInt(objStr);
            }
        } catch (Exception e) {
            LOG.debug("Switch '" + obj + "' to integer failed", e);
        }
        return null;
    }

    public static Boolean switchBoolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean
                || boolean.class.isAssignableFrom(obj.getClass())) {
            return (Boolean) obj;
        }
        try {
            return Boolean.parseBoolean(obj.toString());
        } catch (Exception e) {
            LOG.debug("Switch '" + obj + "' to integer failed", e);
            return null;
        }
    }

    public static String toDelimitedString(Collection<?> coll, String delim) {
        if (ObjectHelper.isEmpty(coll)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    public static boolean equals(String a, String b) {
        if (a == null || b == null) {
            if (a == b) {
                return true;
            }
            return false;
        }
        return a.equals(b);
    }

}
