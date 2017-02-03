package edu.princeton.safe.internal.cytoscape.model;

import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.model.Domain;

public class DomainRow {
    Domain domain;
    double[] color;
    LongSet significantSuids;
    boolean isVisible;

    public DomainRow() {
        significantSuids = new LongScatterSet();
    }

    @Override
    public String toString() {
        String hexColor = String.format("#%02x%02x%02x", Math.round(color[0] * 255), Math.round(color[1] * 255),
                                        Math.round(color[2] * 255));
        return String.format("<html><span style=\"color: %s; font-family: FontAwesome\">\uf111</span> %s", hexColor,
                             domain.getName());
    }

    public double[] getColor() {
        return color;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public void setColor(double[] color) {
        this.color = color;
    }

    public void addSignificant(long suid) {
        significantSuids.add(suid);
    }

    public boolean hasSignificant(LongSet suids) {
        return SafeUtil.hasIntersection(significantSuids, suids);
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }
}
