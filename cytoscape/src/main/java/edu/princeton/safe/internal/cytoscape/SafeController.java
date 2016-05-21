package edu.princeton.safe.internal.cytoscape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;

import net.miginfocom.swing.MigLayout;

public class SafeController implements SetCurrentNetworkViewListener, NetworkViewAboutToBeDestroyedListener,
        ColumnCreatedListener, ColumnDeletedListener, ColumnNameChangedListener {

    CyServiceRegistrar registrar;
    CySwingApplication application;
    DialogTaskManager taskManager;
    CyApplicationManager applicationManager;

    CytoPanelComponent2 cytoPanelComponent;

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

    public SafeController(CyServiceRegistrar registrar,
                          CySwingApplication application,
                          DialogTaskManager taskManager,
                          CyApplicationManager applicationManager) {
        this.registrar = registrar;
        this.application = application;
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;

        sessionsBySuid = new LongObjectHashMap<>();

        JPanel panel = createPanel();
        cytoPanelComponent = new SafeCytoPanelComponent(panel);
        setNetworkView(applicationManager.getCurrentNetworkView());
    }

    @Override
    public void handleEvent(SetCurrentNetworkViewEvent event) {
        CyNetworkView view = event.getNetworkView();
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
                 .sorted()
                 .forEach(new Consumer<String>() {
                     @Override
                     public void accept(String name) {
                         nodeNamesModel.addElement(name);
                         nodeIdsModel.addElement(name);
                     }
                 });
        }
    }

    JPanel createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx", "[right]rel[left, grow, fill]"));

        nodeNamesModel = new DefaultComboBoxModel<>();
        nodeIdsModel = new DefaultComboBoxModel<>();

        nodeNames = new JComboBox<>(nodeNamesModel);
        nodeIds = new JComboBox<>(nodeIdsModel);

        panel.add(new JLabel("Node Names"));
        panel.add(nodeNames, "wrap");

        panel.add(new JLabel("Annotation IDs"));
        panel.add(nodeIds, "wrap");

        JButton chooseAnnotationFileButton = new JButton("Choose");
        chooseAnnotationFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showOpenDialog(application.getJFrame());
                if (result == JFileChooser.APPROVE_OPTION) {
                    annotationPath.setText(chooser.getSelectedFile()
                                                  .getPath());
                }
            }
        });

        annotationPath = new JTextField();

        panel.add(new JLabel("Annotation File"));
        panel.add(annotationPath, "growx, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        step1Button = new JButton("Build");
        step1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                session.setNameColumn((String) nodeNames.getSelectedItem());
                session.setIdColumn((String) nodeIds.getSelectedItem());
                session.setAnnotationFile(new File(annotationPath.getText()));

                SafeTaskFactory factory = new SafeTaskFactory(session);
                taskManager.execute(factory.createTaskIterator());
            }
        });

        panel.add(step1Button, "span 2, tag apply");
        return panel;
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
}
