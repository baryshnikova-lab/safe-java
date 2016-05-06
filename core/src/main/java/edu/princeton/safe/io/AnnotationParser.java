package edu.princeton.safe.io;

import java.io.IOException;

import edu.princeton.safe.NetworkProvider;

public interface AnnotationParser {
    
    void parse(NetworkProvider networkProvider,
               AnnotationConsumer consumer)
            throws IOException;

}
