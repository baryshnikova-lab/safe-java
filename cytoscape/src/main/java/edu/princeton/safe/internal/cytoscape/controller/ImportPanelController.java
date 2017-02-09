package edu.princeton.safe.internal.cytoscape.controller;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.LongIntMap;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.FactoryMethod;
import edu.princeton.safe.distance.EdgeWeightedDistanceMetric;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.UnweightedDistanceMetric;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.UiUtil;
import edu.princeton.safe.internal.cytoscape.event.EventService;
import edu.princeton.safe.internal.cytoscape.model.Factory;
import edu.princeton.safe.internal.cytoscape.model.NameValuePair;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.cytoscape.task.ImportTask;
import edu.princeton.safe.internal.cytoscape.task.ImportTaskConsumer;
import edu.princeton.safe.internal.cytoscape.task.SimpleTaskFactory;
import edu.princeton.safe.internal.io.TabDelimitedAnnotationParser;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;
import net.miginfocom.swing.MigLayout;

public class ImportPanelController {

    final DialogTaskManager taskManager;

    final EventService eventService;
    final AttributeBrowserController attributeBrowser;
    final AnnotationChooserController annotationChooser;
    final ImportTaskConsumer consumer;

    SafeSession session;

    Component panel;

    JButton step1Button;

    JComboBox<NameValuePair<Factory<DistanceMetric>>> distanceMetrics;
    JFormattedTextField distanceThreshold;

    JComboBox<NameValuePair<BackgroundMethod>> backgroundMethods;

    JCheckBox forceUndirectedEdges;

    JLabel weightColumnLabel;
    JComboBox<String> weightColumn;
    DefaultComboBoxModel<String> weightColumnModel;
    JComboBox<NameValuePair<Boolean>> thresholdMethod;

    public ImportPanelController(DialogTaskManager taskManager,
                                 AttributeBrowserController attributeBrowser,
                                 AnnotationChooserController annotationChooser,
                                 EventService eventService) {

        this.taskManager = taskManager;
        this.attributeBrowser = attributeBrowser;
        this.annotationChooser = annotationChooser;
        this.eventService = eventService;

        eventService.addEnrichmentLandscapeListener(landscape -> {
            setEnrichmentLandscape(landscape);
        });

        consumer = new ImportTaskConsumer() {
            @Override
            public void accept(EnrichmentLandscape landscape) {
                session.setAnalysisMethod(null);
                session.setCompositeMap(null);
                eventService.notifyListeners((CompositeMap) null);

                session.setEnrichmentLandscape(landscape);
                eventService.notifyListeners(landscape);
            }

            @Override
            public void accept(LongIntMap nodeMappings) {
                session.setNodeMappings(nodeMappings);
            }
        };

        annotationChooser.addListener(file -> handleAnnotationFileSelected(file));
    }

    private void handleAnnotationFileSelected(File file) {
        if (step1Button == null) {
            return;
        }

        if (file == null) {
            step1Button.setEnabled(false);
            return;
        }

        step1Button.setEnabled(file.isFile());
    }

