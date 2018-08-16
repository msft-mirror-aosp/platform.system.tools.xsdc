package com.android.xsdc;

import com.android.xsdc.descriptor.ClassDescriptor;
import com.android.xsdc.descriptor.SchemaDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;

import static java.lang.System.exit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Failed: it should have three arguments: path of an input xsd file, package name, and output directory");
            exit(-1);
        }
        String xsdFile = args[0], packageName = args[1], outDir = args[2];
        try (FileInputStream in = new FileInputStream(xsdFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XsdHandler xsdHandler = new XsdHandler();
            parser.parse(in, xsdHandler);
            XmlSchema schema = xsdHandler.getSchema();
            SchemaDescriptor descriptor = schema.explain();
            File packageDir = new File(Paths.get(outDir, packageName.replace(".", "/")).toString());
            packageDir.mkdirs();
            for (ClassDescriptor cls : descriptor.getClassDescriptorMap().values()) {
                try (PrintWriter writer = new PrintWriter(new File(packageDir, cls.getName() + ".java"))) {
                    cls.print(packageName, writer);
                }
            }
            try (PrintWriter writer = new PrintWriter(new File(packageDir, "XmlParser.java").toString())) {
                descriptor.printXmlParser(packageName, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            exit(-1);
        }
    }
}
