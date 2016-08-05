package edu.princeton.safe.internal.cytoscape.io;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.session.CySession;
import org.cytoscape.view.model.CyNetworkView;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import edu.princeton.safe.AnalysisMethod;
import edu.princeton.safe.DistanceMetric;
import edu.princeton.safe.GroupingMethod;
import edu.princeton.safe.RestrictionMethod;
import edu.princeton.safe.distance.EdgeWeightedDistanceMetric;
import edu.princeton.safe.distance.MapBasedDistanceMetric;
import edu.princeton.safe.distance.UnweightedDistanceMetric;
import edu.princeton.safe.grouping.ClusterBasedGroupingMethod;
import edu.princeton.safe.grouping.DistanceMethod;
import edu.princeton.safe.grouping.JaccardDistanceMethod;
import edu.princeton.safe.grouping.NullGroupingMethod;
import edu.princeton.safe.internal.BackgroundMethod;
import edu.princeton.safe.internal.cytoscape.model.SafeSession;
import edu.princeton.safe.model.EnrichmentLandscape;
import edu.princeton.safe.restriction.HartiganRestrictionMethod;
import edu.princeton.safe.restriction.RadiusBasedRestrictionMethod;
import edu.princeton.safe.restriction.SubtractiveRestrictionMethod;

public class SafeSessionSerializer {

    static final String DATA_TABLE_TITLE = "SAFE Data";

    static final String DATA_TABLE_COLUMN = "SAFE Data Table";
    static final String SAFE_DATA_COLUMN = "Data";

    CyTableManager tableManager;
    CyTableFactory tableFactory;

    public SafeSessionSerializer(CyTableManager tableManager,
                                 CyTableFactory tableFactory) {
        this.tableManager = tableManager;
        this.tableFactory = tableFactory;
    }

    public void saveToSession(SafeSession[] sessions) {
        deleteExistingTables();

        CyTable dataTable = getSafeDataTable();
        checkColumn(dataTable, SAFE_DATA_COLUMN, String.class);

        Arrays.stream(sessions)
              .forEach(session -> {
                  try {
                      serializeSession(dataTable, session);
                  } catch (Throwable t) {
                      t.printStackTrace();
                  }
              });

        tableManager.addTable(dataTable);
    }

    private void deleteExistingTables() {

        Set<CyTable> tables = tableManager.getAllTables(true);
        Set<String> titles = new HashSet<>();
        titles.add(DATA_TABLE_TITLE);

        tables.stream()
              .filter(table -> titles.contains(table.getTitle()))
              .forEach(table -> {
                  tableManager.deleteTable(table.getSUID());
              });
    }

