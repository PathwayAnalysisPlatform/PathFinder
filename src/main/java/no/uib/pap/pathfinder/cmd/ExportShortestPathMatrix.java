package no.uib.pap.pathfinder.cmd;

import java.io.File;
import no.uib.pap.pathfinder.ShortestPath;
import no.uib.pap.pathfinder.io.network.NetworkPool;
import no.uib.pap.pathfinder.model.graph.Graph;
import no.uib.pap.pathfinder.util.ProgressHandler;

/**
 * Exports the shortest path matrix.
 *
 * @author Marc Vaudel
 */
public class ExportShortestPathMatrix {

    /**
     * A simple progress handler.
     */
    private ProgressHandler progressHandler = new ProgressHandler();

    /**
     * Exports the shortest path matrix for the graphs available in the pool.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {

            int nThreads = Integer.parseInt(args[0]);

            ExportShortestPathMatrix espm = new ExportShortestPathMatrix();
            espm.exportMatrices(nThreads);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor.
     */
    public ExportShortestPathMatrix() {

    }

    /**
     * Export the shortest paths matrices for all implemented graphs.
     *
     * @param nThreads the number of threads to use
     */
    public void exportMatrices(int nThreads) {

        String mainTask = "    Reactome";
        progressHandler.start(mainTask);

        String task = "Reactome - Import";
        progressHandler.start(task);
        Graph graph = NetworkPool.getReactomeGraph();
        progressHandler.end(task);

        task = "Reactome - Computing shortest path";
        progressHandler.start(task);
        
        File pathFile = new File(NetworkPool.reactomePathFile);
        if (pathFile.exists()) {
            pathFile.delete();
        }
        
        ShortestPath shortestPath = new ShortestPath(graph, pathFile);
        shortestPath.computeMatrix(nThreads);
        
        progressHandler.end(task);

        progressHandler.end(mainTask);

    }

}
