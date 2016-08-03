package edu.princeton.safe.internal.cytoscape.model;

public class AttributeRow {
    int index;
    String name;
    long totalHighest;
    long totalLowest;

    public AttributeRow(int index,
                        String name,
                        long totalHighest,
                        long totalLowest) {
        this.index = index;
        this.name = name;
        this.totalHighest = totalHighest;
        this.totalLowest = totalLowest;
    }

    public void setTotalHighest(long count) {
        totalHighest = count;
    }

    public void setTotalLowest(long count) {
        totalLowest = count;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public long getTotalHighest() {
        return totalHighest;
    }

    public long getTotalLowest() {
        return totalLowest;
    }
}