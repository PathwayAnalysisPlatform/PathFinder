package no.uib.pap.pathfinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import no.uib.pap.pathfinder.io.path.PathFile;
import no.uib.pap.pathfinder.io.path.SeedPathFile;
import no.uib.pap.pathfinder.model.graph.Graph;
import no.uib.pap.pathfinder.model.graph.Path;
import no.uib.pap.pathfinder.model.graph.Vertex;

/**
 * This class navigates the graph in all directions and stores the shortest
 * paths. A file is used as back-end to store the paths.
 *
 * @author Marc Vaudel
 */
public class ShortestPath {

    /**
     * The graph to compute the matrix from.
     */
    private final Graph graph;
    /**
     * Map of the seeds that have been explored.
     */
    private final HashMap<Integer, SeedPathFile> finishedSeeds;
    /**
     * The path of the file where paths should be stored.
     */
    private final File resultFile;
    /**
     * The folder where to store intermediate files.
     */
    public final File tempFolder;
    /**
     * The number of vertices in the graph.
     */
    private final int nVertices;
    /**
     * Basic progress counter.
     */
    private int progress = 0;
    /**
     * Boolean indicating whether the process crashed.
     */
    private boolean crashed = false;
    /**
     * The maximal path length.
     */
    public static final int maxDepth = 12;

    /**
     * Constructor.
     *
     * @param graph The graph to compute the matrix from.
     * @param pathFile The file where to save the paths.
     */
    public ShortestPath(Graph graph, File pathFile) {

        this.graph = graph;

        nVertices = graph.vertices.length;

        resultFile = pathFile;

        tempFolder = new File(resultFile.getParent(), "temp");

        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }

