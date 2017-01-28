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
    public static final String HIGHLIGHT_COLUMN = "SAFE Enrichment Score";
    public static final String COLOR_COLUMN = "SAFE Color";
    public static final String BRIGHTNESSS_COLUMN = "SAFE Max Enrichment Score";

    public static final String ATTRIBUTE_BROWSER_STYLE = "SAFE Attribute Browser";
    public static final String DOMAIN_BROWSER_STYLE = "SAFE Domain Browser";

    public static final Color NEGATIVE = new Color(0x2c, 0x7b, 0xb6);
    public static final Color ZERO = new Color(51, 51, 51);
    public static final Color POSITIVE = new Color(0xd7, 0x19, 0x1c);

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

    public VisualStyle createAttributeBrowserStyle() {

        VisualStyle style = visualStyleFactory.createVisualStyle(ATTRIBUTE_BROWSER_STYLE);

        style.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);

        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 80D);
        style.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        style.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, ZERO);

        style.setDefaultValue(BasicVisualLexicon.EDGE_VISIBLE, false);

        ContinuousMapping<Double, Paint> fillFunction = (ContinuousMapping<Double, Paint>) continuousMappingFactory.createVisualMappingFunction(HIGHLIGHT_COLUMN,
                                                                                                                                                Double.class,
                                                                                                                                                BasicVisualLexicon.NODE_FILL_COLOR);

        fillFunction.addPoint(-1D, new BoundaryRangeValues<>(NEGATIVE, NEGATIVE, NEGATIVE));
        fillFunction.addPoint(0D, new BoundaryRangeValues<>(ZERO, ZERO, ZERO));
        fillFunction.addPoint(1D, new BoundaryRangeValues<>(POSITIVE, POSITIVE, POSITIVE));
        style.addVisualMappingFunction(fillFunction);

        VisualMappingFunction<Double, Double> zLocationFunction = passthroughMappingFactory.createVisualMappingFunction(StyleFactory.BRIGHTNESSS_COLUMN,
                                                                                                                        Double.class,
                                                                                                                        BasicVisualLexicon.NODE_Z_LOCATION);
        style.addVisualMappingFunction(zLocationFunction);

        return style;
    }

    public VisualStyle createDomainBrowserStyle() {

        Color noDomain = new Color(51, 51, 51);

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
