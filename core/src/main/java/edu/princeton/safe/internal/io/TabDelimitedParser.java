package edu.princeton.safe.internal.io;

import java.io.BufferedReader;
import java.io.IOException;

import edu.princeton.safe.internal.Util;

public class TabDelimitedParser {

    String path;
    int skippedLines;
    int totalLines;

    public TabDelimitedParser(String path) {
        this.path = path;
    }

    public void parse(LineHandler handler) throws IOException {
        try (BufferedReader reader = Util.getReader(path)) {
            String line = reader.readLine();
            while (line != null) {
                try {
                    String[] parts = line.split("\t");
                    if (!handler.handle(parts)) {
                        skippedLines++;
                    }
                } finally {
                    totalLines++;
                    line = reader.readLine();
                }
            }
        }
    }

    public int getTotalLines() {
        return totalLines;
    }
}
