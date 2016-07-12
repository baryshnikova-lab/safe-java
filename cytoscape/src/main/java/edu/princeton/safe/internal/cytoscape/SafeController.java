package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.internal.cytoscape.event.EventService;
import edu.princeton.safe.internal.cytoscape.event.SetCompositeMapListener;
import edu.princeton.safe.internal.cytoscape.event.SetEnrichmentLandscapeListener;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import net.miginfocom.swing.MigLayout;

public class SafeController implements SetCurrentNetworkViewListener, NetworkViewAboutToBeDestroyedListener,
        ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener, SessionLoadedListener {

    final CyServiceRegistrar registrar;
    final CySwingApplication application;
    final CyApplicationManager applicationManager;

    final EventService eventService;
    final ImportPanelController importPanel;
    final AttributeBrowserController attributeBrowser;
    final CompositeMapController compositeMapPanel;
    final DomainBrowserController domainBrowser;

    CytoPanelComponent2 cytoPanelComponent;

    LongObjectMap<SafeSession> sessionsBySuid;
    SafeSession session;
    final Object sessionMutex = new Object();

    final Object panelMutex = new Object();
    boolean panelVisible;

    Component panel;

    public SafeController(CyServiceRegistrar registrar,
                          CySwingApplication application,
                          CyApplicationManager applicationManager,
                          ImportPanelController importPanel,
                          AttributeBrowserController attributeBrowser,
                          CompositeMapController compositeMapPanel,
                          DomainBrowserController domainBrowser,
                          EventService eventService) {

        this.registrar = registrar;
        this.application = application;
        this.applicationManager = applicationManager;

        this.importPanel = importPanel;
        this.attributeBrowser = attributeBrowser;
        this.compositeMapPanel = compositeMapPanel;
        this.domainBrowser = domainBrowser;
        this.eventService = eventService;

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
        session.setSimilarityThreshold(0.75);
        session.setRestrictionMethod(new RadiusBasedRestrictionMethod(0, 0));
        session.setGroupingMethod(new ClusterBasedGroupingMethod(0, DistanceMethod.JACCARD));
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
            compositeMapPanel.setSession(session);
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

    Component createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, hidemode 3"));

        Component step1Section = importPanel.getPanel();
        ExpanderController step1Controller = SafeUtil.addExpandingSection(panel, "Step 1: Build Enrichment Landscapes",
                                                                          step1Section, "grow, wrap");
        SafeUtil.addSeparator(panel);

        Component step2Section = attributeBrowser.getPanel();
        ExpanderController step2Controller = SafeUtil.addExpandingSection(panel, "Step 2: View Enrichment Landscapes",
                                                                          step2Section,
                                                                          "grow, hmin 100, hmax 200, wrap");
        SafeUtil.addSeparator(panel);

        Component step3Section = compositeMapPanel.getPanel();
        ExpanderController step3Controller = SafeUtil.addExpandingSection(panel, "Step 3: Build Composite Map",
                                                                          step3Section, "grow, wrap");
        SafeUtil.addSeparator(panel);

        Component step4Section = domainBrowser.getPanel();
        ExpanderController step4Controller = SafeUtil.addExpandingSection(panel, "Step 4: View Composite Map",
                                                                          step4Section,
                                                                          "grow, hmin 100, hmax 200, wrap");

        JScrollPane container = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        if (UiUtil.isMacOSX()) {
            container.setOpaque(false);
            container.setBorder(BorderFactory.createEmptyBorder());
            JViewport viewport = container.getViewport();
            viewport.setOpaque(false);
        }

        SetEnrichmentLandscapeListener landscapeListener = landscape -> {
            if (landscape == null) {
                step2Controller.setExpanded(false);
                step2Controller.setEnabled(false);

                step3Controller.setExpanded(false);
                step3Controller.setEnabled(false);
            } else {
                step1Controller.setExpanded(false);

                step2Controller.setExpanded(true);
                step2Controller.setEnabled(true);

                step3Controller.setExpanded(true);
                step3Controller.setEnabled(true);
            }
        };

        SetCompositeMapListener compositeMapListener = compositeMap -> {
            if (compositeMap == null) {
                step4Controller.setEnabled(false);
                step4Controller.setExpanded(false);
            } else {
                step1Controller.setExpanded(false);
                step2Controller.setExpanded(false);
                step3Controller.setExpanded(false);

                step4Controller.setEnabled(true);
                step4Controller.setExpanded(true);
            }
        };

        landscapeListener.set(null);
        compositeMapListener.set(null);

        eventService.addEnrichmentLandscapeListener(landscapeListener);
        eventService.addCompositeMapListener(compositeMapListener);

        return container;
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
}