    void serializeSession(CyTable dataTable,
                          SafeSession session) {
        CyNetworkView networkView = session.getNetworkView();
        CyNetwork network = networkView.getModel();
        CyTable networkTable = network.getDefaultNetworkTable();
        CyRow row = networkTable.getRow(network.getSUID());

        checkColumn(networkTable, DATA_TABLE_COLUMN, Long.class);
        Long suid = dataTable.getSUID();
        row.set(DATA_TABLE_COLUMN, suid);

        CyRow viewRow = dataTable.getRow(networkView.getSUID());
        try (StringWriter writer = new StringWriter()) {
            toJson(session, writer);
            String json = writer.toString();
            viewRow.set(SAFE_DATA_COLUMN, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkColumn(CyTable table,
                             String name,
                             Class<?> type) {
        CyColumn column = table.getColumn(name);
        if (column == null) {
            table.createColumn(name, type, false);
        }
    }

    private CyTable getSafeDataTable() {
        CyTable table = tableFactory.createTable(DATA_TABLE_TITLE, CyIdentifiable.SUID, Long.class, true, true);
        table.setSavePolicy(SavePolicy.SESSION_FILE);
        return table;
    }

    void toJson(SafeSession session,
                Writer writer)
            throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(writer)) {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            generator.setPrettyPrinter(printer);

            generator.writeStartObject();

            AnalysisMethod analysisMethod = session.getAnalysisMethod();
            if (analysisMethod != null) {
                generator.writeStringField("analysisMethod", analysisMethod.name());
            }

            generator.writeNumberField("analysisType", session.getAnalysisType());

            File annotationFile = session.getAnnotationFile();
            if (annotationFile != null) {
                generator.writeStringField("annotationFile", annotationFile.getPath());
            }

            BackgroundMethod backgroundMethod = session.getBackgroundMethod();
            if (backgroundMethod != null) {
                generator.writeStringField("backgroundMethod", backgroundMethod.name());
            }

            DistanceMetric distanceMetric = session.getDistanceMetric();
            if (distanceMetric != null) {
                generator.writeStringField("distanceMetric", distanceMetric.getId());
            }

            generator.writeNumberField("distanceThreshold", session.getDistanceThreshold());
            generator.writeBooleanField("forceUndirectedEdges", session.getForceUndirectedEdges());

            GroupingMethod groupingMethod = session.getGroupingMethod();
            if (groupingMethod != null) {
                generator.writeStringField("groupingMethod", groupingMethod.getId());
            }

            generator.writeStringField("idColumn", session.getIdColumn());
            generator.writeStringField("nameColumn", session.getNameColumn());
            generator.writeStringField("weightColumn", session.getWeightColumn());
            generator.writeBooleanField("isDistanceThresholdAbsolute", session.isDistanceThresholdAbsolute());
            generator.writeNumberField("minimumLandscapeSize", session.getMinimumLandscapeSize());
            generator.writeNumberField("quantitativeIterations", session.getQuantitativeIterations());

            RestrictionMethod restrictionMethod = session.getRestrictionMethod();
            if (restrictionMethod != null) {
                generator.writeStringField("restrictionMethod", restrictionMethod.getId());
            }

            generator.writeNumberField("similarityThreshold", session.getSimilarityThreshold());
            generator.writeNumberField("colorSeed", session.getColorSeed());
            generator.writeBooleanField("randomizeColors", session.getRandomizeColors());

            generator.writeEndObject();
        }

    }

    public LongObjectMap<SafeSession> loadFromSession(CySession cySession) throws IOException {

        CyTable dataTable = cySession.getTables()
                                     .stream()
                                     .map(metadata -> metadata.getTable())
                                     .filter(table -> DATA_TABLE_TITLE.equals(table.getTitle()))
                                     .findFirst()
                                     .orElse(null);

        LongObjectMap<SafeSession> sessions = new LongObjectHashMap<>();
        if (dataTable == null) {
            return sessions;
        }

        for (CyRow row : dataTable.getAllRows()) {
            String json = row.get(SAFE_DATA_COLUMN, String.class);
            SafeSession session = parseSession(json);

            Long oldSuid = row.get(CyIdentifiable.SUID, Long.class);
            CyNetworkView view = cySession.getObject(oldSuid, CyNetworkView.class);
            session.setNetworkView(view);
            sessions.put(view.getSUID(), session);
        }
        return sessions;
    }

    private SafeSession parseSession(String json) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(json)) {
            expect(JsonToken.START_OBJECT, parser.nextToken());

            SafeSession session = new SafeSession();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String name = parser.getCurrentName();
                if ("analysisMethod".equals(name)) {
                    session.setAnalysisMethod(AnalysisMethod.valueOf(parser.nextTextValue()));
                } else if ("analysisType".equals(name)) {
                    session.setAnalysisType(parser.nextIntValue(0));
                } else if ("distanceThreshold".equals(name)) {
                    parser.nextToken();
                    session.setDistanceThreshold(parser.getDoubleValue());
                } else if ("forceUndirectedEdges".equals(name)) {
                    session.setForceUndirectedEdges(parser.nextBooleanValue());
                } else if ("idColumn".equals(name)) {
                    session.setIdColumn(parser.nextTextValue());
                } else if ("nameColumn".equals(name)) {
                    session.setNameColumn(parser.nextTextValue());
                } else if ("weightColumn".equals(name)) {
                    session.setWeightColumn(parser.nextTextValue());
                } else if ("isDistanceThresholdAbsolute".equals(name)) {
                    session.setDistanceThresholdAbsolute(parser.nextBooleanValue());
                } else if ("minimumLandscapeSize".equals(name)) {
                    session.setMinimumLandscapeSize(parser.nextIntValue(0));
                } else if ("quantitativeIterations".equals(name)) {
                    session.setQuantitativeIterations(parser.nextIntValue(0));
                } else if ("similarityThreshold".equals(name)) {
                    parser.nextToken();
                    session.setSimilarityThreshold(parser.getDoubleValue());

                } else if ("annotationFile".equals(name)) {
                    File file = new File(parser.nextTextValue());
                    if (!file.isFile()) {
                        file = new File(file.getName());
                    }
                    session.setAnnotationFile(file);
                } else if ("backgroundMethod".equals(name)) {
                    session.setBackgroundMethod(BackgroundMethod.valueOf(parser.nextTextValue()));
                } else if ("distanceMetric".equals(name)) {
                    session.setDistanceMetric(getDistanceMetric(parser.nextTextValue()));
                } else if ("groupingMethod".equals(name)) {
                    session.setGroupingMethod(getGroupingMethod(parser.nextTextValue()));
                } else if ("restrictionMethod".equals(name)) {
                    session.setRestrictionMethod(getRestrictionMethod(parser.nextTextValue()));
                } else if ("colorSeed".equals(name)) {
                    session.setColorSeed(parser.nextIntValue(0));
                } else if ("randomizeColors".equals(name)) {
                    session.setRandomizeColors(parser.nextBooleanValue());
                }
            }

