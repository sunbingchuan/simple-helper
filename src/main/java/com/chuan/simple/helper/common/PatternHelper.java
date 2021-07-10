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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * The basic matching rule is '*' equals regex '.*', '?' equals regex '.'
 * <p>
 * '**' matches contains separator '/','..' matches contains separator '.' .
 */
public final class PatternHelper {

    /** Default path separator: "/". */
    public static final String DEFAULT_PATH_SEPARATOR = "/";

    public static final String DEFAULT_NAME_SEPARATOR = ".";

    public static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\{[^/]+?\\}");

    public static final String WILDCARD_CHARS = "*?{";

    public static final Pattern GLOB_PATTERN = Pattern
            .compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

    public static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

    public static final String ANY_PATTERN = "*";

    public static final String DOUBLE_ASTERISK = "**";

    public static final String DOT_DOUBLE_ASTERISK = ".**.";

    public static final String DOUBLE_DOT = "..";

    /**
     * Match path with '/',like aa.bb.cc.Match will be done after split by '/' .
     */
    public static boolean matchPath(String pattern, String path) {
        return doMatch(pattern, path, true, null, DEFAULT_PATH_SEPARATOR);
    }

    /**
     * If the path matches the start part of the pattern.
     */
    public static boolean matchStart(String pattern, String path) {
        return doMatch(pattern, path, false, null, DEFAULT_PATH_SEPARATOR);
    }

    /**
     * Match name with '.',like aa.bb.cc.Match will be done after split by '.' .
     */
    public static boolean matchName(String pattern, String name) {
        if (ANY_PATTERN.equals(pattern)) {
            return true;
        }
        pattern = pattern.replaceAll(Pattern.quote(DOUBLE_DOT),
                DOT_DOUBLE_ASTERISK);
        return doMatch(pattern, name, true, null, DEFAULT_NAME_SEPARATOR);
    }

