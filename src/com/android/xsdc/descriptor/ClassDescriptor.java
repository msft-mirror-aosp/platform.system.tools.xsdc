package com.android.xsdc.descriptor;

import com.android.xsdc.CodeWriter;
import com.android.xsdc.Utils;
import com.android.xsdc.XsdParserException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassDescriptor {

    private String name;
    private ClassDescriptor base;
    private TypeDescriptor valueType;
    private List<VariableDescriptor> elements;
    private List<VariableDescriptor> attributes;
    private List<ClassDescriptor> innerClasses;
    private Set<String> nameSet;

    public ClassDescriptor(String name) {
        this.name = name;
        elements = new ArrayList<>();
        attributes = new ArrayList<>();
        innerClasses = new ArrayList<>();
        nameSet = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setBase(ClassDescriptor base) throws XsdParserException {
        for (String name : base.nameSet) {
            addNameSet(name);
        }
        this.base = base;
    }

    public void setValueType(TypeDescriptor valueType) throws XsdParserException {
        addNameSet("value");
        this.valueType = valueType;
    }

    public void registerElement(VariableDescriptor element) throws XsdParserException {
        addNameSet(element.getName());
        elements.add(element);
    }

    public void registerAttribute(VariableDescriptor attribute) throws XsdParserException {
        addNameSet(attribute.getName());
        attributes.add(attribute);
    }

    public void registerInnerClass(ClassDescriptor innerClass) throws XsdParserException {
        addNameSet(innerClass.getName());
        innerClasses.add(innerClass);
    }

    private void addNameSet(String name) throws XsdParserException {
        if (nameSet.contains(name)) {
            throw new XsdParserException(
                    String.format("duplicate variable name : class %s, variable %s", this.name,
                            name));
        }
        nameSet.add(name);
    }

    public void print(String packageName, CodeWriter out) {
        out.printf("package %s;\n\n", packageName);
        print(out, false);
    }

    private void print(CodeWriter out, boolean isInner) {
        if (isInner) {
            out.printf("public static class %s ", getName());
        } else {
            out.printf("public class %s ", getName());
        }

        if (base != null) {
            out.printf("extends %s {\n", base.getName());
        } else {
            out.println("{");
        }

        List<VariableDescriptor> values = new ArrayList<>();
        if (valueType != null) {
            values.add(new VariableDescriptor(valueType, "value", null, true, false));
        }

        printVariables(out, elements);
        printVariables(out, attributes);
        printVariables(out, values);

        printGetterAndSetter(out, elements);
        printGetterAndSetter(out, attributes);
        printGetterAndSetter(out, values);

        out.println();
        printParser(out);

        for (ClassDescriptor descriptor : innerClasses) {
            out.println();
            descriptor.print(out, true);
        }
        out.println("}");
    }

    private void printVariables(CodeWriter out, List<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            out.printf("protected %s %s;\n", variable.getFullTypeName(), variable.getName());
        }
    }

    private void printGetterAndSetter(CodeWriter out, List<VariableDescriptor> variables) {
        for (VariableDescriptor variable : variables) {
            out.println();
            out.printf("public %s get%s() {\n", variable.getFullTypeName(),
                    Utils.capitalize(variable.getName()));
            if (variable.isMultiple()) {
                out.printf("if (%s == null) {\n"
                        + "%s = new java.util.ArrayList<>();\n"
                        + "}\n", variable.getName(), variable.getName());
            }
            out.printf("return %s;\n"
                    + "}\n", variable.getName());

            if (!variable.isMultiple()) {
                out.println();
                out.printf("public void set%s(%s %s) {\n"
                        + "this.%s = %s;\n"
                        + "}\n",
                        Utils.capitalize(variable.getName()), variable.getFullTypeName(),
                        variable.getName(), variable.getName(), variable.getName());
            }
        }
    }

    private List<VariableDescriptor> getAllAttributes() {
        List<VariableDescriptor> allAttributes;
        if (base != null) {
            allAttributes = base.getAllAttributes();
        } else {
            allAttributes = new ArrayList<>();
        }
        allAttributes.addAll(attributes);
        return allAttributes;
    }

    private List<VariableDescriptor> getAllElements() {
        List<VariableDescriptor> allElements;
        if (base != null) {
            allElements = base.getAllElements();
        } else {
            allElements = new ArrayList<>();
        }
        allElements.addAll(elements);
        return allElements;
    }

    private TypeDescriptor getBaseValueType() {
        if (base != null) {
            return base.getBaseValueType();
        } else {
            return valueType;
        }
    }

    private void printParser(CodeWriter out) {
        out.printf("public static %s read(org.xmlpull.v1.XmlPullParser parser) " +
                "throws org.xmlpull.v1.XmlPullParserException, java.io.IOException, " +
                "javax.xml.datatype.DatatypeConfigurationException {\n", name);

        out.printf("%s instance = new %s();\n"
                + "String raw = null;\n", name, name);
        for (VariableDescriptor attribute : getAllAttributes()) {
            out.printf("raw = parser.getAttributeValue(null, \"%s\");\n"
                    + "if (raw != null) {\n", attribute.getXmlName());
            out.print(attribute.getType().getParsingExpression());
            out.printf("instance.set%s(value);\n"
                    + "}\n", Utils.capitalize(attribute.getName()));
        }

        TypeDescriptor baseValueType = getBaseValueType();
        List<VariableDescriptor> allElements = getAllElements();
        if (baseValueType != null) {
            out.print("raw = XmlParser.readText(parser);\n"
                    + "if (raw != null) {\n");
            out.print(baseValueType.getParsingExpression());
            out.print("instance.setValue(value);\n"
                    + "}\n");
        } else if (!allElements.isEmpty()) {
            out.print("while (parser.next() != org.xmlpull.v1.XmlPullParser.END_TAG) {\n"
                    + "if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) "
                    + "continue;\n"
                    + "String tagName = parser.getName();\n");
            for (VariableDescriptor element : allElements) {
                out.printf("if (tagName.equals(\"%s\")) {\n", element.getXmlName());
                if (element.getType().isSimple()) {
                    out.print("raw = XmlParser.readText(parser);\n");
                }
                out.print(element.getType().getParsingExpression());
                if (element.isMultiple()) {
                    out.printf("instance.get%s().add(value);\n",
                            Utils.capitalize(element.getName()));
                } else {
                    out.printf("instance.set%s(value);\n",
                            Utils.capitalize(element.getName()));
                }
                out.printf("} else ");
            }
            out.print("{\n"
                    + "XmlParser.skip(parser);\n"
                    + "}\n"
                    + "}\n");
        }
        out.print("return instance;\n"
                + "}\n");
    }
}
