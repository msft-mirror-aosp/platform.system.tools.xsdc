/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.xsdc.java;

class JavaSimpleType implements JavaType {
    final private String name;
    final private String rawParsingExpression;
    final private boolean list;

    JavaSimpleType(String name, String rawParsingExpression, boolean list) {
        this.name = name;
        this.rawParsingExpression = rawParsingExpression;
        this.list = list;
    }

    boolean isList() {
        return list;
    }

    JavaSimpleType newListType() throws JavaCodeGeneratorException {
        if (list) throw new JavaCodeGeneratorException("list of list is not supported");
        return new JavaSimpleType(name, rawParsingExpression, true);
    }

    @Override
    public String getName() {
        return list ? String.format("java.util.List<%s>", name) : name;
    }

    @Override
    public String getParsingExpression() {
        StringBuilder expression = new StringBuilder();
        if (list) {
            expression.append(
                    String.format("%s value = new java.util.ArrayList<>();\n", getName()));
            expression.append("for (String token : raw.split(\"\\\\s+\")) {\n");
            expression.append(String.format("value.add(%s);\n",
                    String.format(rawParsingExpression, "token")));
            expression.append("}\n");
        } else {
            expression.append(
                    String.format("%s value = %s;\n", getName(),
                            String.format(rawParsingExpression, "raw")));
        }
        return expression.toString();
    }
}
