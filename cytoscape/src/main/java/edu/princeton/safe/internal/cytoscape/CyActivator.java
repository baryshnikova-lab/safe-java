package edu.princeton.safe.internal.cytoscape;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.events.ColumnCreatedListener;
import org.cytoscape.model.events.ColumnDeletedListener;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
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
        VisualMappingManager visualMappingManager = getService(context, VisualMappingManager.class);
        VisualStyleFactory visualStyleFactory = getService(context, VisualStyleFactory.class);

        VisualMappingFunctionFactory continuousMappingFactory = getService(context, VisualMappingFunctionFactory.class,
                                                                           "(mapping.type=continuous)");

        VisualMappingFunctionFactory passthroughMappingFactory = getService(context, VisualMappingFunctionFactory.class,
                                                                            "(mapping.type=passthrough)");

        StyleFactory styleFactory = new StyleFactory(visualStyleFactory, continuousMappingFactory,
                                                     passthroughMappingFactory);

        AttributeBrowserController attributeBrowser = new AttributeBrowserController(visualMappingManager,
                                                                                     styleFactory);

        ImportPanelController importPanel = new ImportPanelController(application, taskManager, attributeBrowser);

        CompositeMapController compositeMapPanel = new CompositeMapController(taskManager, visualMappingManager,
                                                                              styleFactory);

        SafeController safeController = new SafeController(registrar, application, applicationManager, importPanel,
                                                           attributeBrowser, compositeMapPanel);
        importPanel.setSafeController(safeController);
        compositeMapPanel.setSafeController(safeController);

        Map<String, String> safeActionProperties = new MapBuilder().put("inMenuBar", "true")
                                                                   .put("preferredMenu", ServiceProperties.APPS_MENU)
                                                                   .build();
        SafeAction safeAction = new SafeAction(safeActionProperties, applicationManager, networkViewManager,
                                               safeController);
        safeAction.putValue(CyAction.NAME, "SAFE");

        registerService(context, safeAction, CyAction.class);
        registerService(context, safeController, SetCurrentNetworkViewListener.class,
                        NetworkViewAboutToBeDestroyedListener.class, ColumnCreatedListener.class,
                        ColumnDeletedListener.class, ColumnNameChangedListener.class, SessionLoadedListener.class);
    }

    void registerService(BundleContext context,
                         Object service,
                         Class<?>... interfaces) {
        for (Class<?> type : interfaces) {
            registerService(context, service, type, new Properties());
        }
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
