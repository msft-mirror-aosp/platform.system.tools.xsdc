/*
 * Copyright (C) 2022, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.xsdc.cpp;

import static org.junit.Assert.assertEquals;

import com.android.xsdc.FileSystem;
import com.android.xsdc.XmlSchema;
import com.android.xsdc.XsdHandler;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class TestCppCodeGenerator {
    public static final String SCHEMA = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
            + "  <xs:element name=\"class\" type=\"xs:string\" />\n"
            + "</xs:schema>";

    @Test
    public void testParseSchema() throws Exception {
        XmlSchema schema = parseSchema(SCHEMA);
        assertEquals(schema.getElementMap().keySet(), Set.of("class"));
    }

    @Test
    public void testPrintWithoutEnumOutput() throws Exception {
        Map<String, StringBuffer> files = new TreeMap<>();
        CppCodeGenerator gen =
                new CppCodeGenerator(
                        parseSchema(SCHEMA),
                        "com.abc",
                        false,
                        CppCodeGenerator.GENERATE_PARSER,
                        false,
                        false);

        FileSystem fs = new FileSystem(files);
        gen.print(fs);

        assertEquals(files.keySet(), Set.of("com_abc.cpp", "include/com_abc.h"));
    }

    @Test
    public void printWithEnumOutput() throws Exception {
        Map<String, StringBuffer> files = new TreeMap<>();

        CppCodeGenerator gen =
                new CppCodeGenerator(
                        parseSchema(SCHEMA),
                        "com.abc",
                        false,
                        CppCodeGenerator.GENERATE_PARSER | CppCodeGenerator.GENERATE_ENUMS,
                        false,
                        false);

        FileSystem fs = new FileSystem(files);
        gen.print(fs);

        assertEquals(
                files.keySet(),
                Set.of(
                        "com_abc.cpp",
                        "include/com_abc.h",
                        "com_abc_enums.cpp",
                        "include/com_abc_enums.h"));
    }

    private XmlSchema parseSchema(String contents) throws Exception {
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        XsdHandler xsdHandler = new XsdHandler();
        parser.parse(new ByteArrayInputStream(bytes), xsdHandler);
        return xsdHandler.getSchema();
    }
}
