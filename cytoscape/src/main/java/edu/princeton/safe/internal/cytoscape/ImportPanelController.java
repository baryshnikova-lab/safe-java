package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.LongIntMap;

import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.distance.EdgeWeightedDistanceMetric;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.UnweightedDistanceMetric;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.internal.cytoscape.event.EventService;
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

        JTextField annotationPath = annotationChooser.getTextField();
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
        annotationChooser.updateColumnList(session);
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

        JButton chooseAnnotationFileButton = annotationChooser.getChooseButton();
        JTextField annotationPath = annotationChooser.getTextField();

        panel.add(new JLabel("Annotation file"));
        panel.add(annotationPath, "growx, wmax 200, split 2");
        panel.add(chooseAnnotationFileButton, "wrap");

        panel.add(new JLabel("Annotation ids"));
        panel.add(annotationChooser.getNodeIdComboBox(), "wrap");

        panel.add(annotationChooser.getStatusLabel(), "grow 0, skip 1, wrap");

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
        panel.add(step1Button, "skip 1, wrap");

        return panel;
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

                session.setDistanceThreshold(getDistanceThreshold());

                NameValuePair<BackgroundMethod> backgroundPair = (NameValuePair<BackgroundMethod>) backgroundMethods.getSelectedItem();
                session.setBackgroundMethod(backgroundPair.getValue());

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
        e.printStackTrace();
    }

}
