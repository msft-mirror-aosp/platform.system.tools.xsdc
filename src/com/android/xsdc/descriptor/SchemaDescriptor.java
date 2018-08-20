package com.android.xsdc.descriptor;

import com.android.xsdc.CodeWriter;
import com.android.xsdc.XsdParserException;

import java.util.HashMap;
import java.util.Map;

public class SchemaDescriptor {
    private Map<String, ClassDescriptor> classDescriptorMap;
    private Map<String, VariableDescriptor> rootElementMap;

    public SchemaDescriptor() {
        classDescriptorMap = new HashMap<>();
        rootElementMap = new HashMap<>();
    }

    public void registerClass(ClassDescriptor descriptor) throws XsdParserException {
        if (classDescriptorMap.containsKey(descriptor.getName()) || descriptor.getName().equals(
                "XmlParser")) {
            throw new XsdParserException(
                    String.format("duplicate class name : %s", descriptor.getName()));
        }
        classDescriptorMap.put(descriptor.getName(), descriptor);
    }

    public void registerRootElement(VariableDescriptor element) throws XsdParserException {
        if (rootElementMap.containsKey(element.getXmlName())) {
            throw new XsdParserException(
                    String.format("duplicate root element name : %s", element.getXmlName()));
        }
        rootElementMap.put(element.getXmlName(), element);
    }

    public Map<String, ClassDescriptor> getClassDescriptorMap() {
        return classDescriptorMap;
    }

    public void printXmlParser(String packageName, CodeWriter out) {
        out.printf("package %s;\n", packageName);
        out.println();
        out.println("public class XmlParser {");

        out.print("public static java.lang.Object read(java.io.InputStream in)"
                + " throws org.xmlpull.v1.XmlPullParserException, java.io.IOException, "
                + "javax.xml.datatype.DatatypeConfigurationException {\n"
                + "org.xmlpull.v1.XmlPullParser parser = org.xmlpull.v1.XmlPullParserFactory"
                + ".newInstance().newPullParser();\n"
                + "parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, "
                + "true);\n"
                + "parser.setInput(in, null);\n"
                + "parser.nextTag();\n"
                + "String tagName = parser.getName();\n");
        for (VariableDescriptor element : rootElementMap.values()) {
            out.printf("if (tagName.equals(\"%s\")) {\n", element.getXmlName());
            if (element.getType().isSimple()) {
                out.print("raw = XmlParser.readText(parser);\n");
            }
            out.print(element.getType().getParsingExpression());
            out.print("return value;\n"
                    + "} else ");
        }
        out.print("{\n"
                + "throw new RuntimeException(String.format(\"unknown element '%s'\", tagName));\n"
                + "}\n}\n");
        out.println();

        out.print(
                "public static java.lang.String readText(org.xmlpull.v1.XmlPullParser parser)"
                        + " throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {\n"
                        + "String result = \"\";\n"
                        + "if (parser.next() == org.xmlpull.v1.XmlPullParser.TEXT) {\n"
                        + "result = parser.getText();\n"
                        + "parser.nextTag();\n"
                        + "}\n"
                        + "return result;\n"
                        + "}\n");
        out.println();

        out.print(
                "public static void skip(org.xmlpull.v1.XmlPullParser parser)"
                        + " throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {\n"
                        + "if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {\n"
                        + "throw new IllegalStateException();\n"
                        + "}\n"
                        + "int depth = 1;\n"
                        + "while (depth != 0) {\n"
                        + "switch (parser.next()) {\n"
                        + "case org.xmlpull.v1.XmlPullParser.END_TAG:\n"
                        + "depth--;\n"
                        + "break;\n"
                        + "case org.xmlpull.v1.XmlPullParser.START_TAG:\n"
                        + "depth++;\n"
                        + "break;\n"
                        + "}\n}\n}\n");

        out.println("}");
    }
}
