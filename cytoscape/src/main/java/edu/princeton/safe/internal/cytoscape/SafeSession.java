package edu.princeton.safe.internal.cytoscape;

import java.io.File;

import org.cytoscape.view.model.CyNetworkView;

public class SafeSession {

    CyNetworkView networkView;
    String nameColumn;
    String idColumn;
    File annotationFile;

    public CyNetworkView getNetworkView() {
        return networkView;
    }

    public void setNetworkView(CyNetworkView view) {
        networkView = view;
    }

    public String getNameColumn() {
        return nameColumn;
    }

    public void setNameColumn(String name) {
        nameColumn = name;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String name) {
        idColumn = name;
    }

    public void setAnnotationFile(File file) {
        annotationFile = file;
    }

    public File getAnnotationFile() {
        return annotationFile;
    }
}
