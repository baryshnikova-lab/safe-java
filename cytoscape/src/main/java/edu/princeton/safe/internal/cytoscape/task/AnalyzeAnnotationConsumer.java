package edu.princeton.safe.internal.cytoscape.task;

import edu.princeton.safe.internal.IdMappingResult;

public interface AnalyzeAnnotationConsumer {
    void accept(IdMappingResult result);
}
