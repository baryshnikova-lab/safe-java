package edu.princeton.safe.internal.cytoscape;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

public class StyleFactory {
    public static final String HIGHLIGHT_COLUMN = "SAFE Highlight";
    public static final String COLOR_COLUMN = "SAFE Color";
    public static final String BRIGHTNESSS_COLUMN = "SAFE Brightness";

    public static final String ATTRIBUTE_BROWSER_STYLE = "SAFE Attribute Browser";
    public static final String DOMAIN_BROWSER_STYLE = "SAFE Domain Browser";

    VisualStyleFactory visualStyleFactory;
    VisualMappingFunctionFactory continuousMappingFactory;
    VisualMappingFunctionFactory passthroughMappingFactory;

    public StyleFactory(VisualStyleFactory visualStyleFactory,
                        VisualMappingFunctionFactory continuousMappingFactory,
                        VisualMappingFunctionFactory passthroughMappingFactory) {
        this.visualStyleFactory = visualStyleFactory;
        this.continuousMappingFactory = continuousMappingFactory;
        this.passthroughMappingFactory = passthroughMappingFactory;
    }

    VisualStyle createAttributeBrowserStyle() {

        Color negative = new Color(0, 204, 255);
        Color zero = new Color(30, 30, 30);
        Color positive = new Color(255, 204, 0);

        VisualStyle style = visualStyleFactory.createVisualStyle(ATTRIBUTE_BROWSER_STYLE);

        style.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 80D);
        style.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        style.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, zero);

        style.setDefaultValue(BasicVisualLexicon.EDGE_VISIBLE, false);

        ContinuousMapping<Double, Paint> fillFunction = (ContinuousMapping<Double, Paint>) continuousMappingFactory.createVisualMappingFunction(HIGHLIGHT_COLUMN,
                                                                                                                                                Double.class,
                                                                                                                                                BasicVisualLexicon.NODE_FILL_COLOR);

        fillFunction.addPoint(-1D, new BoundaryRangeValues<>(negative, negative, negative));
        fillFunction.addPoint(0D, new BoundaryRangeValues<>(zero, zero, zero));
        fillFunction.addPoint(1D, new BoundaryRangeValues<>(positive, positive, positive));
        style.addVisualMappingFunction(fillFunction);

        VisualMappingFunction<Double, Double> zLocationFunction = passthroughMappingFactory.createVisualMappingFunction(StyleFactory.BRIGHTNESSS_COLUMN,
                                                                                                                        Double.class,
                                                                                                                        BasicVisualLexicon.NODE_Z_LOCATION);
        style.addVisualMappingFunction(zLocationFunction);

        return style;
    }

    VisualStyle createDomainBrowserStyle() {

        Color noDomain = new Color(30, 30, 30);

        VisualStyle style = visualStyleFactory.createVisualStyle(DOMAIN_BROWSER_STYLE);

        style.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 80D);
        style.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        style.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, noDomain);

        style.setDefaultValue(BasicVisualLexicon.EDGE_VISIBLE, false);

        VisualMappingFunction<String, Paint> fillFunction = passthroughMappingFactory.createVisualMappingFunction(COLOR_COLUMN,
                                                                                                                  String.class,
                                                                                                                  BasicVisualLexicon.NODE_FILL_COLOR);
        style.addVisualMappingFunction(fillFunction);

        VisualMappingFunction<Double, Double> zLocationFunction = passthroughMappingFactory.createVisualMappingFunction(StyleFactory.BRIGHTNESSS_COLUMN,
                                                                                                                        Double.class,
                                                                                                                        BasicVisualLexicon.NODE_Z_LOCATION);
        style.addVisualMappingFunction(zLocationFunction);

        return style;
    }
}
