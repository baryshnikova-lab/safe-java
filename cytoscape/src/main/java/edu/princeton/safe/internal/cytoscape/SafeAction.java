package edu.princeton.safe.internal.cytoscape;

import java.awt.event.ActionEvent;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.view.model.CyNetworkViewManager;

import edu.princeton.safe.internal.cytoscape.controller.SafeController;

public class SafeAction extends AbstractCyAction {

    private static final long serialVersionUID = 1L;

    private SafeController importController;

    public SafeAction(Map<String, String> properties,
                      CyApplicationManager applicationManager,
                      CyNetworkViewManager networkViewManager,
                      SafeController importController) {
        super(properties, applicationManager, networkViewManager);
        this.importController = importController;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        importController.showPanel();
    }

}
