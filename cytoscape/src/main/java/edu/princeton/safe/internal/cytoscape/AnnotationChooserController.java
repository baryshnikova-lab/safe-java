package edu.princeton.safe.internal.cytoscape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;

import edu.princeton.safe.internal.IdAnalyzer;
import edu.princeton.safe.internal.IdMappingResult;
import edu.princeton.safe.internal.cytoscape.UiUtil.FileSelectionMode;

public class AnnotationChooserController {

    final CySwingApplication application;

    JTextField annotationPath;
    JButton chooseButton;
    JLabel statusLabel;
    String lastAnnotationPath;

    SafeSession session;

    JComboBox<String> nodeIds;
    DefaultComboBoxModel<String> nodeIdsModel;

    ObjectIntMap<String> columnCoverage;
    IdMappingResult idMappingResult;

    public AnnotationChooserController(CySwingApplication application) {
        this.application = application;
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
                        handleAnnotationFileSelected();
                    }
                } catch (IOException e) {
                    fail(e, "Unexpected error while reading annotation file");
                }
            }
        });
        return button;
    }

    void handleAnnotationFileSelected() {
        String path = annotationPath.getText();
        File file = new File(path);
        if (!file.isFile()) {
            return;
        }

        try {
            String canonicalPath = file.getCanonicalPath();
            if (canonicalPath.equals(lastAnnotationPath)) {
                return;
            }

            checkIdCoverage(path);
            lastAnnotationPath = canonicalPath;
        } catch (IOException e) {
            fail(e, "Unexpected error");
        }
    }

    void checkIdCoverage(String path) {
        CyNetworkView view = session.getNetworkView();
        CyNetwork network = view.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();

        List<String> names = getColumnNameStream(nodeTable).collect(Collectors.toList());
        try {
            idMappingResult = IdAnalyzer.analyzeAnnotations(path, new CyNodeTableVisitor(nodeTable, names));
            IntIntMap coverage = idMappingResult.coverage;
            int topIndex = getTopHit(coverage);
            nodeIds.setSelectedIndex(topIndex);

            columnCoverage = new ObjectIntHashMap<>();
            coverage.forEach((Consumer<? super IntIntCursor>) c -> columnCoverage.addTo(names.get(c.key), c.value));

            updateCoverage();
        } catch (IOException e) {
            fail(e, "Unexpected error");
        }
    }

    int getTopHit(IntIntMap coverage) {
        int[] topIndex = { -1 };
        int[] topCount = { 0 };
        coverage.forEach((Consumer<? super IntIntCursor>) c -> {
            if (c.value > topCount[0]) {
                topCount[0] = c.value;
                topIndex[0] = c.key;
            }
        });
        return topIndex[0];
    }

    void updateCoverage() {
        if (columnCoverage == null) {
            statusLabel.setText("");
            return;
        }

        String name = (String) nodeIds.getSelectedItem();
        int nodeHits = columnCoverage.getOrDefault(name, 0);

        double networkCoverage = (double) nodeHits / idMappingResult.totalNetworkNodes * 100;
        double annotationCoverage = (double) nodeHits / idMappingResult.totalAnnotationNodes * 100;

        statusLabel.setText(String.format("<html><div>Network coverage: %.0f%%</div><div>Annotation coverage: %.0f%%</div></html>",
                                          networkCoverage, annotationCoverage));
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
        this.session = session;

        nodeIdsModel.removeAllElements();

        if (session == null) {
            return;
        }

        CyNetworkView view = session.getNetworkView();
        CyNetwork model = view.getModel();
        CyTable table = model.getDefaultNodeTable();
        getColumnNameStream(table).forEach(name -> {
            nodeIdsModel.addElement(name);
        });

        nodeIds.setSelectedItem(session.getIdColumn());
    }

    Stream<String> getColumnNameStream(CyTable table) {
        return table.getColumns()
                    .stream()
                    .filter(c -> c.getType()
                                  .equals(String.class))
                    .map(c -> c.getName())
                    .sorted(String.CASE_INSENSITIVE_ORDER);
    }

    void fail(Throwable t,
              String string) {
        t.printStackTrace();
    }
}
