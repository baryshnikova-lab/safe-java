package edu.princeton.safe.model;

import java.util.function.IntConsumer;

public interface Domain {
    int getIndex();
    void forEachAttribute(IntConsumer action);
}
