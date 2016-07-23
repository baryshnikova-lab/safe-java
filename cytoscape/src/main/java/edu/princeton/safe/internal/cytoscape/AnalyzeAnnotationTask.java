package edu.princeton.safe.internal.cytoscape;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.princeton.safe.internal.IdAnalyzer;
import edu.princeton.safe.internal.IdMappingResult;
import edu.princeton.safe.io.NodeTableVisitor;

public class AnalyzeAnnotationTask extends AbstractTask {

    String path;
    NodeTableVisitor visitor;
    AnalyzeAnnotationConsumer consumer;

    public AnalyzeAnnotationTask(String path,
                                 NodeTableVisitor visitor,
                                 AnalyzeAnnotationConsumer consumer) {
        this.path = path;
        this.visitor = visitor;
        this.consumer = consumer;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("SAFE: Analyze Annotation File");
        IdMappingResult result = IdAnalyzer.analyzeAnnotations(path, visitor);
        consumer.accept(result);
    }
}
