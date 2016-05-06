package edu.princeton.safe.io;

import java.io.IOException;

public interface NetworkParser {
    void parse(NetworkConsumer consumer) throws IOException;

    boolean isDirected();
}
