package edu.princeton.safe.internal.cytoscape;

import javax.swing.JPanel;

public class UiUtil {
    public static JPanel createJPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }
}
