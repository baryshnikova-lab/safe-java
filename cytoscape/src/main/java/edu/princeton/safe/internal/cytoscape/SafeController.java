package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.ColumnCreatedEvent;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedEvent;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedEvent;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.IntLongMap;
import com.carrotsearch.hppc.IntLongScatterMap;
import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.LongScatterSet;
import com.carrotsearch.hppc.cursors.LongIntCursor;

import edu.princeton.safe.AnalysisMethod;
import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.distance.EdgeWeightedDistanceMetric;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.UnweightedDistanceMetric;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.internal.cytoscape.UiUtil.FileSelectionMode;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.model.Neighborhood;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class SafeController implements SetCurrentNetworkViewListener, NetworkViewAboutToBeDestroyedListener,
        ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener, SessionLoadedListener {

    CyServiceRegistrar registrar;
    CySwingApplication application;
    DialogTaskManager taskManager;
    CyApplicationManager applicationManager;
    VisualMappingManager visualMappingManager;
    StyleFactory styleFactory;

    CytoPanelComponent2 cytoPanelComponent;
    VisualStyle attributeBrowserStyle;

    LongObjectMap<SafeSession> sessionsBySuid;
    IntLongMap suidsByNodeIndex;
    SafeSession session;
    Object sessionMutex = new Object();

    Object panelMutex = new Object();
    boolean panelVisible;

    JButton step1Button;

    JComboBox<String> nodeNames;
    DefaultComboBoxModel<String> nodeNamesModel;

    JComboBox<String> nodeIds;
    DefaultComboBoxModel<String> nodeIdsModel;

    JTextField annotationPath;

    List<AttributeRow> attributes;
    ListTableModel<AttributeRow> attributeTableModel;

    JComboBox<NameValuePair<AnalysisMethod>> analysisMethods;

    JComboBox<NameValuePair<Factory<DistanceMetric>>> distanceMetrics;
    JFormattedTextField distanceThreshold;

    JComboBox<NameValuePair<BackgroundMethod>> backgroundMethods;

    JComboBox<NameValuePair<Factory<RestrictionMethod>>> neighborhoodFilteringMethod;

    JFormattedTextField minimumLandscapeSize;

    JComboBox<NameValuePair<Factory<GroupingMethod>>> similarityMetric;
    JFormattedTextField similarityThreshold;

    JCheckBox forceUndirectedEdges;

    public SafeController(CyServiceRegistrar registrar,
                          CySwingApplication application,
                          DialogTaskManager taskManager,
                          CyApplicationManager applicationManager,
                          VisualMappingManager visualMappingManager,
                          StyleFactory styleFactory) {

        this.registrar = registrar;
        this.application = application;
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.visualMappingManager = visualMappingManager;
        this.styleFactory = styleFactory;

        sessionsBySuid = new LongObjectHashMap<>();

        JComponent panel = createPanel();
        cytoPanelComponent = new SafeCytoPanelComponent(panel);
        setNetworkView(applicationManager.getCurrentNetworkView());
    }

    @Override
    public void handleEvent(SetCurrentNetworkViewEvent event) {
        CyNetworkView view = event.getNetworkView();
        setNetworkView(view);
    }

    @Override
    public void handleEvent(SessionLoadedEvent event) {
        CyNetworkView view = applicationManager.getCurrentNetworkView();
        setNetworkView(view);
    }

    void setNetworkView(CyNetworkView view) {
        if (view == null) {
            setSession(null);
            return;
        }

        long suid = view.getSUID();
        synchronized (sessionMutex) {
            SafeSession session = sessionsBySuid.get(suid);
            if (session == null) {
                session = new SafeSession();
                session.networkView = view;
                session.nameColumn = CyRootNetwork.SHARED_NAME;
                session.idColumn = CyRootNetwork.SHARED_NAME;
                session.distanceThreshold = 0.5;
                session.forceUndirectedEdges = true;

                sessionsBySuid.put(suid, session);

                CyNetwork network = view.getModel();
                CyTable nodeTable = network.getDefaultNodeTable();
                sessionsBySuid.put(nodeTable.getSUID(), session);
            }
            setSession(session);
        }
    }

    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
        synchronized (sessionMutex) {
            CyNetworkView view = event.getNetworkView();
            CyNetwork network = view.getModel();
            CyTable nodeTable = network.getDefaultNodeTable();
            sessionsBySuid.remove(nodeTable.getSUID());

            long suid = view.getSUID();
            SafeSession session = sessionsBySuid.remove(suid);
            if (session != null && this.session == session) {
                setSession(null);
            }
        }
    }

    @Override
    public void handleEvent(ColumnCreatedEvent e) {
        checkTable(e.getSource());
    }

    @Override
    public void handleEvent(ColumnDeletedEvent e) {
        checkTable(e.getSource());
    }

    @Override
    public void handleEvent(ColumnNameChangedEvent e) {
        e.getSource();
    }

    void checkTable(CyTable table) {
        if (session == null) {
            return;
        }

        SafeSession session = sessionsBySuid.get(table.getSUID());
        if (this.session == session) {
            updateColumns();
        }
    }

    void updateColumns() {
        updateColumnList();
    }

    private void setSession(SafeSession session) {
        synchronized (sessionMutex) {
            this.session = session;
            updateColumnList();
            step1Button.setEnabled(session != null);

            distanceThreshold.setValue(session.getDistanceThreshold());
            forceUndirectedEdges.setSelected(session.getForceUndirectedEdges());
        }
    }

    void updateColumnList() {
        synchronized (sessionMutex) {
            nodeNamesModel.removeAllElements();
            nodeIdsModel.removeAllElements();

            if (session == null) {
                return;
            }

            CyNetworkView view = session.getNetworkView();
            CyNetwork model = view.getModel();
            CyTable table = model.getDefaultNodeTable();
            table.getColumns()
                 .stream()
                 .filter(c -> c.getType()
                               .equals(String.class))
                 .map(c -> c.getName())
                 .sorted(String.CASE_INSENSITIVE_ORDER)
                 .forEach(new Consumer<String>() {
                     @Override
                     public void accept(String name) {
                         nodeNamesModel.addElement(name);
                         nodeIdsModel.addElement(name);
                     }
                 });

            nodeNames.setSelectedItem(session.getNameColumn());
            nodeIds.setSelectedItem(session.getIdColumn());
        }
    }

    JButton createChooseButton() {
        JButton button = new JButton("Choose");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Set<String> extensions = new HashSet<>();
                try {
                    File file = UiUtil.getFile(application.getJFrame(), "Select Annotation File", new File("."),
                                               "Annotation File", extensions, FileSelectionMode.OPEN_FILE);
                    if (file != null) {
                        annotationPath.setText(file.getPath());
                    }
                } catch (IOException e) {
                    fail(e, "Unexpected error while reading annotation file");
                }
            }
        });
        return button;
    }

    JButton createStep1Button() {
        JButton button = new JButton("Build");
        button.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                session.setNameColumn((String) nodeNames.getSelectedItem());
                session.setIdColumn((String) nodeIds.getSelectedItem());
                session.setAnnotationFile(new File(annotationPath.getText()));

                NameValuePair<AnalysisMethod> analysisPair = (NameValuePair<AnalysisMethod>) analysisMethods.getSelectedItem();
                session.setAnalysisMethod(analysisPair.getValue());

                NameValuePair<Factory<DistanceMetric>> distancePair = (NameValuePair<Factory<DistanceMetric>>) distanceMetrics.getSelectedItem();
                session.setDistanceMetric(distancePair.getValue()
                                                      .create());

                session.setDistanceThreshold(getDistanceThreshold());

                NameValuePair<BackgroundMethod> backgroundPair = (NameValuePair<BackgroundMethod>) backgroundMethods.getSelectedItem();
                session.setBackgroundMethod(backgroundPair.getValue());

                SafeTaskFactory factory = new SafeTaskFactory(session, SafeController.this);
                taskManager.execute(factory.createTaskIterator());
            }
        });
        return button;
    }

    void addSection(JPanel panel,
                    String title) {
        addSection(panel, title, "center");
    }

    void addSubsection(JPanel panel,
                       String title) {
        addSection(panel, title, "leading");
    }

    void addSection(JPanel panel,
                    String title,
                    String alignment) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont()
                           .deriveFont(Font.BOLD));

        panel.add(label, "alignx " + alignment + ", span 2, wrap");
    }

    @SuppressWarnings("unchecked")
    JComponent createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx", "[grow 0, right]rel[left]"));

        nodeNamesModel = new DefaultComboBoxModel<>();
        nodeIdsModel = new DefaultComboBoxModel<>();

        nodeNames = new JComboBox<>(nodeNamesModel);
        nodeIds = new JComboBox<>(nodeIdsModel);

        addSection(panel, "Step 1: Build Enrichment Landscapes");
        panel.add(new JLabel("Node names"));
        panel.add(nodeNames, "wrap");

        panel.add(new JLabel("Annotation ids"));
        panel.add(nodeIds, "wrap");

        JButton chooseAnnotationFileButton = createChooseButton();
        annotationPath = new JTextField();

        panel.add(new JLabel("Annotation file"));
        panel.add(annotationPath, "growx, wmax 200, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        analysisMethods = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Highest", AnalysisMethod.Highest),
                                                                new NameValuePair<>("Lowest", AnalysisMethod.Lowest),
                                                                new NameValuePair<>("Highest and lowest",
                                                                                    AnalysisMethod.HighestAndLowest) });

        distanceMetrics = new JComboBox<>(new NameValuePair[] { new NameValuePair<Factory<DistanceMetric>>("Map-based",
                                                                                                           () -> new MapBasedDistanceMetric()),
                                                                new NameValuePair<Factory<DistanceMetric>>("Edge-weighted",
                                                                                                           () -> new EdgeWeightedDistanceMetric()),
                                                                new NameValuePair<Factory<DistanceMetric>>("Unweighted",
                                                                                                           () -> new UnweightedDistanceMetric()) });

        distanceThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        backgroundMethods = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("All nodes in network",
                                                                                      BackgroundMethod.Network),
                                                                  new NameValuePair<>("All nodes in annotation standard",
                                                                                      BackgroundMethod.Annotation) });

        forceUndirectedEdges = new JCheckBox("Assume edges are undirected");

        panel.add(new JLabel("Values to consider"));
        panel.add(analysisMethods, "wrap");

        panel.add(new JLabel("Distance metric"));
        panel.add(distanceMetrics, "wrap");

        panel.add(new JLabel("Max. distance threshold"));
        panel.add(distanceThreshold, "growx, wmax 200, wrap");

        panel.add(new JLabel("Background"));
        panel.add(backgroundMethods, "wrap");

        panel.add(forceUndirectedEdges, "skip 1, wrap");

        step1Button = createStep1Button();
        panel.add(step1Button, "span 2, tag apply, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        addSection(panel, "Step 2: Preview Enrichment Landscapes");

        panel.add(createAttributePanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        addSection(panel, "Step 3: Build Composite Map");

        neighborhoodFilteringMethod = new JComboBox<>(new NameValuePair[] { new NameValuePair<Factory<RestrictionMethod>>("Radius-based",
                                                                                                                          () -> new RadiusBasedRestrictionMethod(getDistanceThreshold())) });
        minimumLandscapeSize = new JFormattedTextField(NumberFormat.getIntegerInstance());

        addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Neighborhood filtering method"));
        panel.add(neighborhoodFilteringMethod, "wrap");
        panel.add(new JLabel("Min. landscape size"));
        panel.add(minimumLandscapeSize, "growx, wmax 200, wrap");

        similarityMetric = new JComboBox<>(new NameValuePair[] { new NameValuePair<Factory<GroupingMethod>>("Jaccard",
                                                                                                            () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                                                                 DistanceMethod.JACCARD)),
                                                                 new NameValuePair<Factory<GroupingMethod>>("Pearson",
                                                                                                            () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                                                                 DistanceMethod.CORRELATION)) });
        similarityThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        addSubsection(panel, "Group Attributes");
        panel.add(new JLabel("Similarity metric"));
        panel.add(similarityMetric, "wrap");
        panel.add(new JLabel("Similarity threshold"));
        panel.add(similarityThreshold, "growx, wmax 200, wrap");

        JScrollPane container = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        if (UiUtil.isMacOSX()) {
            container.setOpaque(false);
            container.setBorder(BorderFactory.createEmptyBorder());
            JViewport viewport = container.getViewport();
            viewport.setOpaque(false);
        }

        return container;
    }

    double getDistanceThreshold() {
        return ((Number) distanceThreshold.getValue()).doubleValue();
    }

    double getClusterThreshold() {
        return Double.parseDouble(similarityThreshold.getText());
    }

    @SuppressWarnings("serial")
    Component createAttributePanel() {
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

                     EnrichmentLandscape landscape = session.getEnrichmentLandscape();
                     List<? extends Neighborhood> neighborhoods = landscape.getNeighborhoods();
                     LongScatterSet set = new LongScatterSet();
                     AnnotationProvider annotationProvider = landscape.getAnnotationProvider();
                     int totalAttributes = annotationProvider.getAttributeCount();
                     double threshold = Neighborhood.getEnrichmentThreshold(totalAttributes);

                     int[] rows = table.getSelectedRows();
                     Arrays.stream(rows)
                           .map(i -> sorter.convertRowIndexToModel(i))
                           .flatMap(new IntFunction<IntStream>() {
                               public IntStream apply(int attributeIndex) {
                                   return neighborhoods.stream()
                                                       .filter(n -> n.getEnrichmentScore(attributeIndex) > threshold)
                                                       .mapToInt(n -> n.getNodeIndex());
                               }
                           })
                           .mapToLong(i -> suidsByNodeIndex.get(i))
                           .forEach(i -> set.add(i));

                     CyNetworkView view = session.getNetworkView();
                     CyNetwork network = view.getModel();
                     CyTable table = network.getDefaultNodeTable();

                     for (CyRow row : table.getAllRows()) {
                         Long id = row.get(CyIdentifiable.SUID, Long.class);
                         row.set(CyNetwork.SELECTED, set.contains(id));
                     }
                 }
             });

        return filteredTable.getPanel();
    }

    public void showPanel() {
        synchronized (panelMutex) {
            if (!panelVisible) {
                registrar.registerService(cytoPanelComponent, CytoPanelComponent.class, new Properties());
                panelVisible = true;
            }

            CytoPanel panel = application.getCytoPanel(cytoPanelComponent.getCytoPanelName());
            int index = panel.indexOfComponent(cytoPanelComponent.getIdentifier());

            if (panel.getState() == CytoPanelState.HIDE) {
                panel.setState(CytoPanelState.DOCK);
            }

            if (index != -1) {
                panel.setSelectedIndex(index);
            }
        }
    }

    public void hidePanel() {
        synchronized (panelMutex) {
            if (!panelVisible) {
                return;
            }
            registrar.unregisterService(cytoPanelComponent, CytoPanelComponent.class);
            panelVisible = false;
        }
    }

    void fail(IOException e,
              String string) {
        // TODO Auto-generated method stub
        e.printStackTrace();
    }

    public void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        session.setEnrichmentLandscape(landscape);

        attributes.clear();

        AnnotationProvider provider = landscape.getAnnotationProvider();
        double threshold = Neighborhood.getEnrichmentThreshold(provider.getAttributeCount());

        IntLongMapper mapper = i -> landscape.getNeighborhoods()
                                             .stream()
                                             .filter(n -> n.getEnrichmentScore(i) > threshold)
                                             .count();

        IntStream.range(0, provider.getAttributeCount())
                 .mapToObj(i -> new AttributeRow(i, provider.getAttributeLabel(i), mapper.map(i)))
                 .forEach(r -> attributes.add(r));

        attributeTableModel.fireTableDataChanged();
    }

    void setAttributeBrowserStyle() {
        if (attributeBrowserStyle == null) {
            attributeBrowserStyle = styleFactory.createAttributeBrowserStyle();
            visualMappingManager.addVisualStyle(attributeBrowserStyle);
        }

        VisualStyle style = visualMappingManager.getCurrentVisualStyle();
        if (style == attributeBrowserStyle) {
            return;
        }
        visualMappingManager.setCurrentVisualStyle(attributeBrowserStyle);
    }

    @FunctionalInterface
    static interface IntLongMapper {
        long map(int value);
    }

    @FunctionalInterface
    static interface Factory<T> {
        T create();
    }

    public void setNodeMappings(LongIntMap nodeMappings) {
        suidsByNodeIndex = new IntLongScatterMap(nodeMappings.size());
        nodeMappings.forEach((Consumer<? super LongIntCursor>) (LongIntCursor c) -> suidsByNodeIndex.put(c.value,
                                                                                                         c.key));
    }
}
