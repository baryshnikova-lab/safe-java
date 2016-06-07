package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyTable;

import edu.princeton.safe.Identifiable;
import edu.princeton.safe.internal.cytoscape.SafeController.Factory;

public class SafeUtil {

    public static void checkSafeColumns(CyTable table) {
        CyColumn column = table.getColumn(StyleFactory.HIGHLIGHT_COLUMN);
        if (column != null) {
            return;
        }
        table.createColumn(StyleFactory.HIGHLIGHT_COLUMN, Double.class, false, 0D);
    }

    public static void addSection(JPanel panel,
                                  String title) {
        addSection(panel, title, "center");
    }

    public static void addSubsection(JPanel panel,
                                     String title) {
        addSection(panel, title, "leading");
    }

    public static void addSection(JPanel panel,
                                  String title,
                                  String alignment) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont()
                           .deriveFont(Font.BOLD));

        panel.add(label, "alignx " + alignment + ", span 2, wrap");
    }

    public static <T> void setSelected(JComboBox<NameValuePair<T>> comboBox,
                                       T value) {

        if (value == null) {
            int defaultIndex = comboBox.getItemCount() > 0 ? 0 : -1;
            comboBox.setSelectedIndex(defaultIndex);
            return;
        }

        OptionalInt index = IntStream.range(0, comboBox.getItemCount())
                                     .filter(i -> comboBox.getItemAt(i)
                                                          .getValue()
                                                          .equals(value))
                                     .findFirst();
        if (index.isPresent()) {
            comboBox.setSelectedIndex(index.getAsInt());
        }
    }

    public static <T> void setSelected(JComboBox<NameValuePair<Factory<T>>> comboBox,
                                       Identifiable value) {

        if (value == null) {
            int defaultIndex = comboBox.getItemCount() > 0 ? 0 : -1;
            comboBox.setSelectedIndex(defaultIndex);
            return;
        }

        OptionalInt index = IntStream.range(0, comboBox.getItemCount())
                                     .filter(i -> comboBox.getItemAt(i)
                                                          .getValue()
                                                          .getId()
                                                          .equals(value.getId()))
                                     .findFirst();
        if (index.isPresent()) {
            comboBox.setSelectedIndex(index.getAsInt());
        }
    }

    public static void updateLayout(Component component) {
        component.invalidate();

        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JFrame) {
                JFrame frame = (JFrame) parent;
                frame.validate();
                frame.repaint();
            }
            parent = parent.getParent();
        }
    }

}
