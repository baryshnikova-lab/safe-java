package edu.princeton.safe.internal.cluster;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Dendrogram {
    private Dendrogram() {
    }

    public static List<DendrogramNode> cut(DendrogramNode root,
                                           double threshold) {

        List<DendrogramNode> roots = new ArrayList<>();
        cut(roots, root, threshold);
        return roots;
    }

    static void cut(List<DendrogramNode> roots,
                    DendrogramNode root,
                    double threshold) {
        if (root instanceof ObservationNode) {
            roots.add(root);
            return;
        }

        MergeNode node = (MergeNode) root;
        double score = node.getDissimilarity();
        if (score <= threshold) {
            roots.add(root);
            return;
        }

        cut(roots, root.getLeft(), threshold);
        cut(roots, root.getRight(), threshold);
    }

    public static void dump(LabelFunction label,
                            DendrogramNode root,
                            PrintWriter writer) {
        dumpNode(label, "  ", root, writer);
    }

    static void dumpNode(LabelFunction label,
                         final String indent,
                         final DendrogramNode node,
                         PrintWriter writer) {
        if (node == null) {
            writer.println(indent + "<null>");
        } else if (node instanceof ObservationNode) {
            writer.println(indent + label.get(((ObservationNode) node).getObservation()));
        } else if (node instanceof MergeNode) {
            MergeNode merge = (MergeNode) node;
            writer.println(indent + "Merge: " + merge.getDissimilarity() + " (children: " + merge.getObservationCount()
                    + ")");
            dumpNode(label, indent + "  ", ((MergeNode) node).getLeft(), writer);
            dumpNode(label, indent + "  ", ((MergeNode) node).getRight(), writer);
        }
    }

}
