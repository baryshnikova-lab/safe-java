package edu.princeton.safe.internal.cytoscape;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class ExpanderController {

    boolean isEnabled;
    boolean isExpanded;

    List<ExpandListener> expandListeners;
    List<EnableListener> enableListeners;

    JLabel expander;

    public ExpanderController() {
        isEnabled = true;
        isExpanded = true;

        expandListeners = new ArrayList<>();
        enableListeners = new ArrayList<>();
    }

    public void addExpandListener(ExpandListener listener) {
        expandListeners.add(listener);
    }

    public void addEnableListener(EnableListener listener) {
        enableListeners.add(listener);
    }

    public JComponent getExpander() {
        if (expander != null) {
            return expander;
        }

        expander = SafeUtil.createIconLabel("");
        expander.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                if (!isEnabled) {
                    return;
                }
                toggle();
            }
        });

        updateState();
        return expander;
    }

    public void toggle() {
        isExpanded = !isExpanded;
        updateState();
    }

    void updateState() {
        expander.setText(isExpanded ? SafeUtil.CARET_DOWN_ICON : SafeUtil.CARET_LEFT_ICON);
        expandListeners.stream()
                       .forEach(l -> l.set(isExpanded));
    }

    public void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
        updateState();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        enableListeners.stream()
                       .forEach(l -> l.set(isEnabled));
    }

    @FunctionalInterface
    public static interface ExpandListener {
        void set(boolean isExpanded);
    }

    @FunctionalInterface
    public static interface EnableListener {
        void set(boolean isEnabled);
    }
}
