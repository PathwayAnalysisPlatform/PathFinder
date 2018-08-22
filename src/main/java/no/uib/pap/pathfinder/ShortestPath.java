package no.uib.pap.pathfinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import no.uib.pap.pathfinder.io.path.PathFile;
import no.uib.pap.pathfinder.io.path.SeedPathFile;
import no.uib.pap.pathfinder.io.path.TempPathFile;
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
     * The file where to save the paths during processing.
     */
    private final File tempFile;
    /**
     * The mapped file where to save the paths during processing.
     */
    private final TempPathFile tempPathFile;
    /**
     * The path of the file where paths are stored.
     */
    private final String filePath;
    /**
     * Set of indexes already processed.
     */
    private final HashSet<Integer> processedIndexes;
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
    public static final int maxDepth = 10;
    /**
     * Semaphore for the different threads to write to the path file.
     */
    private final Semaphore pathFileMutex = new Semaphore(1);

    /**
     * Constructor.
     *
     * @param graph The graph to compute the matrix from.
     * @param pathFile The file where to save the paths.
     */
    public ShortestPath(Graph graph, File pathFile) {

        this.graph = graph;

        nVertices = graph.vertices.length;

        processedIndexes = new HashSet<>(nVertices);
        filePath = pathFile.getAbsolutePath();

        tempFile = new File(filePath + "_temp");
        tempPathFile = new TempPathFile(tempFile, nVertices, maxDepth);

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
     * Stores the seed matrix in the main matrix.
     *
     * @param seedPathFile the seed matrix
     * @param origin the origin vertex of this seed
     */
    private void storeSeedMatrix(SeedPathFile seedPathFile, int origin) {

        try {

            pathFileMutex.acquire();

            for (int destination = 0; destination < nVertices; destination++) {

                if (destination != origin) {

                    Path seedPath = seedPathFile.getPath(destination);

                    if (seedPath != null) {

                        Path referencePath = tempPathFile.getPath(origin, destination);

                        if (referencePath == null
                                || referencePath.getWeight() > seedPath.getWeight()
                                || referencePath.getWeight() == seedPath.getWeight() && referencePath.length() < seedPath.getWeight()) {

                            tempPathFile.setPath(seedPath);

                        }
                    }
                }
            }

            pathFileMutex.release();

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
    }

    /**
     * Builds the final file from the temp file.
     */
    private void wrap() {

        File resultFile = new File(filePath);
        PathFile pathFile = new PathFile(resultFile, nVertices);

        for (int j = 1; j < nVertices; j++) {

            for (int i = 0; i < j; i++) {

                Path path = tempPathFile.getPath(i, j);

                if (path == null) {

                    throw new IllegalArgumentException("Missing path between " + i + " and " + j + ".");

                }

                pathFile.setPath(path);

            }
        }

        tempPathFile.close();
        pathFile.close();

        System.gc();

        try {

            wait(10);

        } catch (Exception e) {
            // Ignore
        }

        boolean success = tempFile.delete();

        if (!success) {

            System.out.println("Failed to delete " + tempFile + ".");
            tempFile.deleteOnExit();

        }

    }

    /**
     * Convenience class finding the shortest paths to all vertices reachable
     * from a given vertex
     */
    private class Seed implements Runnable {

        /**
         * The index of the origin vertex.
         */
        private final int origin;
        /**
         * The mapped file where to save the paths for this seed.
         */
        private final SeedPathFile seedPathFile;
        /**
         * The file where to save the paths for this seed.
         */
        private final File seedFilePath;

        /**
         * Constructor.
         *
         * @param origin The index of the origin vertex
         */
        public Seed(int origin) {

            this.origin = origin;

            seedFilePath = new File(filePath + "_" + origin);

            seedPathFile = new SeedPathFile(seedFilePath, nVertices, maxDepth);

        }

        @Override
        public void run() {

            if (crashed) {
                return;
            }

            try {

                System.out.print(origin + " ");

                computeShortestPaths();

                if (crashed) {
                    return;
                }

                storeSeedMatrix(seedPathFile, origin);

                processedIndexes.add(origin);

                seedPathFile.close();

                System.gc();

                try {

                    wait(10);

                } catch (Exception e) {
                    // Ignore
                }

                boolean success = seedFilePath.delete();

                if (!success) {

                    System.out.println("Failed to delete " + seedFilePath + ".");
                    seedFilePath.deleteOnExit();

                }

                int tempProgress = (int) (1000.0 * ((double) origin) / nVertices);
                if (tempProgress > progress) {
                    progress = tempProgress;
                    double tempProgressDouble = (tempProgress) / 10.0;
                    System.out.println(tempProgressDouble + "%");
                }

            } catch (Throwable e) {

                System.out.println(origin + " Crashed.");

                crashed = true;

                throw new RuntimeException(e);
            }
        }

        /**
         * Computes all shortest paths from the origin index.
         */
        public void computeShortestPaths() {

            Vertex originVertex = graph.vertices[origin];

            ArrayList<Path> pathsToExpand = new ArrayList<>();

            for (int i = 0; i < originVertex.neighbors.length && !crashed; i++) {

                int neighbor = originVertex.neighbors[i];
                double weight = originVertex.weights[i];

                Path path = seedPathFile.getPath(neighbor);

                if (path == null
                        || path.getWeight() > weight
                        || path.getWeight() == weight && path.length() > 2) {

                    int[] pathIndexes = new int[2];
                    pathIndexes[0] = origin;
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

            if (processedIndexes.contains(lastIndex)) {

                for (int j = 0; j < nVertices; j++) {

                    if (!path.contains(j)) {

                        Path pathExtension = tempPathFile.getPath(lastIndex, j);

                        if (pathExtension != null) {

                            int totalLength = path.length() + pathExtension.length();

                            if (totalLength <= maxDepth) {

                                double totalWeight = pathExtension.getWeight() + path.getWeight();
                                Path seedPath = seedPathFile.getPath(j);

                                if (seedPath == null
                                        || seedPath.getWeight() > totalWeight
                                        || seedPath.getWeight() == totalWeight && seedPath.length() > totalLength) {

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
                            Path seedPath = seedPathFile.getPath(neighbor);

                            if (seedPath == null
                                    || seedPath.getWeight() > totalWeight
                                    || seedPath.getWeight() == totalWeight && seedPath.length() > totalLength) {

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
