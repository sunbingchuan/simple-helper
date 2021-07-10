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
package com.chuan.simple.helper.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.proxy.BaseProxy;

/**
 * Tools for dealing with path.
 */
public final class PathHelper extends BaseProxy{

    public static final String FOLDER_SEPARATOR = "/";

    public static final char FOLDER_SEPARATOR_CHAR = '/';

    public static final String WINDOWS_FOLDER_SEPARATOR = "\\\\";

    public static final String TOP_PATH = "..";

    public static final String CURRENT_PATH = ".";

    public static final String COLON = ":";
    
    private static final Pattern URL_PATTERN= Pattern.
            compile("(https?|ftp|file)://[-\\w+&@#/%?=~|!:,.;]+[-\\w+&@#/%=~|]");
    
    /**
     * @param path
     * @return the clean path without '..' or '\'(replaced by '/')
     */
    public static String cleanPath(String path) {
        if (!StringHelper.hasLength(path)) {
            return path;
        }
        String pathToUse =
                path.replaceAll(WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);
        pathToUse = delDupSep(pathToUse);
        if (!pathToUse.contains(CURRENT_PATH)) {
            return pathToUse;
        }
        int pfxIdx = pathToUse.indexOf(COLON);
        String pfx = "";
        if (pfxIdx != -1) {
            pfx = pathToUse.substring(0, pfxIdx + 1);
            if (pfx.contains(FOLDER_SEPARATOR)) {
                pfx = "";
            } else {
                pathToUse = pathToUse.substring(pfxIdx + 1);
            }
        }
        if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
            pfx = pfx + FOLDER_SEPARATOR;
            pathToUse = pathToUse.substring(1);
        }
        String[] pathArray =
                StringHelper.splitByDelimiter(pathToUse, FOLDER_SEPARATOR);
        LinkedList<String> pathElements = new LinkedList<>();
        int parents = 0;
        for (int i = pathArray.length - 1; i >= 0; i--) {
            String element = pathArray[i];
            if (CURRENT_PATH.equals(element)) {
                continue;
            }
            if (TOP_PATH.equals(element)) {
                parents++;
                continue;
            }
            if (parents > 0) {
                parents--;
            } else {
                pathElements.add(0, element);
            }
        }
        for (int i = 0; i < parents; i++) {
            pathElements.add(0, TOP_PATH);
        }
        if (pathElements.size() == 1 && "".equals(pathElements.getLast())
                && !pfx.endsWith(FOLDER_SEPARATOR)) {
            pathElements.add(0, CURRENT_PATH);
        }
        StringJoiner joiner = new StringJoiner(FOLDER_SEPARATOR);
        for (String cs : pathElements) {
            joiner.add(cs);
        }
        return pfx + joiner.toString();
    }

    private static String delDupSep(String path) {
        char[] array = path.toCharArray();
        boolean prevSeparator = false;
        StringBuffer result = new StringBuffer();
        for (char c : array) {
            if (c == FOLDER_SEPARATOR_CHAR) {
            	 if (!prevSeparator) {
                     prevSeparator = true;
                     result.append(c);
                 }
            } else {
            	result.append(c);
                prevSeparator = false;
            }
        }
        return result.toString();
    }

    public static String getSimpleNameByPath(String path) {
        path = cleanPath(path);
        if (path.indexOf(FOLDER_SEPARATOR) >= 0) {
            return path.substring(path.lastIndexOf(FOLDER_SEPARATOR));
        }
        return path;
    }

    public static String relativePath(String path, String relative) {
        String finalPath = null;
        int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (separatorIndex != -1) {
            String newPath = path.substring(0, separatorIndex);
            if (!relative.startsWith(FOLDER_SEPARATOR)) {
                newPath += FOLDER_SEPARATOR;
            }
            finalPath = newPath + relative;
        } else {
            finalPath = relative;
        }
        return cleanPath(finalPath);

    }

    public static boolean isURL(String path) {
        if (StringHelper.isEmpty(path)) {
            return false;
        }
        return URL_PATTERN.matcher(path).matches();
    }

    public static boolean isAbsolute(String path) {
        if (StringHelper.isEmpty(path)) {
            return false;
        }
        return Paths.get(path).isAbsolute();
    }

    private PathHelper() {
    }
    
}
