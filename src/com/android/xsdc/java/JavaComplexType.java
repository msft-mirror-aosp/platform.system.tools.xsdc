package com.android.xsdc.java;

class JavaComplexType implements JavaType {
    final private String name;

    JavaComplexType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParsingExpression() {
        return String.format("%s value = %s.read(parser);\n", name, name);
    }
}
