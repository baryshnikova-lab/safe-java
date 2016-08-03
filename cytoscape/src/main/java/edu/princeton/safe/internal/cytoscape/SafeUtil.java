package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

import edu.princeton.safe.Identifiable;
import edu.princeton.safe.internal.cytoscape.controller.ExpanderController;
import edu.princeton.safe.internal.cytoscape.controller.ExpansionChangeListener;
import edu.princeton.safe.internal.cytoscape.model.Factory;
import edu.princeton.safe.internal.cytoscape.model.NameValuePair;

public class SafeUtil {

    public static final String SEARCH_ICON = "\uf002";
    public static final String CARET_DOWN_ICON = "\uf0d7";
    public static final String CARET_LEFT_ICON = "\uf0da";

    static Font iconFont;

    public static void checkSafeColumns(CyTable table) {
        CyColumn column = table.getColumn(StyleFactory.HIGHLIGHT_COLUMN);
        if (column == null) {
            table.createColumn(StyleFactory.HIGHLIGHT_COLUMN, Double.class, false, 0D);
        }

        column = table.getColumn(StyleFactory.COLOR_COLUMN);
        if (column == null) {
            table.createColumn(StyleFactory.COLOR_COLUMN, String.class, false, null);
        }

        column = table.getColumn(StyleFactory.BRIGHTNESSS_COLUMN);
        if (column == null) {
            table.createColumn(StyleFactory.BRIGHTNESSS_COLUMN, Double.class, false, 0D);
        }
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
        Font boldFont = label.getFont()
                             .deriveFont(Font.BOLD);
        label.setFont(boldFont);

        panel.add(label, "alignx " + alignment + ", wrap");
    }

    public static ExpanderController addExpandingSection(JPanel parent,
                                                         String title,
                                                         Component section,
                                                         ExpansionChangeListener expansionListener,
                                                         String layoutOptions) {

        ExpanderController controller = new ExpanderController();
        JComponent expander = controller.getExpander();

        JLabel label = new JLabel(title);

        Font boldFont = label.getFont()
                             .deriveFont(Font.BOLD);
        label.setFont(boldFont);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                if (!controller.isEnabled()) {
                    return;
                }
                controller.toggle();
            }
        });

        controller.addExpandListener(isExpanded -> {
            section.setVisible(isExpanded);
            if (expansionListener != null) {
                expansionListener.expansionChanged(isExpanded);
            }
        });

        controller.addEnableListener(isEnabled -> {
            label.setEnabled(isEnabled);
            expander.setEnabled(isEnabled);
        });

        parent.add(expander, "grow 0, split 2");
        parent.add(label, "wrap");
        parent.add(section, layoutOptions);

        return controller;
    }

    public static JLabel createIconLabel(String icon) {
        int size = 15;

        JLabel label = new JLabel(icon);
        label.setFont(getIconFont(size));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(size, size));
        return label;
    }

    public static Font getIconFont(float size) {
        if (iconFont == null) {
            InputStream stream = SafeUtil.class.getResourceAsStream("/fonts/fontawesome-webfont.ttf");
            try {
                iconFont = Font.createFont(Font.TRUETYPE_FONT, stream);
                GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                environment.registerFont(iconFont);
            } catch (FontFormatException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return iconFont.deriveFont(size);
    }

    public static void addSeparator(JPanel panel) {
        panel.add(new JSeparator(), "span, growx, hmin 10, wrap");
    }

    public static <T> void setSelected(JComboBox<NameValuePair<T>> comboBox,
                                       T value) {

        if (value == null) {
            int defaultIndex = comboBox.getItemCount() > 0 ? 0 : -1;
            comboBox.setSelectedIndex(defaultIndex);
            return;
        }

        OptionalInt index = IntStream.range(0, comboBox.getItemCount())
                                     .filter(i -> isEqual(comboBox.getItemAt(i)
                                                                  .getValue(),
                                                          value))
                                     .findFirst();
        if (index.isPresent()) {
            comboBox.setSelectedIndex(index.getAsInt());
        }
    }

    static boolean isEqual(Object o1,
                           Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        return o1.equals(o2);
    }

    public static <T> void setSelected(JComboBox<NameValuePair<Factory<T>>> comboBox,
                                       Identifiable value) {

        String id = value == null ? null : value.getId();
        OptionalInt index = IntStream.range(0, comboBox.getItemCount())
                                     .filter(i -> isEqual(comboBox.getItemAt(i)
                                                                  .getValue()
                                                                  .getId(),
                                                          id))
                                     .findFirst();
        if (index.isPresent()) {
            comboBox.setSelectedIndex(index.getAsInt());
        } else {
            int defaultIndex = comboBox.getItemCount() > 0 ? 0 : -1;
            comboBox.setSelectedIndex(defaultIndex);
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

    public static void clearSelection(CyTable table) {
        table.getMatchingRows(CyNetwork.SELECTED, true)
             .stream()
             .forEach(row -> row.set(CyNetwork.SELECTED, false));
    }

    public static Stream<String> getStringColumnNames(CyTable table) {
        return table.getColumns()
                    .stream()
                    .filter(c -> c.getType()
                                  .equals(String.class))
                    .map(c -> c.getName())
                    .sorted(String.CASE_INSENSITIVE_ORDER);
    }

}
