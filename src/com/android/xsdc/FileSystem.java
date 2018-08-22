package com.android.xsdc;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class FileSystem {
    private File rootDirectory;
    private Map<String, StringBuffer> fileOutputMap;

    public FileSystem(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public FileSystem(Map<String, StringBuffer> fileOutputMap) {
        this.fileOutputMap = fileOutputMap;
    }

    public PrintWriter getPrintWriter(String fileName) throws IOException {
        if (rootDirectory != null) {
            return new PrintWriter(new File(rootDirectory, fileName));
        } else {
            StringWriter sw = new StringWriter();
            fileOutputMap.put(fileName, sw.getBuffer());
            return new PrintWriter(sw);
        }
    }
}
