package edu.princeton.safe;

import java.io.IOException;

public interface AnnotationParser {
    
    void parse(NetworkProvider networkProvider,
               AnnotationConsumer consumer)
            throws IOException;

}
