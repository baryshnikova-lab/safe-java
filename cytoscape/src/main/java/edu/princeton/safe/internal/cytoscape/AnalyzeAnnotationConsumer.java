package edu.princeton.safe.internal.cytoscape;

import edu.princeton.safe.internal.IdMappingResult;

public interface AnalyzeAnnotationConsumer {
    void accept(IdMappingResult result);
}
