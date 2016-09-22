package edu.princeton.safe.io;

import java.util.Collections;
import java.util.List;

public interface NetworkConsumer {
    void startNodes();

    void node(int nodeIndex,
              String label,
              List<String> ids,
              double x,
              double y);

    default void node(int nodeIndex,
                      String label,
                      String id,
                      double x,
                      double y) {
        node(nodeIndex, label, Collections.singletonList(id), x, y);
    }

    void finishNodes();

    void startEdges();

    void edge(int fromIndex,
              int toIndex,
              double weight);

    void finishEdges();
}
