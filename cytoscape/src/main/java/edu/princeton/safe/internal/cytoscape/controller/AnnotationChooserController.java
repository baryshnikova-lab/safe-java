package edu.princeton.safe.internal.cytoscape.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.swing.DialogTaskManager;

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;

import edu.princeton.safe.FactoryMethod;
import edu.princeton.safe.internal.IdMappingResult;
import edu.princeton.safe.internal.Util;
import edu.princeton.safe.internal.cytoscape.SafeUtil;
import edu.princeton.safe.internal.cytoscape.UiUtil;
import edu.princeton.safe.internal.cytoscape.UiUtil.FileSelectionMode;
import edu.princeton.safe.internal.cytoscape.io.CyNodeTableVisitor;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.internal.cytoscape.task.AnalyzeAnnotationConsumer;
import edu.princeton.safe.internal.cytoscape.task.AnalyzeAnnotationTask;
import edu.princeton.safe.internal.cytoscape.task.SimpleTaskFactory;
import edu.princeton.safe.internal.io.TabDelimitedAnnotationParser;

public class AnnotationChooserController {

    final CySwingApplication application;
    final DialogTaskManager taskManager;

    JTextField annotationPath;
    JButton chooseButton;
    JLabel statusLabel;
    String lastAnnotationPath;

    SafeSession session;

    JComboBox<String> nodeIds;
    DefaultComboBoxModel<String> nodeIdsModel;

    ObjectIntMap<String> columnCoverage;
    IdMappingResult idMappingResult;

    File lastDirectory;

    List<FileSelectedListener> listeners;

    public AnnotationChooserController(CySwingApplication application,
                                       DialogTaskManager taskManager) {
        this.application = application;
        this.taskManager = taskManager;

        lastDirectory = new File(".");

        listeners = new ArrayList<>();
    }

    JButton createChooseButton() {
        JButton button = new JButton("Choose...");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Set<String> extensions = new HashSet<>();
                try {
                    File file = UiUtil.getFile(application.getJFrame(), "Select Attribute File", lastDirectory,
                                               "Attribute File", extensions, FileSelectionMode.OPEN_FILE);
                    if (file != null) {
                        if (file.isFile()) {
                            lastDirectory = file.getParentFile();
                        }
                        annotationPath.setText(file.getPath());
                        handleAnnotationFileSelected();
                    }
                } catch (IOException e) {
                    fail(e, "Unexpected error while reading attribute file");
                }
            }
        });
        return button;
    }

    void handleAnnotationFileSelected() {
        notifyListeners(null);
        
        String path = annotationPath.getText();
        File file = new File(path);
        if (!file.isFile()) {
            lastAnnotationPath = null;
            return;
        }

        try {
            String canonicalPath = file.getCanonicalPath();
            if (canonicalPath.equals(lastAnnotationPath)) {
                return;
            }

            checkIdCoverage(file);
        } catch (IOException e) {
            fail(e, "Unexpected error");
        }
    }

    void checkIdCoverage(File file) throws IOException {
        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();

        List<String> names = SafeUtil.getStringColumnNames(nodeTable)
                                     .collect(Collectors.toList());
        CyNodeTableVisitor visitor = new CyNodeTableVisitor(nodeTable, names);

        String path = file.getPath();
        AnalyzeAnnotationConsumer consumer = new AnalyzeAnnotationConsumer() {
            @Override
            public void accept(IdMappingResult result) {
                idMappingResult = result;
                IntIntMap coverage = result.coverage;
                int topIndex = Util.getTopKey(coverage, -1);

                columnCoverage = new ObjectIntHashMap<>();
                coverage.forEach((Consumer<? super IntIntCursor>) c -> columnCoverage.addTo(names.get(c.key), c.value));

                nodeIds.setSelectedIndex(topIndex);

                try {
                    String canonicalPath = file.getCanonicalPath();
                    lastAnnotationPath = canonicalPath;
                    notifyListeners(file);
                } catch (IOException e) {
                    fail(e, "Unexpected error");
                }
            }
        };

        SimpleTaskFactory taskFactory = new SimpleTaskFactory(() -> new AnalyzeAnnotationTask(path, visitor, consumer));
        taskManager.execute(taskFactory.createTaskIterator());
    }

    void updateCoverage() {
        if (columnCoverage == null) {
            statusLabel.setVisible(false);
            return;
        }

        String name = (String) nodeIds.getSelectedItem();
        int nodeHits = columnCoverage.getOrDefault(name, 0);

        double networkCoverage = (double) nodeHits / idMappingResult.totalNetworkNodes * 100;
        double annotationCoverage = (double) nodeHits / idMappingResult.totalAnnotationNodes * 100;

        statusLabel.setText(String.format("<html><div>Network coverage: %.0f%%</div><div>Attribute coverage: %.0f%%</div></html>",
                                          networkCoverage, annotationCoverage));
        statusLabel.setVisible(true);
    }

    public JButton getChooseButton() {
        if (chooseButton == null) {
            chooseButton = createChooseButton();
        }

        return chooseButton;
    }

    public JTextField getTextField() {
        if (annotationPath == null) {
            annotationPath = createTextField();
        }

        return annotationPath;
    }

    JTextField createTextField() {
        JTextField field = new JTextField();
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                handleAnnotationFileSelected();
            }
        });

        return field;
    }

    public JLabel getStatusLabel() {
        if (statusLabel == null) {
            statusLabel = new JLabel();
        }

        return statusLabel;
    }

    public JComboBox<String> getNodeIdComboBox() {
        if (nodeIds == null) {
            nodeIdsModel = new DefaultComboBoxModel<>();
            nodeIds = new JComboBox<>(nodeIdsModel);
            nodeIds.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateCoverage();
                }
            });
        }

        return nodeIds;
    }

    void updateColumnList(SafeSession session) {
        lastAnnotationPath = null;
        this.session = session;

        nodeIdsModel.removeAllElements();

        if (session == null) {
            return;
        }

        CyNetworkView view = session.getNetworkView();
        CyNetwork model = view.getModel();
        CyTable table = model.getDefaultNodeTable();
        SafeUtil.getStringColumnNames(table)
                .forEach(name -> {
                    nodeIdsModel.addElement(name);
                });

        nodeIds.setSelectedItem(session.getIdColumn());
    }

    public FactoryMethod<TabDelimitedAnnotationParser> getParserFactory() {
        return () -> {
            int labelLineIndex = idMappingResult.getLabelLineIndex();
            String commentCharacter = idMappingResult.getCommentCharacter();
            return new TabDelimitedAnnotationParser(session.getAnnotationFile()
                                                           .getPath(),
                                                    labelLineIndex, commentCharacter);
        };
    }

    void fail(Throwable t,
              String string) {
        t.printStackTrace();
    }

    public void addListener(FileSelectedListener listener) {
        listeners.add(listener);
    }

    void notifyListeners(File selectedFile) {
        listeners.stream()
                 .forEach(l -> l.selected(selectedFile));
    }

    @FunctionalInterface
    public interface FileSelectedListener {
        void selected(File file);
    }
}
