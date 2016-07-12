package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.ComboBoxModel;
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
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.internal.ScoringFunction;
import edu.princeton.safe.internal.SignificancePredicate;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.Domain;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import net.miginfocom.swing.MigLayout;

public class DomainBrowserController {

    VisualMappingManager visualMappingManager;
    StyleFactory styleFactory;

    SafeSession session;
    List<DomainRow> domainRows;
    ListTableModel<DomainRow> domainTableModel;
    JTable table;
    VisualStyle domainBrowserStyle;

    Component panel;
    JComboBox<NameValuePair<Integer>> analysisTypes;
    FilteredTable<DomainRow> filteredTable;

    public DomainBrowserController(VisualMappingManager visualMappingManager,
                                   StyleFactory styleFactory) {

        this.visualMappingManager = visualMappingManager;
        this.styleFactory = styleFactory;
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

        filteredTable = new FilteredTable<>(domainTableModel);

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        configureSorter(sorter);

        JTable table = filteredTable.getTable();
        table.getSelectionModel()
             .addListSelectionListener(createListSelectionListener(table, sorter));

        analysisTypes.addActionListener((e) -> {
            updateAnalysisType();
        });

        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));
        panel.add(new JLabel("Values to consider"));
        panel.add(analysisTypes, "wrap");
        panel.add(filteredTable.getPanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        return panel;
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

                updateSelectedAttributes(sorter, table.getSelectedRows());
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
                    String color = String.format("#%02x%02x%02x", Math.round(row.color[0] * 255),
                                                 Math.round(row.color[1] * 255), Math.round(row.color[2] * 255));
                    return String.format("<html><span style=\"color: %s; font-family: FontAwesome\">\uf111</span> %s",
                                         color, row.name);
                case 1:
                    return row.domain.getAttributeCount();
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
                    return String.class;
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
        Arrays.stream(rows)
              .map(i -> sorter.convertRowIndexToModel(i))
              .forEach(i -> filteredDomains.add(i));

        applyColors(compositeMap, typeIndex, filteredDomains);
    }

    void configureSorter(TableRowSorter<TableModel> sorter) {
        sorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
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
            analysisTypes.setEnabled(!isBinary);

            NameValuePair<Integer> pair = (NameValuePair<Integer>) analysisTypes.getSelectedItem();
            Integer analysisType = pair.getValue();
            if (isBinary && analysisType != 0) {
                analysisTypes.setSelectedIndex(0);
                return;
            }

            List<? extends Domain> domains = compositeMap.getDomains(analysisType);
            int totalDomains = domains.size();
            List<double[]> colors = IntStream.range(0, totalDomains)
                                             .mapToObj(i -> {
                                                 double hue = (double) i / totalDomains;
                                                 return Util.hslToRgb(hue, 1.0, 0.5);
                                             })
                                             .collect(Collectors.toList());

            Collections.shuffle(colors);

            IntStream.range(0, domains.size())
                     .mapToObj(domainIndex -> {
                         Domain domain = domains.get(domainIndex);
                         DomainRow row = new DomainRow();
                         int attributeIndex = domain.getAttribute(0);
                         row.name = annotationProvider.getAttributeLabel(attributeIndex);
                         row.domain = domain;
                         row.color = colors.get(domainIndex);
                         return row;
                     })
                     .forEach(row -> domainRows.add(row));

            applyColors(compositeMap, analysisType, null);
        } finally {
            domainTableModel.fireTableDataChanged();
        }

    }

    void applyColors(CompositeMap compositeMap,
                     int typeIndex,
                     IntSet filteredDomains) {

        IntObjectMap<Long> nodeMappings = session.getNodeMappings();

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

        List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
        neighborhoods.stream()
                     .forEach(n -> {
                         Long suid = nodeMappings.get(n.getNodeIndex());
                         CyRow row = nodeTable.getRow(suid);

                         double[] color = computeColor(compositeMap, domains, filteredDomains, isSignificant, score, n,
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
            int index = domain.getIndex();
            domain.forEachAttribute(j -> {
                if (!isSignificant.test(neighborhood, j) || !compositeMap.isTop(j, typeIndex)) {
                    return;
                }

                DomainRow row = domainRows.get(index);
                double opacity = score.get(neighborhood, j);
                double[] attributeColor = Util.multiply(opacity * opacity, row.color);
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
