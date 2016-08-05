package edu.princeton.safe.internal.cytoscape.model;

import java.io.File;
import java.util.function.Consumer;

import org.cytoscape.view.model.CyNetworkView;

import com.carrotsearch.hppc.LongIntMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;

import edu.princeton.safe.AnalysisMethod;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.model.CompositeMap;
import edu.princeton.safe.model.EnrichmentLandscape;

public class SafeSession {

    CyNetworkView networkView;
    String nameColumn;
    String idColumn;
    String weightColumn;
    File annotationFile;
    AnalysisMethod analysisMethod;
    DistanceMetric distanceMetric;
    double distanceThreshold;
    BackgroundMethod backgroundMethod;
    boolean isDistanceThresholdAbsolute;
    private int quantitativeIterations;
    EnrichmentLandscape enrichmentLandscape;
    boolean forceUndirectedEdges;
    Long[] suidsByNodeIndex;
    int minimumLandscapeSize;
    double similarityThreshold;
    GroupingMethod groupingMethod;
    RestrictionMethod restrictionMethod;
    int analysisType;
    CompositeMap compositeMap;
    int colorSeed;
    boolean randomizeColors;

    public SafeSession() {
        // TODO: Expose as setting
        setQuantitativeIterations(100);
    }

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

    public AnalysisMethod getAnalysisMethod() {
        return analysisMethod;
    }

    public void setAnalysisMethod(AnalysisMethod method) {
        analysisMethod = method;
    }

    public DistanceMetric getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(DistanceMetric metric) {
        distanceMetric = metric;
    }

    public void setDistanceThreshold(double threshold) {
        distanceThreshold = threshold;
    }

    public void setBackgroundMethod(BackgroundMethod method) {
        backgroundMethod = method;
    }

    public boolean isDistanceThresholdAbsolute() {
        return isDistanceThresholdAbsolute;
    }

    public void setDistanceThresholdAbsolute(boolean isAbsolute) {
        isDistanceThresholdAbsolute = isAbsolute;
    }

    public double getDistanceThreshold() {
        return distanceThreshold;
    }

    public BackgroundMethod getBackgroundMethod() {
        return backgroundMethod;
    }

    public int getQuantitativeIterations() {
        return quantitativeIterations;
    }

    public EnrichmentLandscape getEnrichmentLandscape() {
        return enrichmentLandscape;
    }

    public void setEnrichmentLandscape(EnrichmentLandscape landscape) {
        this.enrichmentLandscape = landscape;
    }

    public boolean getForceUndirectedEdges() {
        return forceUndirectedEdges;
    }

    public void setForceUndirectedEdges(boolean force) {
        forceUndirectedEdges = force;
    }

    public void setNodeMappings(LongIntMap nodeMappings) {
        suidsByNodeIndex = new Long[nodeMappings.size()];
        nodeMappings.forEach((Consumer<? super LongIntCursor>) (LongIntCursor c) -> suidsByNodeIndex[c.value] = c.key);
    }

    public Long[] getNodeMappings() {
        return suidsByNodeIndex;
    }

    public int getMinimumLandscapeSize() {
        return minimumLandscapeSize;
    }

    public void setMinimumLandscapeSize(int minimum) {
        minimumLandscapeSize = minimum;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double threshold) {
        similarityThreshold = threshold;
    }

    public GroupingMethod getGroupingMethod() {
        return groupingMethod;
    }

    public void setGroupingMethod(GroupingMethod method) {
        groupingMethod = method;
    }

    public RestrictionMethod getRestrictionMethod() {
        return restrictionMethod;
    }

    public void setRestrictionMethod(RestrictionMethod method) {
        restrictionMethod = method;
    }

    public int getAnalysisType() {
        return analysisType;
    }

    public CompositeMap getCompositeMap() {
        return compositeMap;
    }

    public void setCompositeMap(CompositeMap map) {
        compositeMap = map;
    }

    public String getWeightColumn() {
        return weightColumn;
    }

    public void setWeightColumn(String name) {
        weightColumn = name;
    }

    public void setAnalysisType(int type) {
        analysisType = type;
    }

    public void setQuantitativeIterations(int iterations) {
        quantitativeIterations = iterations;
    }

    public int getColorSeed() {
        return colorSeed;
    }

    public void setColorSeed(int colorSeed) {
        this.colorSeed = colorSeed;
    }

    public boolean getRandomizeColors() {
        return randomizeColors;
    }

    public void setRandomizeColors(boolean randomizeColors) {
        this.randomizeColors = randomizeColors;
    }

}
