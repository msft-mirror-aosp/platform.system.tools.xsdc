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