    void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        attributeBrowser.updateEnrichmentLandscape();
    }

    void setSession(SafeSession session) {
        this.session = session;

        step1Button.setEnabled(false);

        if (session == null) {
            return;
        }

        JTextField annotationPath = annotationChooser.getTextField();
        File annotationFile = session.getAnnotationFile();
        if (annotationFile == null) {
            annotationPath.setText("");
        } else {
            annotationPath.setText(annotationFile.getPath());
        }
        handleAnnotationFileSelected(annotationFile);

        JComboBox<String> nodeIdComboBox = annotationChooser.getNodeIdComboBox();
        nodeIdComboBox.setSelectedItem(session.getIdColumn());

        SafeUtil.setSelected(distanceMetrics, session.getDistanceMetric());
        weightColumn.setSelectedItem(session.getWeightColumn());
        SafeUtil.setSelected(backgroundMethods, session.getBackgroundMethod());

        distanceThreshold.setValue(session.getDistanceThreshold());
        thresholdMethod.setSelectedItem(session.isDistanceThresholdAbsolute());

        forceUndirectedEdges.setSelected(session.getForceUndirectedEdges());

        setEnrichmentLandscape(session.getEnrichmentLandscape());
    }

    void updateColumnList() {
        annotationChooser.updateColumnList(session);
        updateEdgeWeightColumnList();
    }

    private void updateEdgeWeightColumnList() {
        weightColumnModel.removeAllElements();

        if (session == null) {
            return;
        }

        CyNetworkView view = session.getNetworkView();
        CyNetwork model = view.getModel();
        CyTable table = model.getDefaultEdgeTable();
        table.getColumns()
             .stream()
             .filter(c -> !CyIdentifiable.SUID.equals(c.getName()) && Number.class.isAssignableFrom(c.getType()))
             .map(c -> c.getName())
             .sorted(String.CASE_INSENSITIVE_ORDER)
             .forEach(name -> weightColumnModel.addElement(name));

        weightColumn.setSelectedItem(session.getWeightColumn());

        if (weightColumn.getSelectedIndex() == -1 && weightColumnModel.getSize() > 0) {
            weightColumn.setSelectedIndex(0);
        }
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
        panel.setLayout(new MigLayout("fillx, insets 0, hidemode 3", "[grow 0, right]rel[left]"));

        JEditorPane examplesLabel = UiUtil.createLinkEnabledEditorPane(panel,
                                                                       "<html><a href=\"https://github.com/baryshnikova-lab/safe-java/wiki\">Examples</a>");
        panel.add(examplesLabel, "grow 0, skip 1, wrap");

        JButton chooseAnnotationFileButton = annotationChooser.getChooseButton();
        JTextField annotationPath = annotationChooser.getTextField();

        panel.add(new JLabel("Attribute file"));
        panel.add(annotationPath, "growx, wmax 200, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        panel.add(new JLabel("Node column"));
        panel.add(annotationChooser.getNodeIdComboBox(), "wrap");

        panel.add(annotationChooser.getStatusLabel(), "grow 0, skip 1, wrap");

        distanceMetrics = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Map-weighted",
                                                                                    new Factory<>("map",
                                                                                                  () -> new MapBasedDistanceMetric())),
                                                                new NameValuePair<>("Edge-weighted",
                                                                                    new Factory<>("edge",
                                                                                                  () -> new EdgeWeightedDistanceMetric())),
                                                                new NameValuePair<>("Unweighted",
                                                                                    new Factory<>("unweighted",
                                                                                                  () -> new UnweightedDistanceMetric())) });

        distanceMetrics.addActionListener(event -> validateState());

        weightColumnLabel = new JLabel("Weight column");
        weightColumnModel = new DefaultComboBoxModel<>();
        weightColumn = new JComboBox<>(weightColumnModel);

        distanceThreshold = new JFormattedTextField(NumberFormat.getNumberInstance());

        thresholdMethod = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("Percentile", Boolean.FALSE),
                                                                new NameValuePair<>("Absolute", Boolean.TRUE) });

        backgroundMethods = new JComboBox<>(new NameValuePair[] { new NameValuePair<>("All nodes in the network",
                                                                                      BackgroundMethod.Network),
                                                                  new NameValuePair<>("All nodes in the attribute file",
                                                                                      BackgroundMethod.Annotation) });

        forceUndirectedEdges = new JCheckBox("Assume edges are undirected");

        panel.add(forceUndirectedEdges, "newline unrel, skip 1, wrap");

        panel.add(new JLabel("Distance"));
        panel.add(distanceMetrics, "wrap");

        panel.add(weightColumnLabel);
        panel.add(weightColumn, "wrap");

        panel.add(new JLabel("Threshold"));
        panel.add(distanceThreshold, "split 2, growx, wmax 50");
        panel.add(thresholdMethod, "wrap");

        panel.add(new JLabel("Background"), "newline unrel");
        panel.add(backgroundMethods, "wrap");

        step1Button = createStep1Button();
        panel.add(step1Button, "newline unrel, skip 1, wrap");

        validateState();

        return panel;
    }

    @SuppressWarnings("unchecked")
    private void validateState() {
        NameValuePair<Factory<DistanceMetric>> metric = (NameValuePair<Factory<DistanceMetric>>) distanceMetrics.getSelectedItem();
        boolean showWeightColumn = metric != null && EdgeWeightedDistanceMetric.ID.equals(metric.getValue()
                                                                                                .getId());
        weightColumnLabel.setVisible(showWeightColumn);
        weightColumn.setVisible(showWeightColumn);
    }

    JButton createStep1Button() {
        JButton button = new JButton("Build");
        button.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox<String> nodeIds = annotationChooser.getNodeIdComboBox();
                session.setIdColumn((String) nodeIds.getSelectedItem());

                JTextField annotationPath = annotationChooser.getTextField();
                session.setAnnotationFile(new File(annotationPath.getText()));

                NameValuePair<Factory<DistanceMetric>> distancePair = (NameValuePair<Factory<DistanceMetric>>) distanceMetrics.getSelectedItem();
                session.setDistanceMetric(distancePair.getValue()
                                                      .create());

                session.setWeightColumn((String) weightColumn.getSelectedItem());

                session.setDistanceThreshold(getDistanceThreshold());

                NameValuePair<Boolean> isAbsolutePair = (NameValuePair<Boolean>) thresholdMethod.getSelectedItem();
                session.setDistanceThresholdAbsolute(isAbsolutePair.getValue());

                NameValuePair<BackgroundMethod> backgroundPair = (NameValuePair<BackgroundMethod>) backgroundMethods.getSelectedItem();
                session.setBackgroundMethod(backgroundPair.getValue());

                FactoryMethod<TabDelimitedAnnotationParser> parserFactory = annotationChooser.getParserFactory();
                TaskFactory factory = new SimpleTaskFactory(() -> new ImportTask(session, consumer, parserFactory));
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
        e.printStackTrace();
    }

}
