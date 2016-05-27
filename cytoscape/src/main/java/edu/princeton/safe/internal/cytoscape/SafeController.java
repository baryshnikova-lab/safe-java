package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
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

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;

import edu.princeton.safe.AnnotationProvider;
import edu.princeton.safe.IndexedDoubleConsumer;
import edu.princeton.safe.internal.cytoscape.UiUtil.FileSelectionMode;
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
    JComboBox analysisTypes;
    JComboBox distanceMetrics;
    JTextField distanceThreshold;
    JComboBox backgroundMethod;
    private JComboBox neighborhoodFilteringMethod;
    private JTextField minimumLandscapeSize;
    private JComboBox similarityMetric;
    private JTextField similarityThreshold;

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
            @Override
            public void actionPerformed(ActionEvent e) {
                session.setNameColumn((String) nodeNames.getSelectedItem());
                session.setIdColumn((String) nodeIds.getSelectedItem());
                session.setAnnotationFile(new File(annotationPath.getText()));

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
        panel.add(annotationPath, "growx, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        analysisTypes = new JComboBox<>(new String[] { "Highest", "Lowest" });
        distanceMetrics = new JComboBox<>(new String[] { "Map-based", "Edge-weighted", "Unweighted" });

        distanceThreshold = new JTextField();
        backgroundMethod = new JComboBox<>(new String[] { "All nodes in network", "All nodes in annotation standard" });

        panel.add(new JLabel("Values to consider"));
        panel.add(analysisTypes, "wrap");

        panel.add(new JLabel("Distance metric"));
        panel.add(distanceMetrics, "wrap");

        panel.add(new JLabel("Max. distance threshold"));
        panel.add(distanceThreshold, "growx, wrap");

        panel.add(new JLabel("Background"));
        panel.add(backgroundMethod, "wrap");

        step1Button = createStep1Button();
        panel.add(step1Button, "span 2, tag apply, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        addSection(panel, "Step 2: Preview Enrichment Landscapes");

        panel.add(createAttributePanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        addSection(panel, "Step 3: Build Composite Map");

        neighborhoodFilteringMethod = new JComboBox<>(new String[] { "Radius-based" });
        minimumLandscapeSize = new JTextField();

        addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Neighborhood filtering method"));
        panel.add(neighborhoodFilteringMethod, "wrap");
        panel.add(new JLabel("Min. landscape size"));
        panel.add(minimumLandscapeSize, "growx, wrap");

        similarityMetric = new JComboBox<>(new String[] { "Jaccard", "Pearson" });
        similarityThreshold = new JTextField();

        addSubsection(panel, "Group Attributes");
        panel.add(new JLabel("Similarity metric"));
        panel.add(similarityMetric, "wrap");
        panel.add(new JLabel("Similarity threshold"));
        panel.add(similarityThreshold, "growx, wrap");

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
                    return row.score;
                }
                return null;
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return "Attribute";
                case 1:
                    return "Score";
                }
                return null;
            }
        };

        FilteredTable<AttributeRow> filteredTable = new FilteredTable<>(attributeTableModel);

        TableRowSorter<TableModel> sorter = filteredTable.getSorter();
        sorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
        sorter.setComparator(1, (Double x,
                                 Double y) -> Double.compare(y, x));

        JTable table = filteredTable.getTable();
        table.getSelectionModel()
             .addListSelectionListener(new ListSelectionListener() {
                 @Override
                 public void valueChanged(ListSelectionEvent e) {
                     if (e.getValueIsAdjusting()) {
                         return;
                     }

                     int[] rows = table.getSelectedRows();
                     System.out.printf("Selected: %s\n", Arrays.stream(rows)
                                                               .map(i -> sorter.convertRowIndexToModel(i))
                                                               .mapToObj(i -> String.valueOf(i))
                                                               .collect(Collectors.joining(", ")));
                     setAttributeBrowserStyle();

                     CyNetworkView view = session.getNetworkView();
                     CyNetwork network = view.getModel();
                     CyTable table = network.getDefaultNodeTable();
                     CyColumn column = table.getColumn("SAFE Highlight");
                     if (column == null) {
                         table.createColumn("SAFE Highlight", Double.class, false, 0D);
                     }

                     // map node
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

    public void setAttributes(AnnotationProvider provider) {
        attributes.clear();
        IntDoubleMapper mapper;
        if (provider.isBinary()) {
            mapper = i -> provider.getNetworkNodeCountForAttribute(i);
        } else {
            mapper = new IntDoubleMapper() {
                @Override
                public double map(int attributeIndex) {
                    double[] sum = { 0 };
                    int[] count = { 0 };
                    provider.forEachAttributeValue(attributeIndex, new IndexedDoubleConsumer() {
                        @Override
                        public void accept(int index,
                                           double value) {
                            sum[0] += value;
                            count[0]++;
                        }
                    });
                    return sum[0] / count[0];
                }
            };
        }
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
    static interface IntDoubleMapper {
        double map(int value);
    }
}
