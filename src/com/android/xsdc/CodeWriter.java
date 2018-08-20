package com.android.xsdc;

import java.io.Closeable;
import java.io.PrintWriter;

public class CodeWriter implements Closeable {
    private PrintWriter out;
    private int indent;
    private boolean startLine;

    public CodeWriter(PrintWriter printWriter) {
        out = printWriter;
        indent = 0;
        startLine = true;
    }

    private void printIndent() {
        assert startLine;
        for (int i=0; i<indent; ++i) {
            out.print("    ");
        }
        startLine = false;
    }

    public void println() {
        out.println();
        startLine = true;
    }

    public void println(String code) {
        print(code + "\n");
    }

    public void print(String code) {
        String[] lines = code.split("\n", -1);
        for (int i=0; i<lines.length; ++i) {
            String line = lines[i].trim();
            if (line.startsWith("}")) {
                --indent;
            }
            if (startLine && !line.isEmpty()) {
                printIndent();
            }
            out.print(line);
            if (line.endsWith("{")) {
                ++indent;
            }
            if (i+1 < lines.length) {
                out.println();
                startLine = true;
            }
        }
    }

    public void printf(String code, Object... arguments) {
        print(String.format(code, arguments));
    }

    @Override
    public void close() {
        if (out != null) {
            out.close();
        }
    }
}