    private static boolean doMatch(String pattern, String path,
            boolean fullMatch, Map<String, String> uriTemplateVariables,
            String pathSeparator) {

        if (path.startsWith(pathSeparator) != pattern
                .startsWith(pathSeparator)) {
            return false;
        }

        String[] patternDirs = StringHelper.splitByDelimiters(pattern,
                pathSeparator, false, true);
        if (fullMatch && !isPotentialMatch(path, patternDirs, pathSeparator)) {
            return false;
        }

        String[] pathDirs = StringHelper.splitByDelimiters(path, pathSeparator,
                false, true);

        int patternIndexStart = 0;
        int patternIndexEnd = patternDirs.length - 1;
        int pathIndexStart = 0;
        int pathIdxEnd = pathDirs.length - 1;

        // Match all elements up to the first **
        while (patternIndexStart <= patternIndexEnd && pathIndexStart <= pathIdxEnd) {
            String pattDir = patternDirs[patternIndexStart];
            if (DOUBLE_ASTERISK.equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIndexStart],
                    uriTemplateVariables)) {
                return false;
            }
            patternIndexStart++;
            pathIndexStart++;
        }

        if (pathIndexStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (patternIndexStart > patternIndexEnd) {
                return pattern.endsWith(pathSeparator) == path
                        .endsWith(pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (patternIndexStart == patternIndexEnd && patternDirs[patternIndexStart].equals("*")
                    && path.endsWith(pathSeparator)) {
                return true;
            }
            for (int i = patternIndexStart; i <= patternIndexEnd; i++) {
                if (!patternDirs[i].equals(DOUBLE_ASTERISK)) {
                    return false;
                }
            }
            return true;
        } else if (patternIndexStart > patternIndexEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else if (!fullMatch
                && DOUBLE_ASTERISK.equals(patternDirs[patternIndexStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }

        // up to last '**'
        while (patternIndexStart <= patternIndexEnd && pathIndexStart <= pathIdxEnd) {
            String pattDir = patternDirs[patternIndexEnd];
            if (pattDir.equals(DOUBLE_ASTERISK)) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxEnd],
                    uriTemplateVariables)) {
                return false;
            }
            patternIndexEnd--;
            pathIdxEnd--;
        }
        if (pathIndexStart > pathIdxEnd) {
            // String is exhausted
            for (int i = patternIndexStart; i <= patternIndexEnd; i++) {
                if (!patternDirs[i].equals(DOUBLE_ASTERISK)) {
                    return false;
                }
            }
            return true;
        }
        // match the strs between '**' s
        while (patternIndexStart != patternIndexEnd && pathIndexStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patternIndexStart + 1; i <= patternIndexEnd; i++) {
                if (patternDirs[i].equals(DOUBLE_ASTERISK)) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patternIndexStart + 1) {
                // '**/**' situation, so skip one
                patternIndexStart++;
                continue;
            }
            // Find the pattern between patIdxStart & patIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = patIdxTmp - patternIndexStart - 1;
            int strLength = pathIdxEnd - pathIndexStart + 1;
            int foundIdx = -1;

            strLoop: for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patternDirs[patternIndexStart + j + 1];
                    String subStr = pathDirs[pathIndexStart + i + j];
                    if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIndexStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patternIndexStart = patIdxTmp;
            pathIndexStart = foundIdx + patLength;
        }

        for (int i = patternIndexStart; i <= patternIndexEnd; i++) {
            if (!patternDirs[i].equals(DOUBLE_ASTERISK)) {
                return false;
            }
        }

        return true;
    }

    private static int skipSeparator(String path, int pos, String separator) {
        int skipped = 0;
        while (path.startsWith(separator, pos + skipped)) {
            skipped += separator.length();
        }
        return skipped;
    }

    private static int passSegment(String path, int pos, String pfx) {
        int passed = 0;
        for (int i = 0; i < pfx.length(); i++) {
            char c = pfx.charAt(i);
            if (isWildcardChar(c)) {
                return passed;
            }
            int currPos = pos + passed;
            if (currPos >= path.length()) {
                return 0;
            }
            if (c == path.charAt(currPos)) {
                passed++;
            }
        }
        return passed;
    }

    private static boolean isWildcardChar(char c) {
        if (WILDCARD_CHARS.indexOf(c)>=0) {
            return true;
        }
        return false;
    }

    private static boolean isPotentialMatch(String path, String[] pattDirs,
            String pathSeparator) {
        int pos = 0;
        for (String pattDir : pattDirs) {
            int skipped = skipSeparator(path, pos, pathSeparator);
            pos += skipped;
            skipped = passSegment(path, pos, pattDir);
            if (skipped < pattDir.length()) {
                return isWildcardChar(pattDir.charAt(skipped));
            }
            pos += skipped;
        }
        return true;
    }
    
    /**
     * @see PatternHelper#matchStrings(String, String, Map)
     */
    public static boolean matchStrings(String pat, String str) {
        return matchStrings(pat, str, null);
    }

    /**
     * @param pat
     *            pattern
     * @param str
     *            the string to be matched
     * @param uriTemplateVariables
     *            the map to save uriTemplateVariables. e.g. xx{yy} matches
     *            xxmnz then the uriTemplateVariables will contain yy=mnz.
     *            <p>
     *            Note:Adjacent characters of '{' or '}' should not be wildcard
     *            char (* or ?).
     * @return
     */
    public static boolean matchStrings(String pat, String str,
            Map<String, String> uriTemplateVariables) {
        List<String> variableNames = new ArrayList<>();
        Pattern pattern = makeGlobalPattern(pat, variableNames);
        return matchStrings(pattern, str, variableNames, uriTemplateVariables);
    }

    private static boolean matchStrings(Pattern pattern, String str,
            List<String> variableNames,
            Map<String, String> uriTemplateVariables) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            if (uriTemplateVariables != null) {
                if (variableNames.size() != matcher.groupCount()) {
                    throw new IllegalArgumentException(
                            "The number of capturing groups in the pattern segment "
                                    + pattern
                                    + " does not match the number of URI template variables it defines, "
                                    + "which can occur if capturing groups are used in a URI template regex. "
                                    + "Use non-capturing groups instead.");
                }
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String name = variableNames.get(i - 1);
                    String value = matcher.group(i);
                    uriTemplateVariables.put(name, value);
                }
            }
            return true;
        }
        return false;
        
    }

    private static Pattern makeGlobalPattern(String pattern,
            List<String> variableNames) {
        StringBuilder patternBuilder = new StringBuilder();
        Matcher matcher = GLOB_PATTERN.matcher(pattern);
        int end = 0;
        while (matcher.find()) {
            patternBuilder.append(quote(pattern, end, matcher.start()));
            String matchString = matcher.group();
            if ("?".equals(matchString)) {
                patternBuilder.append(".");
            } else if ("*".equals(matchString)) {
                patternBuilder.append(".*");
            } else if (matchString.startsWith("{") && matchString.endsWith("}")) {
                int colonIdx = matchString.indexOf(':');
                if (colonIdx == -1) {
                    patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                    variableNames.add(matcher.group(1));
                } else {
                    String variablePattern =
                            matchString.substring(colonIdx + 1, matchString.length() - 1);
                    patternBuilder.append("(");
                    patternBuilder.append(variablePattern);
                    patternBuilder.append(")");
                    String variableName = matchString.substring(1, colonIdx);
                    variableNames.add(variableName);
                }
            }
            end = matcher.end();
        }
        patternBuilder.append(quote(pattern, end, pattern.length()));
        return Pattern.compile(patternBuilder.toString());
    }

    public static String quote(String s, int start, int end) {
        if (start == end) {
            return "";
        }
        return Pattern.quote(s.substring(start, end));
    }

    private PatternHelper() {
    }
    
}
