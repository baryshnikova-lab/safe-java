package edu.princeton.safe.internal.cytoscape;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

    @Override
    public void start(BundleContext context) throws Exception {

        CyApplicationManager applicationManager = getService(context, CyApplicationManager.class);
        CyNetworkViewManager networkViewManager = getService(context, CyNetworkViewManager.class);
        CyServiceRegistrar registrar = getService(context, CyServiceRegistrar.class);
        CySwingApplication application = getService(context, CySwingApplication.class);
        DialogTaskManager taskManager = getService(context, DialogTaskManager.class);

        SafeController importController = new SafeController(registrar, application, taskManager, applicationManager);

        Map<String, String> safeActionProperties = new MapBuilder().put("inMenuBar", "true")
                                                                   .put("preferredMenu", ServiceProperties.APPS_MENU)
                                                                   .build();
        SafeAction safeAction = new SafeAction(safeActionProperties, applicationManager, networkViewManager,
                                               importController);
        safeAction.putValue(CyAction.NAME, "SAFE");

        registerService(context, safeAction, CyAction.class, new Properties());
    }

    static class PropertiesBuilder {
        Properties properties;

        public PropertiesBuilder() {
            properties = new Properties();
        }

        public PropertiesBuilder put(String key,
                                     Object value) {
            properties.put(key, value);
            return this;
        }

        public Properties build() {
            return properties;
        }
    }

    static class MapBuilder {
        Map<String, String> map;

        public MapBuilder() {
            map = new HashMap<>();
        }

        public MapBuilder put(String key,
                              String value) {
            map.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return map;
        }
    }
}
