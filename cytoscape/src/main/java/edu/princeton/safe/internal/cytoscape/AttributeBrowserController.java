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
import edu.princeton.safe.internal.cytoscape.SafeController.IntLongMapper;
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

    Component panel;
    JComboBox<NameValuePair<AnalysisMethod>> analysisMethods;

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

    public void applyToSession() {
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    @SuppressWarnings({ "serial" })
    Component createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx", "[grow 0, right]rel[left]"));

        analysisMethods = new JComboBox<>();

        panel.add(new JLabel("Values to consider"));
        panel.add(analysisMethods, "wrap");

        attributes = new ArrayList<>();
        attributeTableModel = new ListTableModel<AttributeRow>(attributes) {
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
                AttributeRow row = rows.get(rowIndex);

                switch (columnIndex) {
                case 0:
                    return row.name;
                case 1:
                    return row.totalSignificant;
                }
                return null;
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return "Attribute";
                case 1:
                    return "Significant";
                }
                return null;
            }
        };

        FilteredTable<AttributeRow> filteredTable = new FilteredTable<>(attributeTableModel);

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        sorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(1, (Long x,
                                 Long y) -> (int) (y - x));

        JTable table = filteredTable.getTable();
        table.getSelectionModel()
             .addListSelectionListener(new ListSelectionListener() {

                 @Override
                 public void valueChanged(ListSelectionEvent e) {
                     if (e.getValueIsAdjusting()) {
                         return;
                     }

                     updateSelectedAttributes(sorter, table.getSelectedRows());
                 }
             });

        panel.add(filteredTable.getPanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        analysisMethods.addActionListener((e) -> {
            updateSelectedAttributes(sorter, table.getSelectedRows());
        });

        return panel;
    }

    AttributeScoringFunction getScoringFunction() {
        AnalysisMethod method = session.getAnalysisMethod();
        if (method == null) {
            return null;
        }

        switch (method) {
        case Highest:
            return (n,
                    j) -> n.getEnrichmentScore(j);
        case Lowest:
            return (n,
                    j) -> -Neighborhood.computeEnrichmentScore(1 - n.getPValue(j));
        case HighestAndLowest:
            return (n,
                    j) -> {
                double score1 = n.getEnrichmentScore(j);
                double score2 = Neighborhood.computeEnrichmentScore(1 - n.getPValue(j));
                if (score2 > score1) {
                    return -score2;
                }
                return score1;
            };
        default:
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unchecked")
    void updateSelectedAttributes(TableRowSorter<TableModel> sorter,
                                  int[] rows) {

        NameValuePair<AnalysisMethod> pair = (NameValuePair<AnalysisMethod>) analysisMethods.getSelectedItem();
        if (pair == null) {
            return;
        }

        session.setAnalysisMethod(pair.getValue());

        AttributeScoringFunction scoringFunction = getScoringFunction();
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
                         Long suid = nodeMappings.get(n.getNodeIndex());
                         CyRow row = nodeTable.getRow(suid);
                         row.set(StyleFactory.HIGHLIGHT_COLUMN, score);
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void updateEnrichmentLandscape() {
        attributes.clear();
        try {
            EnrichmentLandscape landscape = session.getEnrichmentLandscape();
            if (landscape == null) {
                return;
            }

            AnnotationProvider provider = landscape.getAnnotationProvider();

            NameValuePair[] model;
            if (provider.isBinary()) {
                model = new NameValuePair[] { new NameValuePair<>("Highest", AnalysisMethod.Highest) };
            } else {
                model = new NameValuePair[] { new NameValuePair<>("Highest", AnalysisMethod.Highest),
                                              new NameValuePair<>("Lowest", AnalysisMethod.Lowest),
                                              new NameValuePair<>("Highest and lowest",
                                                                  AnalysisMethod.HighestAndLowest) };
            }

            analysisMethods.setModel(new DefaultComboBoxModel<>(model));

            double threshold = Neighborhood.getEnrichmentThreshold(provider.getAttributeCount());

            IntLongMapper mapper = i -> landscape.getNeighborhoods()
                                                 .stream()
                                                 .filter(n -> n.getEnrichmentScore(i) > threshold)
                                                 .count();

            IntStream.range(0, provider.getAttributeCount())
                     .mapToObj(i -> new AttributeRow(i, provider.getAttributeLabel(i), mapper.map(i)))
                     .forEach(r -> attributes.add(r));
        } finally {
            attributeTableModel.fireTableDataChanged();
        }
    }

    @FunctionalInterface
    static interface AttributeScoringFunction {
        double get(Neighborhood neighborhood,
                   int attributeIndex);
    }

}
