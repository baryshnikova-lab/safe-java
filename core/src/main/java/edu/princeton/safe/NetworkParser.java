package edu.princeton.safe;

import java.io.IOException;

public interface NetworkParser {
    void parse(NetworkConsumer consumer) throws IOException;

    boolean isDirected();
}
