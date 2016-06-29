package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.IntObjectMap;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class CompositeMapController {

    DialogTaskManager taskManager;
    VisualMappingManager visualMappingManager;
    StyleFactory styleFactory;

    SafeSession session;
    BuildCompositeMapTaskConsumer consumer;
    SafeController safeController;

    VisualStyle domainBrowserStyle;

    Component panel;

    JComboBox<NameValuePair<Factory<RestrictionMethod>>> restrictionMethods;

    JFormattedTextField minimumLandscapeSize;

    JComboBox<NameValuePair<Factory<GroupingMethod>>> groupingMethods;
    JFormattedTextField similarityThreshold;

    JComboBox<NameValuePair<Integer>> analysisTypes;

    public CompositeMapController(DialogTaskManager taskManager,
                                  VisualMappingManager visualMappingManager,
                                  StyleFactory styleFactory) {
        this.taskManager = taskManager;
        this.visualMappingManager = visualMappingManager;
        this.styleFactory = styleFactory;

        consumer = new BuildCompositeMapTaskConsumer() {
            @Override
            public void accept(CompositeMap compositeMap) {
                session.setCompositeMap(compositeMap);
                safeController.setCompositeMap(compositeMap);
            }
        };
    }

    public void setSafeController(SafeController safeController) {
        this.safeController = safeController;
        safeController.addConsumer(new SafeResultConsumer() {
            @Override
            public void acceptEnrichmentLandscape(EnrichmentLandscape landscape) {
                setEnrichmentLandscape(landscape);
            }

            @Override
            public void acceptCompositeMap(CompositeMap map) {
                setCompositeMap(map);
            }
        });
    }

    @SuppressWarnings("unchecked")
    void setCompositeMap(CompositeMap compositeMap) {
        if (compositeMap == null) {
            return;
        }
        NameValuePair<Integer> analysisType = (NameValuePair<Integer>) analysisTypes.getSelectedItem();
        applyColors(compositeMap, analysisType.getValue());
    }

    public void setSession(SafeSession session) {
        this.session = session;
        if (session == null) {
            return;
        }

        minimumLandscapeSize.setValue(session.getMinimumLandscapeSize());

        similarityThreshold.setValue(session.getSimilarityThreshold());

        SafeUtil.setSelected(restrictionMethods, session.getRestrictionMethod());
        SafeUtil.setSelected(groupingMethods, session.getGroupingMethod());
        SafeUtil.setSelected(analysisTypes, session.getAnalysisType());

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        setEnrichmentLandscape(landscape);

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

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));

        SafeUtil.addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Neighborhood filtering method"));
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

        panel.add(button, "span 2, tag apply, wrap");

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

                BuildCompositeMapTaskFactory factory = new BuildCompositeMapTaskFactory(session, consumer);
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
        return Double.parseDouble(similarityThreshold.getText());
    }

    void applyColors(CompositeMap compositeMap,
                     int typeIndex) {
        IntObjectMap<Long> nodeMappings = session.getNodeMappings();

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();

        SafeUtil.checkSafeColumns(nodeTable);

        List<? extends Domain> domains = compositeMap.getDomains(typeIndex);
        int totalDomains = domains.size();
        List<double[]> colors = IntStream.range(0, totalDomains)
                                         .mapToObj(i -> {
                                             double hue = (double) i / totalDomains;
                                             return Util.hslToRgb(hue, 1.0, 0.5);
                                         })
                                         .collect(Collectors.toList());

        Collections.shuffle(colors);

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);
        ScoringFunction score = Neighborhood.getScoringFunction(typeIndex);

        ColorFunction coloring = d -> (int) Math.round(Math.min(1, d * 1.5) * 255);

        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
        neighborhoods.stream()
                     .forEach(n -> {
                         Long suid = nodeMappings.get(n.getNodeIndex());
                         CyRow row = nodeTable.getRow(suid);

                         double[] color = computeColor(compositeMap, domains, colors, isSignificant, score, n,
                                                       typeIndex);
                         if (color == null) {
                             row.set(StyleFactory.COLOR_COLUMN, null);
                             row.set(StyleFactory.BRIGHTNESSS_COLUMN, 0D);
                         } else {
                             int red = coloring.get(color[0]);
                             int green = coloring.get(color[1]);
                             int blue = coloring.get(color[2]);

                             String hexColor = String.format("#%02x%02x%02x", red, green, blue);
                             row.set(StyleFactory.COLOR_COLUMN, hexColor);

                             double brightness = (color[0] + color[1] + color[2]) / 3;
                             row.set(StyleFactory.BRIGHTNESSS_COLUMN, brightness);
                         }
                     });

        setDomainBrowserStyle(view);
    }

    void setDomainBrowserStyle(CyNetworkView view) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setDomainBrowserStyle(view));
            return;
        }

        if (domainBrowserStyle == null) {
            domainBrowserStyle = visualMappingManager.getAllVisualStyles()
                                                     .stream()
                                                     .filter(s -> s.getTitle()
                                                                   .equals(StyleFactory.DOMAIN_BROWSER_STYLE))
                                                     .findFirst()
                                                     .orElse(null);
        }

        if (domainBrowserStyle == null) {
            domainBrowserStyle = styleFactory.createDomainBrowserStyle();
            visualMappingManager.addVisualStyle(domainBrowserStyle);
        }

        VisualStyle viewStyle = visualMappingManager.getVisualStyle(view);
        if (viewStyle != domainBrowserStyle) {
            visualMappingManager.setVisualStyle(domainBrowserStyle, view);

        }
    }

    double[] computeColor(CompositeMap compositeMap,
                          List<? extends Domain> domains,
                          List<double[]> colors,
                          SignificancePredicate isSignificant,
                          ScoringFunction score,
                          Neighborhood neighborhood,
                          int typeIndex) {
        double[] color = { 0, 0, 0 };
        int[] totalContributions = { 0 };

        domains.stream()
               .forEach(domain -> {
                   int index = domain.getIndex();
                   domain.forEachAttribute(j -> {
                       if (!isSignificant.test(neighborhood, j) || !compositeMap.isTop(j, typeIndex)) {
                           return;
                       }

                       double opacity = score.get(neighborhood, j);
                       double[] attributeColor = Util.multiply(opacity * opacity, colors.get(index));
                       Util.addInPlace(attributeColor, color);
                       totalContributions[0]++;
                   });
               });

        if (totalContributions[0] == 0) {
            return null;
        }

        Util.divideInPlace(totalContributions[0], color);
        return color;
    }

    static interface ColorFunction {
        int get(double value);
    }
}
