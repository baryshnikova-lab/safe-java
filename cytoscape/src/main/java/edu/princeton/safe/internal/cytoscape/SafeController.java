package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.text.NumberFormat;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JViewport;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelState;
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

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;

import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class SafeController implements SetCurrentNetworkViewListener, NetworkViewAboutToBeDestroyedListener,
        ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener, SessionLoadedListener {

    CyServiceRegistrar registrar;
    CySwingApplication application;
    CyApplicationManager applicationManager;

    CytoPanelComponent2 cytoPanelComponent;

    LongObjectMap<SafeSession> sessionsBySuid;
    SafeSession session;
    Object sessionMutex = new Object();

    Object panelMutex = new Object();
    boolean panelVisible;

    Component panel;

    JComboBox<NameValuePair<Factory<RestrictionMethod>>> neighborhoodFilteringMethod;

    JFormattedTextField minimumLandscapeSize;

    JComboBox<NameValuePair<Factory<GroupingMethod>>> similarityMetric;
    JFormattedTextField similarityThreshold;

    ImportPanelController importPanel;
    AttributeBrowserController attributeBrowser;

    public SafeController(CyServiceRegistrar registrar,
                          CySwingApplication application,
                          CyApplicationManager applicationManager,
                          ImportPanelController importPanel,
                          AttributeBrowserController attributeBrowser) {

        this.registrar = registrar;
        this.application = application;
        this.applicationManager = applicationManager;

        this.importPanel = importPanel;
        this.attributeBrowser = attributeBrowser;

        sessionsBySuid = new LongObjectHashMap<>();
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
                session = createNewSession(view);
                sessionsBySuid.put(suid, session);

                CyNetwork network = view.getModel();
                CyTable nodeTable = network.getDefaultNodeTable();
                sessionsBySuid.put(nodeTable.getSUID(), session);
            }
            setSession(session);
        }
    }

    SafeSession createNewSession(CyNetworkView view) {
        SafeSession session = new SafeSession();
        session.setNetworkView(view);
        session.setNameColumn(CyRootNetwork.SHARED_NAME);
        session.setIdColumn(CyRootNetwork.SHARED_NAME);
        session.setDistanceThreshold(0.5);
        session.setForceUndirectedEdges(true);
        session.setMinimumLandscapeSize(10);
        session.setSimilarityThreshold(0.5);

        return session;
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
        checkTable(e.getSource());
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
            attributeBrowser.setSession(session);
            importPanel.setSession(session);
            if (session == null) {
                return;
            }

            minimumLandscapeSize.setValue(session.getMinimumLandscapeSize());

            similarityThreshold.setValue(session.getSimilarityThreshold());
        }
    }

    void updateColumnList() {
        synchronized (sessionMutex) {
            importPanel.updateColumnList();
        }
    }

    Component getPanel() {
        if (panel == null) {
            Component panel = createPanel();
            cytoPanelComponent = new SafeCytoPanelComponent(panel);
            setNetworkView(applicationManager.getCurrentNetworkView());
        }
        return panel;
    }

    @SuppressWarnings("unchecked")
    Component createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx", "[grow 0, right]rel[left]"));

        SafeUtil.addSection(panel, "Step 1: Build Enrichment Landscapes");

        panel.add(importPanel.getPanel(), "span 2, grow, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        SafeUtil.addSection(panel, "Step 2: Preview Enrichment Landscapes");

        panel.add(attributeBrowser.getPanel(), "span 2, grow, hmin 100, hmax 200, wrap");

        panel.add(new JSeparator(), "span 2, growx, hmin 10, wrap");
        SafeUtil.addSection(panel, "Step 3: Build Composite Map");

        neighborhoodFilteringMethod = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Radius-based",
                                                                                                new Factory<>("radius",
                                                                                                              () -> new RadiusBasedRestrictionMethod(getDistanceThreshold()))) });
        minimumLandscapeSize = new JFormattedTextField(NumberFormat.getIntegerInstance());

        SafeUtil.addSubsection(panel, "Filter Attributes");
        panel.add(new JLabel("Neighborhood filtering method"));
        panel.add(neighborhoodFilteringMethod, "wrap");
        panel.add(new JLabel("Min. landscape size"));
        panel.add(minimumLandscapeSize, "growx, wmax 200, wrap");

        similarityMetric = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Jaccard",
                                                                                     new Factory<>("jaccard",
                                                                                                   () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                                                        DistanceMethod.JACCARD))),
                                                                 new NameValuePair<>("Pearson",
                                                                                     new Factory<>("pearson",
                                                                                                   () -> new ClusterBasedGroupingMethod(getClusterThreshold(),
                                                                                                                                        DistanceMethod.CORRELATION))) });
        similarityThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        SafeUtil.addSubsection(panel, "Group Attributes");
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
        return 65;
    }

    double getClusterThreshold() {
        return Double.parseDouble(similarityThreshold.getText());
    }

    public void showPanel() {
        synchronized (panelMutex) {
            if (cytoPanelComponent == null) {
                getPanel();
                setNetworkView(applicationManager.getCurrentNetworkView());
            }

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

    @FunctionalInterface
    static interface IntLongMapper {
        long map(int value);
    }

    @FunctionalInterface
    static interface FactoryMethod<T> {
        T create();
    }

    static class Factory<T> {
        String id;
        FactoryMethod<T> method;

        public Factory(String id,
                       FactoryMethod<T> method) {
            this.id = id;
            this.method = method;
        }

        public String getId() {
            return id;
        }

        public T create() {
            return method.create();
        }
    }
}
