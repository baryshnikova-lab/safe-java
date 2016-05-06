package edu.princeton.safe.io;

public interface AnnotationConsumer {

    void start(String[] attributeLabels,
               int totalNetworkNodes);

    void value(int nodeIndex,
               int attributeIndex,
               double value);

    void finish(int totalAnnotationNodes);

}
