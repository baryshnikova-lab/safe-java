package edu.princeton.safe.internal.cytoscape;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

public class StyleFactory {
    VisualStyleFactory visualStyleFactory;
    VisualMappingFunctionFactory continuousMappingFactory;

    public StyleFactory(VisualStyleFactory visualStyleFactory,
                        VisualMappingFunctionFactory continuousMappingFactory) {
        this.visualStyleFactory = visualStyleFactory;
        this.continuousMappingFactory = continuousMappingFactory;
    }

    VisualStyle createAttributeBrowserStyle() {
        VisualStyle style = visualStyleFactory.createVisualStyle("SAFE Attribute Browser");

        style.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);
        style.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 30D);
        style.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);

        ContinuousMapping<Double, Paint> fillFunction = (ContinuousMapping<Double, Paint>) continuousMappingFactory.createVisualMappingFunction("SAFE Highlight",
                                                                                                                                                Double.class,
                                                                                                                                                BasicVisualLexicon.NODE_FILL_COLOR);
        fillFunction.addPoint(0D, new BoundaryRangeValues<>(Color.BLACK, Color.BLACK, Color.WHITE));
        fillFunction.addPoint(1D, new BoundaryRangeValues<>(Color.BLACK, Color.WHITE, Color.WHITE));
        style.addVisualMappingFunction(fillFunction);
        return style;
    }
}
