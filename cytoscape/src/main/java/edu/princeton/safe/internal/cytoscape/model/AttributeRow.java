package edu.princeton.safe.internal.cytoscape.model;

import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.internal.cytoscape.SafeUtil;

public class AttributeRow {
    int index;
    String name;
    LongSet highestSuids;
    LongSet lowestSuids;
    boolean isVisible;

    public AttributeRow(int index,
                        String name) {
        this.index = index;
        this.name = name;
        highestSuids = new LongScatterSet();
        lowestSuids = new LongScatterSet();
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public long getTotalHighest() {
        return highestSuids.size();
    }

    public long getTotalLowest() {
        return lowestSuids.size();
    }

    public void addHighest(long suid) {
        highestSuids.add(suid);
    }

    public void addLowest(long suid) {
        lowestSuids.add(suid);
    }

    public boolean hasHighest(LongSet suids) {
        return SafeUtil.hasIntersection(highestSuids, suids);
    }

    public boolean hasLowest(LongSet suids) {
        return SafeUtil.hasIntersection(lowestSuids, suids);
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }
}