            return session;
        }
    }

    private RestrictionMethod getRestrictionMethod(String id) {
        if (RadiusBasedRestrictionMethod.ID.equals(id)) {
            return new RadiusBasedRestrictionMethod(0, 0);
        }
        if (HartiganRestrictionMethod.ID.equals(id)) {
            return new HartiganRestrictionMethod();
        }
        if (SubtractiveRestrictionMethod.ID.equals(id)) {
            return new SubtractiveRestrictionMethod();
        }
        return null;
    }

    private GroupingMethod getGroupingMethod(String id) {
        if (JaccardDistanceMethod.ID.equals(id)) {
            return new ClusterBasedGroupingMethod(0, new JaccardDistanceMethod(d -> true));
        }
        if (DistanceMethod.CORRELATION_ID.equals(id)) {
            return new ClusterBasedGroupingMethod(0, DistanceMethod.CORRELATION);
        }

        return NullGroupingMethod.instance;
    }

    private DistanceMetric getDistanceMetric(String id) {
        if (MapBasedDistanceMetric.ID.equals(id)) {
            return new MapBasedDistanceMetric();
        }
        if (EdgeWeightedDistanceMetric.ID.equals(id)) {
            return new EdgeWeightedDistanceMetric();
        }
        if (UnweightedDistanceMetric.ID.equals(id)) {
            return new UnweightedDistanceMetric();
        }
        return null;
    }

    private void expect(JsonToken expected,
                        JsonToken actual)
            throws IOException {
        if (expected != actual) {
            throw new IOException(String.format("Expected: %s; Got: %s", expected, actual));
        }
    }

    public static void main(String[] args) throws Exception {
        SafeSession session = new SafeSession();
        session.setAnalysisMethod(AnalysisMethod.HighestAndLowest);
        session.setAnalysisType(EnrichmentLandscape.TYPE_LOWEST);
        session.setDistanceThreshold(5.0);
        session.setForceUndirectedEdges(true);
        session.setIdColumn("ID");
        session.setNameColumn("name");
        session.setWeightColumn("weight");
        session.setDistanceThresholdAbsolute(true);
        session.setMinimumLandscapeSize(10);
        session.setQuantitativeIterations(50);
        session.setSimilarityThreshold(0.5);

        session.setAnnotationFile(new File("test.txt"));
        session.setBackgroundMethod(BackgroundMethod.Network);
        session.setDistanceMetric(new MapBasedDistanceMetric());
        session.setGroupingMethod(new ClusterBasedGroupingMethod(0, new JaccardDistanceMethod(d -> true)));
        session.setRestrictionMethod(new RadiusBasedRestrictionMethod(0, 0));

        SafeSessionSerializer serializer = new SafeSessionSerializer(null, null);
        StringWriter writer = new StringWriter();

        serializer.toJson(session, writer);
        String json = writer.toString();
        System.out.println(json);

        SafeSession session2 = serializer.parseSession(json);
        writer = new StringWriter();
        serializer.toJson(session2, writer);
        String json2 = writer.toString();
        System.out.println(json2);
        if (!json.equals(json2)) {
            System.out.println("Error");
        }
    }
}
