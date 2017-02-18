package edu.princeton.safe.internal.cytoscape.controller;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.LongSet;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.StyleFactory;
import edu.princeton.safe.internal.cytoscape.SubstringRowFilter;
import edu.princeton.safe.internal.cytoscape.UiUtil;
import edu.princeton.safe.internal.cytoscape.event.EventService;
import edu.princeton.safe.internal.cytoscape.model.DomainRow;
import edu.princeton.safe.internal.cytoscape.model.ListTableModel;
import edu.princeton.safe.internal.cytoscape.model.NameValuePair;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.cytoscape.task.ExportReportsTask;
import edu.princeton.safe.internal.cytoscape.task.SimpleTaskFactory;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import net.miginfocom.swing.MigLayout;

public class DomainBrowserController implements ExpansionChangeListener {

    VisualMappingManager visualMappingManager;
    StyleFactory styleFactory;
    DialogTaskManager taskManager;
    EventService eventService;

    SafeSession session;
    List<DomainRow> domainRows;
    ListTableModel<DomainRow> domainTableModel;

    Component panel;
    JComboBox<NameValuePair<Integer>> analysisTypes;
    FilteredTable<DomainRow> filteredTable;
    JButton selectSignificantButton;
    JCheckBox filterDomainsCheckBox;

    LongSet lastNodeSuids;

    public DomainBrowserController(VisualMappingManager visualMappingManager,
                                   StyleFactory styleFactory,
                                   DialogTaskManager taskManager,
                                   EventService eventService) {

        this.visualMappingManager = visualMappingManager;
        this.styleFactory = styleFactory;
        this.taskManager = taskManager;
        this.eventService = eventService;

        eventService.addNodeSelectionChangedListener(nodeSuids -> {
            lastNodeSuids = nodeSuids;
            applyRowVisibility();
        });
    }

    void applyRowVisibility() {
        updateRowVisibility(lastNodeSuids);
        if (filteredTable == null) {
            return;
        }
        filteredTable.getSorter()
                     .sort();
    }

    void updateRowVisibility(LongSet nodeSuids) {
        if (session == null || nodeSuids == null || nodeSuids.isEmpty()) {
            setAllVisible();
            return;
        }
        if (filterDomainsCheckBox == null || !filterDomainsCheckBox.isSelected()) {
            setAllVisible();
            return;
        }

        Long[] nodeMappings = session.getNodeMappings();
        if (nodeMappings == null) {
            setAllVisible();
            return;
        }

        domainRows.stream()
                  .forEach(row -> row.setVisible(row.hasSignificant(nodeSuids)));
    }

    void setAllVisible() {
        domainRows.stream()
                  .forEach(row -> row.setVisible(true));
    }

    public void setSession(SafeSession session) {
        this.session = session;
        if (session == null) {
            return;
        }
        SafeUtil.setSelected(analysisTypes, session.getAnalysisType());
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    Component createPanel() {
        analysisTypes = new JComboBox<>(createAnalysisTypeModel());
        domainRows = new ArrayList<>();
        domainTableModel = createDomainTableModel();

        filteredTable = new FilteredTable<>(domainTableModel, new SubstringRowFilter() {
            @Override
            protected boolean test(Predicate<String> predicate,
                                   int rowIndex) {
                DomainRow row = domainTableModel.getRow(rowIndex);
                String value = row.getDomain()
                                  .getName();
                return value != null && predicate.test(value) && row.isVisible();
            }
        });

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        configureSorter(sorter);

        JTable table = filteredTable.getTable();
        table.getSelectionModel()
             .addListSelectionListener(createListSelectionListener(table, sorter));

        analysisTypes.addActionListener((e) -> {
            updateAnalysisType();
            applyRowVisibility();
        });

        filterDomainsCheckBox = new JCheckBox("Only domains associated with selection");
        filterDomainsCheckBox.addActionListener(event -> applyRowVisibility());

        selectSignificantButton = new JButton("Select Significant Nodes");
        selectSignificantButton.setEnabled(false);
        selectSignificantButton.addActionListener(event -> selectSignificantNodes());

        JButton exportButton = new JButton("Export Reports");
        exportButton.addActionListener(event -> {
            TaskFactory taskFactory = new SimpleTaskFactory(() -> new ExportReportsTask(session));
            taskManager.execute(taskFactory.createTaskIterator());
        });

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));
        panel.add(new JLabel("Values to consider"));
        panel.add(analysisTypes, "wrap");
        panel.add(filteredTable.getPanel(), "span 2, grow, hmin 100, hmax 200, wrap");
        panel.add(filterDomainsCheckBox, "span, alignx left, wrap");
        panel.add(selectSignificantButton, "span, alignx center, split 2");
        panel.add(exportButton, "wrap");

