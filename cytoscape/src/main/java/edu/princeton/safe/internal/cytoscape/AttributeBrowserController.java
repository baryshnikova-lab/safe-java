package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;

import com.carrotsearch.hppc.IntObjectMap;

import edu.princeton.safe.AnalysisMethod;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import net.miginfocom.swing.MigLayout;

public class AttributeBrowserController {

    VisualMappingManager visualMappingManager;
    StyleFactory styleFactory;

    SafeSession session;
    List<AttributeRow> attributes;
    ListTableModel<AttributeRow> attributeTableModel;
    VisualStyle attributeBrowserStyle;
    TableColumn optionalColumn;

    Component panel;
    JComboBox<NameValuePair<AnalysisMethod>> analysisMethods;
    FilteredTable<AttributeRow> filteredTable;

    public AttributeBrowserController(VisualMappingManager visualMappingManager,
                                      StyleFactory styleFactory) {

        this.visualMappingManager = visualMappingManager;
        this.styleFactory = styleFactory;
    }

    public void setSession(SafeSession session) {
        this.session = session;
        if (session == null) {
            return;
        }
        SafeUtil.setSelected(analysisMethods, session.getAnalysisMethod());
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    Component createPanel() {
        analysisMethods = new JComboBox<>();
        attributes = new ArrayList<>();
        attributeTableModel = createAttributeTableModel();

        filteredTable = new FilteredTable<>(attributeTableModel);

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        configureSorter(sorter);

        JTable table = filteredTable.getTable();
        table.getSelectionModel()
             .addListSelectionListener(createListSelectionListener(table, sorter));

        analysisMethods.addActionListener((e) -> {
            updateAnalysisMethod();
            updateTableStructure();
        });

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));
        panel.add(new JLabel("Values to consider"));
        panel.add(analysisMethods, "wrap");
        panel.add(filteredTable.getPanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        return panel;
    }

    void updateTableStructure() {
        JTable table = filteredTable.getTable();
        TableColumnModel columnModel = table.getColumnModel();

        int columnCount = table.getColumnCount();
        AnalysisMethod analysisMethod = session.getAnalysisMethod();
        if (analysisMethod == AnalysisMethod.HighestAndLowest && columnCount != 3) {
            columnModel.addColumn(optionalColumn);
        }

        if (analysisMethod != AnalysisMethod.HighestAndLowest && columnCount == 3) {
            optionalColumn = columnModel.getColumn(2);
            columnModel.removeColumn(optionalColumn);
        }

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        configureSorter(sorter);

        updateSelectedAttributes(sorter, table.getSelectedRows());
    }

