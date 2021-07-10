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
package com.chuan.simple.constant;

public class Constant {

    // attribute type
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VAL = "val";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_REF = "ref";

    public static final String ATTR_VALUE = "value";

    public static final String ATTR_NAME_AUTO_INIT = "autoInit";

    public static final String ATTR_RESOURCE = "resource";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_SCOPE = "scope";
    public static final String ATTR_DEFAULT_AUTOWIRED_FIELDS =
            "default-autowired-fields";
    public static final String ATTR_DEFAULT_AUTOWIRED_EXECUTABLES =
            "default-autowired-executables";
    public static final String ATTR_AUTO_INIT = "auto-init";
    public static final String ATTR_AUTOWIRED_FIELD = "autowired-field";
    public static final String ATTR_AUTOWIRED_EXECUTABLE =
            "autowired-executable";
    public static final String ATTR_NAME_AUTOWIRED_FIELD = "autowiredField";
    public static final String ATTR_NAME_AUTOWIRED_EXECUTABLE =
            "autowiredExecutable";
    public static final String ATTR_ORDER = "order";
    public static final String ATTR_METHOD_NAME = "method-name";
    public static final String ATTR_BUILD_PARAMETER_TYPES =
            "build-parameter-types";
    public static final String ATTR_OWNER_NAME = "owner-name";
    public static final String ATTR_OWNER_CLASS_NAME = "owner-class-name";
    public static final String ATTR_DEPENDS_ON = "depends-on";
    public static final String ATTR_INDEX = "index";
    public static final String ATTR_ALIAS = "alias";
    public static final String ATTR_POINTCUT = "pointcut";
    public static final String ATTR_REQUIRED = "required";
    public static final String ATTR_BASE_PACKAGE = "base-package";
    public static final String ATTR_EXECUTABLE_NAME = "executable-name";

    // document type
    public static final String DOC_BUILDER = "builder";
    public static final String DOC_IMPORT = "import";
    public static final String DOC_DESCRIPTION = "description";
    public static final String DOC_BUILD_PARAMETER = "build-parameter";
    public static final String DOC_EXECUTABLE_PARAMETER =
            "executable-parameter";
    public static final String DOC_ALIAS = "alias";

    public static final String DOC_ARRAY = "array";
    public static final String DOC_LIST = "list";
    public static final String DOC_SET = "set";
    public static final String DOC_MAP = "map";
    public static final String DOC_PROP = "prop";
    public static final String DOC_FIELD = "field";
    public static final String DOC_ELE = "ele";
    public static final String DOC_PAIR = "pair";
    public static final String DOC_PAIR_KEY = "key";
    public static final String DOC_PAIR_VALUE = "value";

    // value type
    public static final String TRUE_VALUE = "true";

    // constant String
    public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";
    
    private Constant() {
    }

}
