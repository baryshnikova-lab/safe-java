package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.text.NumberFormat;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class CompositeMapController {

    SafeSession session;

    Component panel;

    JComboBox<NameValuePair<Factory<RestrictionMethod>>> neighborhoodFilteringMethod;

    JFormattedTextField minimumLandscapeSize;

    JComboBox<NameValuePair<Factory<GroupingMethod>>> similarityMetric;
    JFormattedTextField similarityThreshold;

    public CompositeMapController() {
    }

    public void setSession(SafeSession session) {
        this.session = session;
        if (session == null) {
            return;
        }

        minimumLandscapeSize.setValue(session.getMinimumLandscapeSize());

        similarityThreshold.setValue(session.getSimilarityThreshold());
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    Component createPanel() {
        neighborhoodFilteringMethod = new JComboBox<>(createFilteringMethodModel());
        minimumLandscapeSize = new JFormattedTextField(NumberFormat.getIntegerInstance());

        similarityMetric = new JComboBox<>(createSimilarityMetricModel());
        similarityThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));

        SafeUtil.addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Neighborhood filtering method"));
        panel.add(neighborhoodFilteringMethod, "wrap");
        panel.add(new JLabel("Min. landscape size"));
        panel.add(minimumLandscapeSize, "growx, wmax 200, wrap");

        SafeUtil.addSubsection(panel, "Group Attributes");
        panel.add(new JLabel("Similarity metric"));
        panel.add(similarityMetric, "wrap");
        panel.add(new JLabel("Similarity threshold"));
        panel.add(similarityThreshold, "growx, wmax 200, wrap");

        return panel;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Factory<GroupingMethod>>> createSimilarityMetricModel() {
        NameValuePair[] items = { new NameValuePair<>("Jaccard",
                                                      new Factory<>("jaccard",
                                                                    () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                         DistanceMethod.JACCARD))),
                                  new NameValuePair<>("Pearson",
                                                      new Factory<>("pearson",
                                                                    () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                         DistanceMethod.CORRELATION))) };
        return new DefaultComboBoxModel<>(items);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Factory<RestrictionMethod>>> createFilteringMethodModel() {
        NameValuePair[] items = new NameValuePair[] { new NameValuePair<>("Radius-based",
                                                                          new Factory<>("radius",
                                                                                        () -> new RadiusBasedRestrictionMethod(getDistanceThreshold()))) };
        return new DefaultComboBoxModel<>(items);
    }

    double getDistanceThreshold() {
        return 65;
    }

    double getClusterThreshold() {
        return Double.parseDouble(similarityThreshold.getText());
    }

}
