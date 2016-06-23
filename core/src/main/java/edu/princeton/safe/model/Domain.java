package edu.princeton.safe.model;

import java.util.function.IntConsumer;

public interface Domain {
    void forEachAttribute(IntConsumer action);
}
