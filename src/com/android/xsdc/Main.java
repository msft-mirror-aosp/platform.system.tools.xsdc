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

package com.android.xsdc;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

import static java.lang.System.exit;

import com.android.xsdc.java.JavaCodeGenerator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Failed - the number of arguments should be 3.");
            System.err.println("Usage: ./xsdc input_xsd_file package_name output_directory");
            exit(-1);
        }
        String xsdFile = args[0], packageName = args[1], outDir = args[2];
        XmlSchema xmlSchema;
        try (FileInputStream in = new FileInputStream(xsdFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XsdHandler xsdHandler = new XsdHandler();
            parser.parse(in, xsdHandler);
            xmlSchema = xsdHandler.getSchema();
        }
        File packageDir = new File(Paths.get(outDir, packageName.replace(".", "/")).toString());
        packageDir.mkdirs();
        FileSystem fs = new FileSystem(packageDir);
        JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(xmlSchema, packageName);
        javaCodeGenerator.print(fs);
    }
}