    ListSelectionListener createListSelectionListener(JTable table,
                                                      TableRowSorter<TableModel> sorter) {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                updateSelectedAttributes(sorter, table.getSelectedRows());
            }
        };
    }

    @SuppressWarnings("unchecked")
    void updateAnalysisMethod() {
        NameValuePair<AnalysisMethod> pair = (NameValuePair<AnalysisMethod>) analysisMethods.getSelectedItem();
        if (pair == null) {
            return;
        }

        AnalysisMethod method = pair.getValue();
        session.setAnalysisMethod(method);

        attributeTableModel.fireTableStructureChanged();
    }

    @SuppressWarnings("serial")
    ListTableModel<AttributeRow> createAttributeTableModel() {
        ListTableModel<AttributeRow> model = new ListTableModel<AttributeRow>(attributes) {
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public Object getValueAt(int rowIndex,
                                     int columnIndex) {
                if (rowIndex < 0 || rowIndex >= rows.size()) {
                    return null;
                }

                AttributeRow row = rows.get(rowIndex);
                AnalysisMethod analysisMethod = session.getAnalysisMethod();

                switch (columnIndex) {
                case 0:
                    return row.name;
                case 1:
                    switch (analysisMethod) {
                    case Highest:
                    case HighestAndLowest:
                        return row.totalHighest;
                    case Lowest:
                        return row.totalLowest;
                    default:
                        return null;
                    }
                case 2:
                    switch (analysisMethod) {
                    case HighestAndLowest:
                        return row.totalLowest;
                    case Highest:
                    case Lowest:
                    default:
                        return null;
                    }
                }
                return null;
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return "Attribute";
                case 1:
                    if (session != null) {
                        AnalysisMethod analysisMethod = session.getAnalysisMethod();
                        if (analysisMethod == AnalysisMethod.HighestAndLowest) {
                            return "Highest";
                        }
                    }
                    return "Significant";
                case 2:
                    return "Lowest";
                }
                return null;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                case 2:
                    return Long.class;
                default:
                    return null;
                }
            }
        };

        return model;
    }

    void configureSorter(TableRowSorter<TableModel> sorter) {
        sorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
    }

    ScoringFunction getScoringFunction() {
        AnalysisMethod method = session.getAnalysisMethod();
        if (method == null) {
            return null;
        }

        switch (method) {
        case Highest:
            return Neighborhood.HIGHEST_SCORE;
        case Lowest:
            return (n,
                    j) -> -Neighborhood.LOWEST_SCORE.get(n, j);
        case HighestAndLowest:
            return (n,
                    j) -> {
                double score1 = Neighborhood.HIGHEST_SCORE.get(n, j);
                double score2 = Neighborhood.LOWEST_SCORE.get(n, j);
                if (score2 > score1) {
                    return -score2;
                }
                return score1;
            };
        default:
            throw new RuntimeException();
        }
    }

    void updateSelectedAttributes(TableRowSorter<TableModel> sorter,
                                  int[] rows) {

        ScoringFunction scoringFunction = getScoringFunction();
        if (scoringFunction == null) {
            return;
        }

        IntObjectMap<Long> nodeMappings = session.getNodeMappings();

        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();

        SafeUtil.checkSafeColumns(nodeTable);

        EnrichmentLandscape landscape = session.getEnrichmentLandscape();
        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
        neighborhoods.stream()
                     .forEach(n -> {
                         double score = Arrays.stream(rows)
                                              .map(i -> sorter.convertRowIndexToModel(i))
                                              .mapToDouble(i -> scoringFunction.get(n, i))
                                              .reduce(0, (x,
                                                          y) -> Math.abs(x) > Math.abs(y) ? x : y);

                         if (!Double.isFinite(score)) {
                             score = 0;
                         }

                         Long suid = nodeMappings.get(n.getNodeIndex());
                         CyRow row = nodeTable.getRow(suid);
                         row.set(StyleFactory.HIGHLIGHT_COLUMN, score);

                         double brightness = Math.abs(score);
                         row.set(StyleFactory.BRIGHTNESSS_COLUMN, brightness);
                     });

        setAttributeBrowserStyle(view);
    }

    void setAttributeBrowserStyle(CyNetworkView view) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setAttributeBrowserStyle(view));
            return;
        }

        if (attributeBrowserStyle == null) {
            attributeBrowserStyle = visualMappingManager.getAllVisualStyles()
                                                        .stream()
                                                        .filter(s -> s.getTitle()
                                                                      .equals(StyleFactory.ATTRIBUTE_BROWSER_STYLE))
                                                        .findFirst()
                                                        .orElse(null);
        }

        if (attributeBrowserStyle == null) {
            attributeBrowserStyle = styleFactory.createAttributeBrowserStyle();
            visualMappingManager.addVisualStyle(attributeBrowserStyle);
        }

        VisualStyle viewStyle = visualMappingManager.getVisualStyle(view);
        if (viewStyle != attributeBrowserStyle) {
            visualMappingManager.setVisualStyle(attributeBrowserStyle, view);
        }
    }

    void updateEnrichmentLandscape() {
        attributes.clear();
        try {
            EnrichmentLandscape landscape = session.getEnrichmentLandscape();
            if (landscape == null) {
                return;
            }

            AnnotationProvider provider = landscape.getAnnotationProvider();
            updateAnalysisMethods(provider);
            updateAnalysisMethod();
            updateTableStructure();

            int totalAttributes = provider.getAttributeCount();

            SignificancePredicate isHighest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_HIGHEST,
                                                                                    totalAttributes);

            SignificancePredicate isLowest = Neighborhood.getSignificancePredicate(EnrichmentLandscape.TYPE_LOWEST,
                                                                                   totalAttributes);

            IntStream.range(0, provider.getAttributeCount())
                     .mapToObj(i -> {
                         AttributeRow row = new AttributeRow(i, provider.getAttributeLabel(i), 0, 0);
                         row.totalHighest = landscape.getNeighborhoods()
                                                     .stream()
                                                     .filter(n -> isHighest.test(n, i))
                                                     .count();
                         if (!provider.isBinary()) {
                             row.totalLowest = landscape.getNeighborhoods()
                                                        .stream()
                                                        .filter(n -> isLowest.test(n, i))
                                                        .count();
                         }
                         return row;
                     })
                     .forEach(r -> attributes.add(r));

        } finally {
            attributeTableModel.fireTableDataChanged();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void updateAnalysisMethods(AnnotationProvider provider) {
        NameValuePair[] model;
        if (provider.isBinary()) {
            model = new NameValuePair[] { new NameValuePair<>("Highest", AnalysisMethod.Highest) };
        } else {
            model = new NameValuePair[] { new NameValuePair<>("Highest and lowest", AnalysisMethod.HighestAndLowest),
                                          new NameValuePair<>("Highest", AnalysisMethod.Highest),
                                          new NameValuePair<>("Lowest", AnalysisMethod.Lowest) };
        }

        analysisMethods.setModel(new DefaultComboBoxModel<>(model));
        SafeUtil.setSelected(analysisMethods, session.getAnalysisMethod());
    }

}
