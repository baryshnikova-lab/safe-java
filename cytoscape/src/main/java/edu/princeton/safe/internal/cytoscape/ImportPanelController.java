package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.LongIntMap;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.distance.EdgeWeightedDistanceMetric;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.UnweightedDistanceMetric;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.internal.cytoscape.UiUtil.FileSelectionMode;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;
import net.miginfocom.swing.MigLayout;

public class ImportPanelController {

    CySwingApplication application;
    DialogTaskManager taskManager;

    AttributeBrowserController attributeBrowser;
    ImportTaskConsumer consumer;
    SafeController safeController;

    SafeSession session;

    Component panel;

    JButton step1Button;

    JComboBox<String> nodeNames;
    DefaultComboBoxModel<String> nodeNamesModel;

    JComboBox<String> nodeIds;
    DefaultComboBoxModel<String> nodeIdsModel;

    JTextField annotationPath;

    JComboBox<NameValuePair<Factory<DistanceMetric>>> distanceMetrics;
    JFormattedTextField distanceThreshold;

    JComboBox<NameValuePair<BackgroundMethod>> backgroundMethods;

    JCheckBox forceUndirectedEdges;

    public ImportPanelController(CySwingApplication application,
                                 DialogTaskManager taskManager,
                                 AttributeBrowserController attributeBrowser) {

        this.application = application;
        this.taskManager = taskManager;
        this.attributeBrowser = attributeBrowser;

        consumer = new ImportTaskConsumer() {
            @Override
            public void accept(EnrichmentLandscape landscape) {
                safeController.setEnrichmentLandscape(landscape);
                safeController.setCompositeMap(null);
            }

            @Override
            public void accept(LongIntMap nodeMappings) {
                session.setNodeMappings(nodeMappings);
            }
        };
    }

    public void setSafeController(SafeController safeController) {
        this.safeController = safeController;
        safeController.addConsumer(new SafeResultConsumer() {
            @Override
            public void acceptEnrichmentLandscape(EnrichmentLandscape landscape) {
                session.setEnrichmentLandscape(landscape);
                setEnrichmentLandscape(landscape);
            }

            @Override
            public void acceptCompositeMap(CompositeMap map) {
            }
        });
    }

    void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        attributeBrowser.updateEnrichmentLandscape();
    }

    void setSession(SafeSession session) {
        this.session = session;

        step1Button.setEnabled(session != null);

        if (session == null) {
            return;
        }

        File annotationFile = session.getAnnotationFile();
        if (annotationFile == null) {
            annotationPath.setText("");
        } else {
            annotationPath.setText(annotationFile.getPath());
        }

        SafeUtil.setSelected(distanceMetrics, session.getDistanceMetric());
        SafeUtil.setSelected(backgroundMethods, session.getBackgroundMethod());

        distanceThreshold.setValue(session.getDistanceThreshold());

        forceUndirectedEdges.setSelected(session.getForceUndirectedEdges());

        setEnrichmentLandscape(session.getEnrichmentLandscape());
    }

    void updateColumnList() {
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
             .forEach(name -> {
                 nodeNamesModel.addElement(name);
                 nodeIdsModel.addElement(name);
             });

        nodeNames.setSelectedItem(session.getNameColumn());
        nodeIds.setSelectedItem(session.getIdColumn());
    }

    Component getPanel() {
        if (panel == null) {
            panel = createPanel();
        }
        return panel;
    }

    @SuppressWarnings("unchecked")
    JComponent createPanel() {
        JPanel panel = UiUtil.createJPanel();
        panel.setLayout(new MigLayout("fillx, insets 0", "[grow 0, right]rel[left]"));

        nodeNamesModel = new DefaultComboBoxModel<>();
        nodeIdsModel = new DefaultComboBoxModel<>();

        nodeNames = new JComboBox<>(nodeNamesModel);
        nodeIds = new JComboBox<>(nodeIdsModel);

        panel.add(new JLabel("Node names"));
        panel.add(nodeNames, "wrap");

        panel.add(new JLabel("Annotation ids"));
        panel.add(nodeIds, "wrap");

        JButton chooseAnnotationFileButton = createChooseButton();
        annotationPath = new JTextField();

        panel.add(new JLabel("Annotation file"));
        panel.add(annotationPath, "growx, wmax 200, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        distanceMetrics = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Map-based",
                                                                                    new Factory<>("map",
                                                                                                  () -> new MapBasedDistanceMetric())),
                                                                new NameValuePair<>("Edge-weighted",
                                                                                    new Factory<>("edge",
                                                                                                  () -> new EdgeWeightedDistanceMetric())),
                                                                new NameValuePair<>("Unweighted",
                                                                                    new Factory<>("unweighted",
                                                                                                  () -> new UnweightedDistanceMetric())) });

        distanceThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        backgroundMethods = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("All nodes in network",
                                                                                      BackgroundMethod.Network),
                                                                  new NameValuePair<>("All nodes in annotation standard",
                                                                                      BackgroundMethod.Annotation) });

        forceUndirectedEdges = new JCheckBox("Assume edges are undirected");

        panel.add(new JLabel("Distance metric"));
        panel.add(distanceMetrics, "wrap");

        panel.add(new JLabel("Max. distance threshold"));
        panel.add(distanceThreshold, "growx, wmax 200, wrap");

        panel.add(new JLabel("Background"));
        panel.add(backgroundMethods, "wrap");

        panel.add(forceUndirectedEdges, "skip 1, wrap");

        step1Button = createStep1Button();
        panel.add(step1Button, "span 2, tag apply, wrap");

        return panel;
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

                NameValuePair<Factory<DistanceMetric>> distancePair = (NameValuePair<Factory<DistanceMetric>>) distanceMetrics.getSelectedItem();
                session.setDistanceMetric(distancePair.getValue()
                                                      .create());

                session.setDistanceThreshold(getDistanceThreshold());

                NameValuePair<BackgroundMethod> backgroundPair = (NameValuePair<BackgroundMethod>) backgroundMethods.getSelectedItem();
                session.setBackgroundMethod(backgroundPair.getValue());

                attributeBrowser.applyToSession();

                ImportTaskFactory factory = new ImportTaskFactory(session, consumer);
                taskManager.execute(factory.createTaskIterator());
            }
        });
        return button;
    }

    double getDistanceThreshold() {
        Number value = (Number) distanceThreshold.getValue();
        if (value == null) {
            return 0;
        }
        return value.doubleValue();
    }

    void fail(IOException e,
              String string) {
        // TODO Auto-generated method stub
        e.printStackTrace();
    }

}
