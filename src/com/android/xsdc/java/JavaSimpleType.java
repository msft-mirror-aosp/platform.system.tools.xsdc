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