        finishedSeeds = new HashMap<>(nVertices);

    }

    /**
     * Computes the path matrix.
     *
     * @param nThreads the number of threads to use
     */
    public void computeMatrix(int nThreads) {

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        for (int origin = 0; origin < nVertices; origin++) {

            Seed singlePath = new Seed(origin);
            pool.submit(singlePath);

        }

        pool.shutdown();

        try {
            if (!pool.awaitTermination(nVertices, TimeUnit.DAYS)) {

                throw new TimeoutException("Shortest path computation timed out.");

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!crashed) {
            wrap();
        }
    }

    /**
     * Builds the final file from the temp file.
     */
    private void wrap() {

        PathFile pathFile = new PathFile(resultFile, nVertices);

        for (int j = 1; j < nVertices; j++) {

            for (int i = 0; i < j; i++) {

                SeedPathFile seedPathFile = finishedSeeds.get(i);

                Path path = seedPathFile.getPath(j);

                if (path == null) {

                    throw new IllegalArgumentException("Missing path between " + i + " and " + j + ".");

                }

                if (path.length() >= maxDepth-2) {

                    throw new IllegalArgumentException("Long path between " + i + " and " + j + ", consider extending the maximal length.");

                }

                pathFile.setPath(path);

            }
        }

        finishedSeeds.values().forEach(seedPathFile -> seedPathFile.close());

        pathFile.close();

        System.gc();

        try {

            wait(100);

        } catch (Exception e) {
            // Ignore
        }

        finishedSeeds.values().forEach(seedPathFile -> {

            File tempFile = seedPathFile.file;
            boolean success = tempFile.delete();

            if (!success) {

                System.out.println("Failed to delete " + tempFile + ".");
                tempFile.deleteOnExit();

            }
        });

        boolean success = tempFolder.delete();

        if (!success) {

            System.out.println("Failed to delete " + tempFolder + ".");
            tempFolder.deleteOnExit();

        }
    }

    /**
     * Convenience class finding the shortest paths to all vertices reachable
     * from a given vertex
     */
    private class Seed implements Runnable {

        /**
         * The mapped file where to save the paths for this seed.
         */
        private final SeedPathFile seedPathFile;

        /**
         * Constructor.
         *
         * @param origin The index of the origin vertex
         */
        public Seed(int origin) {

            File seedFilePath = new File(tempFolder, Integer.toString(origin));

            seedPathFile = new SeedPathFile(seedFilePath, nVertices, maxDepth, origin);

        }

        @Override
        public void run() {

            if (crashed) {
                return;
            }

            try {

                System.out.print(seedPathFile.origin + " ");

                computeShortestPaths();

                if (crashed) {
                    return;
                }

                finishedSeeds.put(seedPathFile.origin, seedPathFile);

                int tempProgress = (int) (1000.0 * ((double) seedPathFile.origin) / nVertices);
                if (tempProgress > progress) {
                    progress = tempProgress;
                    double tempProgressDouble = (tempProgress) / 10.0;
                    System.out.println(tempProgressDouble + "%");
                }

            } catch (Throwable e) {

                System.out.println(seedPathFile.origin + " Crashed.");

                e.printStackTrace();

                crashed = true;

                throw new RuntimeException(e);
            }
        }

        /**
         * Computes all shortest paths from the origin index.
         */
        public void computeShortestPaths() {

            Vertex originVertex = graph.vertices[seedPathFile.origin];

            ArrayList<Path> pathsToExpand = new ArrayList<>();

            for (int i = 0; i < originVertex.neighbors.length && !crashed; i++) {

                int neighbor = originVertex.neighbors[i];
                double weight = originVertex.weights[i];

                boolean found = seedPathFile.hasPath(neighbor);
                double currentWeight = seedPathFile.getWeight(neighbor);
                int currentLength = seedPathFile.getLength(neighbor);

                if (!found
                        || currentWeight > weight
                        || currentWeight == weight && currentLength > 2) {

                    int[] pathIndexes = new int[2];
                    pathIndexes[0] = seedPathFile.origin;
                    pathIndexes[1] = neighbor;
                    Path startPath = new Path(pathIndexes, weight);

                    seedPathFile.setPath(startPath);

                    pathsToExpand.add(startPath);

                }
            }

            while (!pathsToExpand.isEmpty()) {

                pathsToExpand = pathsToExpand.stream()
                        .flatMap(vertex -> expand(vertex).stream())
                        .collect(Collectors.toCollection(ArrayList::new));

            }

        }

        /**
         * Expands the given path to all next vertices.
         *
         * @param lastIndex the vertex from which to expand
         *
         * @return the paths left to expand
         */
        private ArrayList<Path> expand(Path path) {

            int lastIndex = path.getEnd();

            SeedPathFile otherSeedPathFile = finishedSeeds.get(lastIndex);

            if (otherSeedPathFile != null) {

                for (int j = 0; j < nVertices; j++) {

                    if (!path.contains(j)) {

                        Path pathExtension = otherSeedPathFile.getPath(j);

                        if (pathExtension != null) {

                            int totalLength = path.length() + pathExtension.length();

                            if (totalLength <= maxDepth) {

                                double totalWeight = pathExtension.getWeight() + path.getWeight();

                                boolean found = seedPathFile.hasPath(j);
                                double currentWeight = seedPathFile.getWeight(j);
                                int currentLength = seedPathFile.getLength(j);

                                if (!found
                                        || currentWeight > totalWeight
                                        || currentWeight == totalWeight && currentLength > totalLength) {

                                    int[] newIndexes = Arrays.copyOf(path.getPath(), totalLength);
                                    System.arraycopy(pathExtension.getPath(), 0, newIndexes, path.length(), pathExtension.length());

                                    Path newPath = new Path(newIndexes, totalWeight);

                                    seedPathFile.setPath(newPath);

                                }
                            }
                        }
                    }
                }

                return new ArrayList<>(0);

            } else {

                Vertex lastVertex = graph.vertices[lastIndex];
                int nVertices = lastVertex.neighbors.length;

                ArrayList<Path> pathsToExpand = new ArrayList<>(nVertices);

                for (int i = 0; i < nVertices && !crashed; i++) {

                    int neighbor = lastVertex.neighbors[i];

                    if (!path.contains(neighbor)) {

                        int totalLength = path.length() + 1;

                        if (totalLength <= maxDepth) {

                            double weight = lastVertex.weights[i];
                            double totalWeight = weight + path.getWeight();

                            boolean found = seedPathFile.hasPath(neighbor);
                            double currentWeight = seedPathFile.getWeight(neighbor);
                            int currentLength = seedPathFile.getLength(neighbor);

                            if (!found
                                    || currentWeight > totalWeight
                                    || currentWeight == totalWeight && currentLength > totalLength) {

                                int[] newIndexes = Arrays.copyOf(path.getPath(), totalLength);
                                newIndexes[path.length()] = neighbor;

                                Path newPath = new Path(newIndexes, totalWeight);

                                seedPathFile.setPath(newPath);

                                if (totalLength < maxDepth) {

                                    pathsToExpand.add(newPath);

                                }
                            }
                        }
                    }
                }

                return pathsToExpand;

            }
        }
    }
}
