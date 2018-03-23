package org.xel.helpers;

import org.xel.computation.ExecutionEngine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReader {
    public static String readFile(String path, Charset encoding)
            throws IOException {
        return ExecutionEngine.getEplCode(path);
    }
}