        return panel;
    }

    @SuppressWarnings("unchecked")
    private void selectSignificantNodes() {
        NameValuePair<Integer> analysisType = (NameValuePair<Integer>) analysisTypes.getSelectedItem();
        int typeIndex = analysisType.getValue();

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);
        CompositeMap compositeMap = session.getCompositeMap();

        int[] rows = filteredTable.getTable()
                                  .getSelectedRows();
        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        Long[] nodeMappings = session.getNodeMappings();

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();

        SafeUtil.clearSelection(nodeTable);
        SafeUtil.clearSelection(edgeTable);

        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
        neighborhoods.stream()
                     .forEach(n -> {
                         boolean include = Arrays.stream(rows)
                                                 .map(i -> sorter.convertRowIndexToModel(i))
                                                 .mapToObj(i -> domainRows.get(i)
                                                                          .getDomain())
                                                 .filter(domain -> IntStream.range(0, domain.getAttributeCount())
                                                                            .map(m -> domain.getAttribute(m))
                                                                            .anyMatch(j -> isSignificant.test(n, j)
                                                                                    && compositeMap.isTop(j, typeIndex))

                                                 )
                                                 .findAny()
                                                 .isPresent();

                         Long suid = nodeMappings[n.getNodeIndex()];
                         CyRow row = nodeTable.getRow(suid);
                         row.set(CyNetwork.SELECTED, include);
                     });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    ComboBoxModel<NameValuePair<Integer>> createAnalysisTypeModel() {
        NameValuePair[] items = new NameValuePair[] { new NameValuePair<>("Highest", EnrichmentLandscape.TYPE_HIGHEST),
                                                      new NameValuePair<>("Lowest", EnrichmentLandscape.TYPE_LOWEST) };
        return new DefaultComboBoxModel<>(items);
    }

    ListSelectionListener createListSelectionListener(JTable table,
                                                      TableRowSorter<TableModel> sorter) {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                int[] selectedRows = table.getSelectedRows();
                selectSignificantButton.setEnabled(selectedRows.length > 0);
                updateSelectedAttributes(sorter, selectedRows);
            }
        };
    }

    void updateAnalysisType() {
        updateCompositeMap();
    }

    @SuppressWarnings("serial")
    ListTableModel<DomainRow> createDomainTableModel() {
        ListTableModel<DomainRow> model = new ListTableModel<DomainRow>(domainRows) {
            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex,
                                     int columnIndex) {

                if (rowIndex < 0 || rowIndex >= rows.size()) {
                    return null;
                }

                DomainRow row = rows.get(rowIndex);
                switch (columnIndex) {
                case 0:
                    return row;
                case 1:
                    return row.getDomain()
                              .getAttributeCount();
                default:
                    return null;
                }
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return "Domain";
                case 1:
                    return "Attributes";
                default:
                    return null;
                }
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                case 0:
                    return DomainRow.class;
                case 1:
                    return Integer.class;
                default:
                    return null;
                }
            }
        };

        return model;
    }

    @SuppressWarnings("unchecked")
    void updateSelectedAttributes(TableRowSorter<TableModel> sorter,
                                  int[] rows) {

        CompositeMap compositeMap = session.getCompositeMap();
        if (compositeMap == null) {
            return;
        }

        NameValuePair<Integer> analysisType = (NameValuePair<Integer>) analysisTypes.getSelectedItem();
        int typeIndex = analysisType.getValue();

        IntScatterSet filteredDomains = new IntScatterSet();
        IntStream rowIndexStream;
        if (rows.length == 0) {
            rowIndexStream = IntStream.range(0, filteredTable.getTable()
                                                             .getRowCount());
        } else {
            rowIndexStream = Arrays.stream(rows);
        }
        rowIndexStream.map(i -> sorter.convertRowIndexToModel(i))
                      .forEach(i -> filteredDomains.add(i));

        applyColors(compositeMap, typeIndex, filteredDomains);
    }

    void configureSorter(TableRowSorter<TableModel> sorter) {
        sorter.setComparator(0, (DomainRow d1,
                                 DomainRow d2) -> String.CASE_INSENSITIVE_ORDER.compare(d1.getDomain()
                                                                                          .getName(),
                                                                                        d2.getDomain()
                                                                                          .getName()));
    }

    @SuppressWarnings("unchecked")
    void updateCompositeMap() {
        domainRows.clear();
        try {
            if (session == null) {
                return;
            }

            CompositeMap compositeMap = session.getCompositeMap();
            if (compositeMap == null) {
                return;
            }

            EnrichmentLandscape landscape = session.getEnrichmentLandscape();
            AnnotationProvider annotationProvider = landscape.getAnnotationProvider();

            boolean isBinary = annotationProvider.isBinary();
            boolean hasHighestDomains = compositeMap.getDomains(EnrichmentLandscape.TYPE_HIGHEST) != null;
            analysisTypes.setEnabled(!isBinary && hasHighestDomains);

            NameValuePair<Integer> pair = (NameValuePair<Integer>) analysisTypes.getSelectedItem();
            Integer analysisType = pair.getValue();
            if (isBinary && analysisType != EnrichmentLandscape.TYPE_HIGHEST) {
                analysisTypes.setSelectedIndex(0);
                return;
            } else if (!isBinary && !hasHighestDomains && analysisType != EnrichmentLandscape.TYPE_LOWEST) {
                analysisTypes.setSelectedIndex(1);
                return;
            }

            List<? extends Domain> domains = compositeMap.getDomains(analysisType);
            if (domains == null) {
                return;
            }

            int totalDomains = domains.size();
            List<double[]> colors = IntStream.range(0, totalDomains)
                                             .mapToObj(i -> {
                                                 double hue = (double) i / totalDomains;
                                                 return Util.hslToRgb(hue, 1.0, 0.5);
                                             })
                                             .collect(Collectors.toList());

            Random random = new Random(session.getColorSeed());
            Collections.shuffle(colors, random);

            int totalAttributes = annotationProvider.getAttributeCount();
            SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(analysisType, totalAttributes);

            Long[] nodeMappings = session.getNodeMappings();
            List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
            IntStream.range(0, domains.size())
                     .mapToObj(domainIndex -> {
                         Domain domain = domains.get(domainIndex);
                         DomainRow row = new DomainRow();

                         double[] color = colors.get(domainIndex);
                         domain.setColor(color);
                         row.setDomain(domain);

                         IntStream.range(0, domain.getAttributeCount())
                                  .map(i -> domain.getAttribute(i))
                                  .filter(j -> compositeMap.isTop(j, analysisType))
                                  .forEach(j -> {
                                      neighborhoods.stream()
                                                   .filter(n -> isSignificant.test(n, j))
                                                   .map(n -> nodeMappings[n.getNodeIndex()])
                                                   .forEach(suid -> row.addSignificant(suid));

                                  });
                         return row;
                     })
                     .forEach(row -> domainRows.add(row));

            applyColors(compositeMap, analysisType, null);
        } finally {
            domainTableModel.fireTableDataChanged();
            applyRowVisibility();
        }

    }

    void applyColors(CompositeMap compositeMap,
                     int typeIndex,
                     IntSet filteredDomains) {

        Long[] nodeMappings = session.getNodeMappings();

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();

        SafeUtil.checkSafeColumns(nodeTable);

        List<? extends Domain> domains = compositeMap.getDomains(typeIndex);

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
        int totalAttributes = annotationProvider.getAttributeCount();
        SignificancePredicate isSignificant = Neighborhood.getSignificancePredicate(typeIndex, totalAttributes);
        ScoringFunction score = Neighborhood.getScoringFunction(typeIndex);

        ColorFunction coloring = d -> (int) Math.round(Math.min(1, d * 1.5) * 255);

        eventService.notifyPresentationStateChanged(false);

        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
        neighborhoods.stream()
                     .forEach(n -> {
                         Long suid = nodeMappings[n.getNodeIndex()];
                         CyRow row = nodeTable.getRow(suid);

                         double[] color = computeColor(compositeMap, domains, filteredDomains, isSignificant, score, n,
                                                       typeIndex);
                         if (color == null) {
                             row.set(StyleFactory.COLOR_COLUMN, null);
                             row.set(StyleFactory.BRIGHTNESSS_COLUMN, 0D);
                             row.set(StyleFactory.HIGHLIGHT_COLUMN, null);
                         } else {
                             int red = coloring.get(color[0]);
                             int green = coloring.get(color[1]);
                             int blue = coloring.get(color[2]);

                             String hexColor = String.format("#%02x%02x%02x", red, green, blue);
                             row.set(StyleFactory.COLOR_COLUMN, hexColor);

                             double brightness = (color[0] + color[1] + color[2]) / 3;
                             brightness = Math.round(brightness * 10000.0) / 10000.0;
                             row.set(StyleFactory.BRIGHTNESSS_COLUMN, brightness);
                             row.set(StyleFactory.HIGHLIGHT_COLUMN, null);
                         }
                     });

        setDomainBrowserStyle(view);
    }

    void setDomainBrowserStyle(CyNetworkView view) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setDomainBrowserStyle(view));
            return;
        }

        VisualStyle style = visualMappingManager.getAllVisualStyles()
                                                .stream()
                                                .filter(s -> s.getTitle()
                                                              .equals(StyleFactory.DOMAIN_BROWSER_STYLE))
                                                .findFirst()
                                                .orElse(null);

        if (style == null) {
            style = styleFactory.createDomainBrowserStyle();
            visualMappingManager.addVisualStyle(style);
        }

        VisualStyle viewStyle = visualMappingManager.getVisualStyle(view);
        if (viewStyle != style) {
            visualMappingManager.setVisualStyle(style, view);
        }
    }

    double[] computeColor(CompositeMap compositeMap,
                          List<? extends Domain> domains,
                          IntSet filteredDomains,
                          SignificancePredicate isSignificant,
                          ScoringFunction score,
                          Neighborhood neighborhood,
                          int typeIndex) {
        double[] color = { 0, 0, 0 };
        int[] totalContributions = { 0 };

        Stream<? extends Domain> stream = domains.stream();
        if (filteredDomains != null) {
            stream = stream.filter(d -> filteredDomains.contains(d.getIndex()));
        }

        stream.forEach(domain -> {
            domain.forEachAttribute(j -> {
                if (!isSignificant.test(neighborhood, j) || !compositeMap.isTop(j, typeIndex)) {
                    return;
                }

                double opacity = score.get(neighborhood, j);
                double[] attributeColor = Util.multiply(opacity * opacity, domain.getColor());
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

    @Override
    public void expansionChanged(boolean isExpanded) {
    }

    static interface ColorFunction {
        int get(double value);
    }

}
