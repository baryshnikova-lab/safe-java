package edu.princeton.safe.internal.cytoscape;

public class AttributeRow {
    int index;
    String name;
    long totalHighest;
    long totalLowest;

    AttributeRow(int index,
                 String name,
                 long totalHighest,
                 long totalLowest) {
        this.index = index;
        this.name = name;
        this.totalHighest = totalHighest;
        this.totalLowest = totalLowest;
    }
}