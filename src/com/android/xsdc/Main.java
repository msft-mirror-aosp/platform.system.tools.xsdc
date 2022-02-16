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
import com.android.xsdc.cpp.CppCodeGenerator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Main {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(OptionBuilder
                .withLongOpt("package")
                .hasArgs(1)
                .withDescription("Package name of the generated java file. " +
                        "file name of generated cpp file and header")
                .create("p"));
        options.addOption(OptionBuilder
                .withLongOpt("outDir")
                .hasArgs(1)
                .withDescription("Out Directory")
                .create("o"));
        options.addOption(OptionBuilder
                .withLongOpt("java")
                .hasArgs(0)
                .withDescription("Generate Java code.")
                .create("j"));
        options.addOption(OptionBuilder
                .withLongOpt("cpp")
                .hasArgs(0)
                .withDescription("Generate Cpp code.")
                .create("c"));
        options.addOption(OptionBuilder
                .withLongOpt("writer")
                .hasArgs(0)
                .withDescription("Generate Writer code.")
                .create("w"));
        options.addOption(OptionBuilder
                .withLongOpt("nullability")
                .hasArgs(0)
                .withDescription("Add @NonNull or @Nullable annotation to generated java code.")
                .create("n"));
        options.addOption(OptionBuilder
                .withLongOpt("genHas")
                .hasArgs(0)
                .withDescription("Generate public hasX() method")
                .create("g"));
        options.addOption(OptionBuilder
                .withLongOpt("booleanGetter")
                .hasArgs(0)
                .withDescription("Generate isX() for boolean element or attribute.")
                .create("b"));
        Option genEnumsOnly = OptionBuilder
                .withLongOpt("genEnumsOnly")
                .hasArgs(0)
                .withDescription("Only generate enum converters in Cpp code.")
                .create("e");
        options.addOption(genEnumsOnly);
        Option genParserOnly = OptionBuilder
                .withLongOpt("genParserOnly")
                .hasArgs(0)
                .withDescription("Only generate XML parser in Cpp code.")
                .create("x");
        options.addOption(genParserOnly);
        // "Only generate enums" and "Only generate parser" options are mutually exclusive.
        OptionGroup genOnlyGroup = new OptionGroup();
        genOnlyGroup.setRequired(false);
        genOnlyGroup.addOption(genEnumsOnly);
        genOnlyGroup.addOption(genParserOnly);
        options.addOptionGroup(genOnlyGroup);

        CommandLineParser CommandParser = new GnuParser();
        CommandLine cmd;

        try {
            cmd = CommandParser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            help(options);
            return;
        }

        String[] xsdFile = cmd.getArgs();
        String packageName = cmd.getOptionValue('p', null);
        String outDir = cmd.getOptionValue('o', null);
        boolean writer = cmd.hasOption('w');
        boolean nullability = cmd.hasOption('n');
        boolean genHas = cmd.hasOption('g');
        boolean enumsOnly = cmd.hasOption('e');
        boolean parserOnly = cmd.hasOption('x');
        boolean booleanGetter = cmd.hasOption('b');

        if (xsdFile.length != 1 || packageName == null) {
            System.err.println("Error: no xsd files or package name");
            help(options);
        }

        if (outDir == null) {
            outDir = ".";
        }

        XmlSchema xmlSchema = parse(xsdFile[0]);

        if (cmd.hasOption('j')) {
            File packageDir = new File(Paths.get(outDir, packageName.replace(".", "/")).toString());
            packageDir.mkdirs();
            FileSystem fs = new FileSystem(packageDir);
            JavaCodeGenerator javaCodeGenerator =
                    new JavaCodeGenerator(xmlSchema, packageName, writer, nullability, genHas,
                                          booleanGetter);
            javaCodeGenerator.print(fs);
        } else if (cmd.hasOption('c')) {
            File includeDir = new File(Paths.get(outDir, "include").toString());
            includeDir.mkdirs();
            FileSystem fs = new FileSystem(new File(outDir));
            int generators = enumsOnly ? CppCodeGenerator.GENERATE_ENUMS :
                    (parserOnly ? CppCodeGenerator.GENERATE_PARSER :
                            CppCodeGenerator.GENERATE_ENUMS | CppCodeGenerator.GENERATE_PARSER);
            CppCodeGenerator cppCodeGenerator =
                    new CppCodeGenerator(xmlSchema, packageName, writer, generators, booleanGetter);
            cppCodeGenerator.print(fs);
        }
    }

    private static XmlSchema parse(String xsdFile) throws Exception {
        XmlSchema xmlSchema;
        try (FileInputStream in = new FileInputStream(xsdFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XsdHandler xsdHandler = new XsdHandler();
            parser.parse(in, xsdHandler);
            xmlSchema = xsdHandler.getSchema();
        }
        for (String file : xmlSchema.getIncludeList()) {
            XmlSchema temp = parse(Paths.get(xsdFile).resolveSibling(file).toString());
            xmlSchema.include(temp);
        }
        return xmlSchema;
    }

    private static void help(Options options) {
        new HelpFormatter().printHelp(
                "xsdc path/to/xsd_file.xsd","", options, null, true);
        System.exit(1);
    }
}
