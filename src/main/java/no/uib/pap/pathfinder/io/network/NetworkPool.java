package no.uib.pap.pathfinder.io.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import no.uib.pap.pathfinder.model.graph.Graph;
import no.uib.pap.pathfinder.model.graph.Vertex;
import static no.uib.pap.pathfinder.util.Utils.encoding;

/**
 * This class provides preset graphs.
 *
 * @author Marc Vaudel
 */
public class NetworkPool {

    /**
     * The file where the Reactome network is stored.
     */
    public static final String reactomeFile = "resources/networks/reactome/reactome_main_component.gz";
    /**
     * The file where the Reactome paths are stored.
     */
    public static final String reactomePathFile = "resources/paths/reactome";

    /**
     * Returns a simple test graph.
     *
     * @return a simple test graph
     */
    public static Graph getTestGraph() {

        int[] edges1 = {1, 3, 4};
        double[] weights1 = {0.6989700, 0.7781513, 0.7781513};
        Vertex vertex1 = new Vertex("1", edges1, weights1);

        int[] edges2 = {0, 3, 4};
        double[] weights2 = {0.6989700, 0.7781513, 0.7781513};
        Vertex vertex2 = new Vertex("2", edges2, weights2);

        int[] edges3 = {3, 4};
        double[] weights3 = {0.6989700, 0.6989700};
        Vertex vertex3 = new Vertex("3", edges3, weights3);

        int[] edges4 = {0, 1, 2, 4};
        double[] weights4 = {0.7781513, 0.7781513, 0.6989700, 0.8450980};
        Vertex vertex4 = new Vertex("4", edges4, weights4);

        int[] edges5 = {0, 1, 2, 3};
        double[] weights5 = {0.7781513, 0.7781513, 0.6989700, 0.8450980};
        Vertex vertex5 = new Vertex("5", edges5, weights5);

        Vertex[] vertices = {vertex1, vertex2, vertex3, vertex4, vertex5};

        return new Graph(vertices);

    }

    /**
     * Returns a simple directed test graph.
     *
     * @return a simple directed test graph
     */
    public static Graph getTestGraphDirected() {

        int[] edges1 = {1, 3, 4};
        double[] weights1 = {0.6989700, 0.7781513, 0.7781513};
        Vertex vertex1 = new Vertex("1", edges1, weights1);

        int[] edges2 = {3, 4};
        double[] weights2 = {0.7781513, 0.7781513};
        Vertex vertex2 = new Vertex("2", edges2, weights2);

        int[] edges3 = {3, 4};
        double[] weights3 = {0.6989700, 0.6989700};
        Vertex vertex3 = new Vertex("3", edges3, weights3);

        int[] edges4 = {4};
        double[] weights4 = {0.8450980};
        Vertex vertex4 = new Vertex("4", edges4, weights4);

        int[] edges5 = {};
        double[] weights5 = {};
        Vertex vertex5 = new Vertex("5", edges5, weights5);

        Vertex[] vertices = {vertex1, vertex2, vertex3, vertex4, vertex5};

        return new Graph(vertices);

    }

    /**
     * Returns the Reactome graph.
     *
     * @return the Reactome graph
     */
    public static Graph getReactomeGraph() {

        return getGraphFromDataFrame(new File(reactomeFile), false);

    }

    /**
     * Returns the a graph from an iGraph data frame.
     *
     * @param file the file to import
     * @param directed boolean indicating whether the graph is directed
     *
     * @return the graph as parsed from the file
     */
    private static Graph getGraphFromDataFrame(File file, boolean directed) {

        try {

            InputStream fileStream = new FileInputStream(file);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, encoding);

            try (BufferedReader br = new BufferedReader(decoder)) {

                HashMap<String, Integer> verticesNames = new HashMap<>();
                HashMap<String, HashMap<String, Double>> edgesMap = new HashMap<>();
                int vertexCount = 0;

                String line = br.readLine();
                while ((line = br.readLine()) != null) {

                    String[] lineSplit = line.split(" ");
                    String from = lineSplit[0];
                    String to = lineSplit[1];
                    double weight = Double.parseDouble(lineSplit[2]);

                    Integer fromI = verticesNames.get(from);

                    if (fromI == null) {

                        fromI = vertexCount++;
                        verticesNames.put(from, fromI);

                    }

                    Integer toI = verticesNames.get(to);

                    if (toI == null) {

                        toI = vertexCount++;
                        verticesNames.put(to, toI);

                    }

                    HashMap<String, Double> fromEdges = edgesMap.get(from);

                    if (fromEdges == null) {

                        fromEdges = new HashMap<>(1);
                        edgesMap.put(from, fromEdges);

                    }

                    fromEdges.put(to, weight);

                    if (!directed) {

                        HashMap<String, Double> toEdges = edgesMap.get(to);

                        if (toEdges == null) {

                            toEdges = new HashMap<>(1);
                            edgesMap.put(to, toEdges);

                        }

                        toEdges.put(from, weight);

                    }
                }

                TreeMap<Integer, TreeSet<String>> degreeMap = new TreeMap();

                for (Entry<String, HashMap<String, Double>> entry : edgesMap.entrySet()) {

                    String vertex = entry.getKey();
                    int degree = entry.getValue().size();

                    TreeSet<String> degreeVertices = degreeMap.get(degree);

                    if (degreeVertices == null) {

                        degreeVertices = new TreeSet<>();
                        degreeMap.put(degree, degreeVertices);

                    }

                    degreeVertices.add(vertex);

                }

                int[] indexes = new int[verticesNames.size()];
                int index = 0;

                for (TreeSet<String> verticesAtDegree : degreeMap.descendingMap().values()) {

                    for (String vertexName : verticesAtDegree) {

                        indexes[verticesNames.get(vertexName)] = index++;

                    }
                }

                final Vertex[] vertices = new Vertex[verticesNames.size()];
                index = 0;

                for (TreeSet<String> verticesAtDegree : degreeMap.descendingMap().values()) {

                    for (String vertexName : verticesAtDegree) {

                        TreeMap<String, Double> vertexEdges = new TreeMap(edgesMap.get(vertexName));

                        int[] edges = new int[vertexEdges.size()];
                        double[] weights = new double[vertexEdges.size()];

                        int i = 0;

                        for (Entry<String, Double> entry2 : vertexEdges.entrySet()) {

                            String vertex2 = entry2.getKey();
                            int originalIndex = verticesNames.get(vertex2);
                            edges[i] = indexes[originalIndex];
                            weights[i] = entry2.getValue();
                            i++;

                        }

                        Vertex vertex = new Vertex(vertexName, edges, weights);
                        vertices[index++] = vertex;

                    }
                }

                return new Graph(vertices);

            }
        } catch (Exception e) {

            throw new RuntimeException(e);
            
        }
    }
}
