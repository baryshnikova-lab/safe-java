package edu.princeton.safe.internal.cytoscape.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Random;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.grouping.JaccardDistanceMethod;
import edu.princeton.safe.grouping.NullGroupingMethod;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.UiUtil;
import edu.princeton.safe.internal.cytoscape.event.EventService;
import edu.princeton.safe.internal.cytoscape.model.Factory;
import edu.princeton.safe.internal.cytoscape.model.NameValuePair;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.cytoscape.task.BuildCompositeMapTask;
import edu.princeton.safe.internal.cytoscape.task.BuildCompositeMapTaskConsumer;
import edu.princeton.safe.internal.cytoscape.task.SimpleTaskFactory;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class CompositeMapController {

    final DialogTaskManager taskManager;

    final EventService eventService;
    final DomainBrowserController domainBrowser;
    final BuildCompositeMapTaskConsumer consumer;

    SafeSession session;

    Component panel;

    JComboBox<NameValuePair<Factory<RestrictionMethod>>> restrictionMethods;

    JFormattedTextField minimumLandscapeSize;

    JComboBox<NameValuePair<Factory<GroupingMethod>>> groupingMethods;
    JFormattedTextField similarityThreshold;

    JComboBox<NameValuePair<Integer>> analysisTypes;

    JCheckBox randomizeColorsCheckBox;

    public CompositeMapController(DialogTaskManager taskManager,
                                  DomainBrowserController domainBrowser,
                                  EventService eventService) {
        this.taskManager = taskManager;
        this.domainBrowser = domainBrowser;
        this.eventService = eventService;

        eventService.addEnrichmentLandscapeListener(landscape -> setEnrichmentLandscape(landscape));
        eventService.addCompositeMapListener(map -> setCompositeMap(map));

        consumer = new BuildCompositeMapTaskConsumer() {
            @Override
            public void accept(CompositeMap compositeMap) {
                session.setCompositeMap(compositeMap);
                eventService.notifyListeners(compositeMap);
            }
        };
    }

    void setCompositeMap(CompositeMap compositeMap) {
        domainBrowser.updateCompositeMap();
    }

    public void setSession(SafeSession session) {
        this.session = session;
        if (session == null) {
            domainBrowser.setSession(null);
            setCompositeMap(null);
            return;
        }

        minimumLandscapeSize.setValue(session.getMinimumLandscapeSize());

        similarityThreshold.setValue(session.getSimilarityThreshold());

        SafeUtil.setSelected(restrictionMethods, session.getRestrictionMethod());
        SafeUtil.setSelected(groupingMethods, session.getGroupingMethod());
        SafeUtil.setSelected(analysisTypes, session.getAnalysisType());

        randomizeColorsCheckBox.setSelected(session.getRandomizeColors());

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        setEnrichmentLandscape(landscape);

        domainBrowser.setSession(session);
        setCompositeMap(session.getCompositeMap());
    }

    void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        if (landscape == null) {
            panel.setEnabled(false);
            return;
        }

        panel.setEnabled(true);

        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        boolean isBinary = annotationProvider.isBinary();

        analysisTypes.setEnabled(!isBinary);
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    Component createPanel() {
        restrictionMethods = new JComboBox<>(createFilteringMethodModel());
        minimumLandscapeSize = new JFormattedTextField(NumberFormat.getIntegerInstance());

        groupingMethods = new JComboBox<>(createSimilarityMetricModel());
        similarityThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        analysisTypes = new JComboBox<>(createAnalysisTypeModel());

        JButton button = createBuildButton();

        randomizeColorsCheckBox = new JCheckBox("Randomize colors");

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));

        SafeUtil.addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Multi-regional"));
        panel.add(restrictionMethods, "wrap");
        panel.add(new JLabel("Min. landscape size"));
        panel.add(minimumLandscapeSize, "growx, wmax 200, wrap");

        SafeUtil.addSubsection(panel, "Group Attributes");
        panel.add(new JLabel("Similarity metric"));
        panel.add(groupingMethods, "wrap");
        panel.add(new JLabel("Similarity threshold"));
        panel.add(similarityThreshold, "growx, wmax 200, wrap");

        panel.add(new JLabel("Values to consider"));
        panel.add(analysisTypes, "wrap");

        panel.add(randomizeColorsCheckBox, "skip 1, wrap");

        panel.add(button, "skip 1, wrap");

        return panel;
    }

    JButton createBuildButton() {
        JButton button = new JButton("Build");
        button.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                NameValuePair<Factory<GroupingMethod>> groupingMethod = (NameValuePair<Factory<GroupingMethod>>) groupingMethods.getSelectedItem();
                session.setGroupingMethod(groupingMethod.getValue()
                                                        .create());

                NameValuePair<Factory<RestrictionMethod>> restrictionMethod = (NameValuePair<Factory<RestrictionMethod>>) restrictionMethods.getSelectedItem();
                session.setRestrictionMethod(restrictionMethod.getValue()
                                                              .create());

                session.setMinimumLandscapeSize(getMinimumLandscapeSize());

                if (randomizeColorsCheckBox.isSelected()) {
                    session.setColorSeed(new Random().nextInt());
                }

                TaskFactory factory = new SimpleTaskFactory(() -> new BuildCompositeMapTask(session, consumer));
                taskManager.execute(factory.createTaskIterator());
            }
        });
        return button;
    }

    int getMinimumLandscapeSize() {
        Number value = (Number) minimumLandscapeSize.getValue();
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Factory<GroupingMethod>>> createSimilarityMetricModel() {
        NameValuePair[] items = { new NameValuePair<>("None (no grouping)",
                                                      new Factory<>(null, () -> NullGroupingMethod.instance)),
                                  new NameValuePair<>("Jaccard", new Factory<>("jaccard", () -> {
                                      int totalAttributes = session.getEnrichmentLandscape()
                                                                   .getAnnotationProvider()
                                                                   .getAttributeCount();
                                      double threshold = Neighborhood.getEnrichmentThreshold(totalAttributes);
                                      JaccardDistanceMethod distanceMethod = new JaccardDistanceMethod(d -> d > threshold);
                                      return new ClusterBasedGroupingMethod(getClusterThreshold(), distanceMethod);
                                  })),
                                  new NameValuePair<>("Pearson",
                                                      new Factory<>("pearson",
                                                                    () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                         DistanceMethod.CORRELATION))) };
        return new DefaultComboBoxModel<>(items);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Factory<RestrictionMethod>>> createFilteringMethodModel() {
        NameValuePair[] items = new NameValuePair[] { new NameValuePair<>("Do not remove",
                                                                          new Factory<>(null, () -> null)),
                                                      new NameValuePair<>("Removed (Radius-based)",
                                                                          new Factory<>("radius",
                                                                                        () -> new RadiusBasedRestrictionMethod(getMinimumLandscapeSize(),
                                                                                                                               getDistanceThreshold()))) };
        return new DefaultComboBoxModel<>(items);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Integer>> createAnalysisTypeModel() {
        NameValuePair[] items = new NameValuePair[] { new NameValuePair<>("Highest", EnrichmentLandscape.TYPE_HIGHEST),
                                                      new NameValuePair<>("Lowest", EnrichmentLandscape.TYPE_LOWEST) };
        return new DefaultComboBoxModel<>(items);
    }

    double getDistanceThreshold() {
        return 65;
    }

    double getClusterThreshold() {
        return Util.parseDouble(similarityThreshold.getText());
    }

}
