package edu.princeton.safe;

@FunctionalInterface
public interface IndexedDoubleConsumer {
    void accept(int index,
                double value);
}
