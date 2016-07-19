package edu.princeton.safe.internal.cluster;

public class ObservationNode extends BaseDendrogramNode {

    int observationIndex;

    public ObservationNode(int observationIndex) {
        super(null, null);
        this.observationIndex = observationIndex;
    }

    public int getObservation() {
        return observationIndex;
    }

}
