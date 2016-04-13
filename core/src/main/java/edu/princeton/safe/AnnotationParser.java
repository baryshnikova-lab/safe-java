package edu.princeton.safe;

import java.io.IOException;

public interface AnnotationParser {
    
    void start(String[] attributeLabels,
               int totalNodes);

    void addValue(int nodeIndex,
                  int attributeIndex,
                  double value);

    void parse(NetworkProvider networkProvider,
               String path)
            throws IOException;

}
