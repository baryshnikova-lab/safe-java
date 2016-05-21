package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;

import javax.swing.Icon;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;

public class SafeCytoPanelComponent implements CytoPanelComponent2 {

    private Component component;

    public SafeCytoPanelComponent(Component component) {
        this.component = component;
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public String getTitle() {
        return "SAFE";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "org.princeton.safe.Safe";
    }

}
