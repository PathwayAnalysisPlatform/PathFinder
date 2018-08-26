package no.uib.pap.pathfinder.model.graph;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Simple model for a path.
 *
 * @author Marc Vaudel
 */
public class Path {

    /**
     * Array of vertices indexes traversed by this path.
     */
    private final int[] path;
    /**
     * Total weight of the path.
     */
    private final double weight;

    /**
     * Constructor.
     *
     * @param path Array of vertices indexes traversed by this path
     * @param weight Total weight of the path
     */
    public Path(int[] path, double weight) {

        this.path = path;
        this.weight = weight;

    }

    /**
     * Returns the path as int array.
     *
     * @return the path as int array
     */
    public int[] getPath() {
        return path;
    }

    /**
     * Returns the weight;
     *
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Returns the start index of the path.
     *
     * @return the start index of the path
     */
    public int getStart() {

        return path[0];

    }

    /**
     * Returns the end index of the path.
     *
     * @return the end index of the path
     */
    public int getEnd() {

        return path[path.length - 1];

    }

    /**
     * Returns the number of vertices in the path including start and end vertices.
     *
     * @return the number of vertices in the path including start and end vertices
     */
    public int length() {

        return path.length;

    }

    /**
     * Returns a boolean indicating whether the given index corresponds to a
     * vertex in the path.
     *
     * @param i the index of the vertex
     *
     * @return a boolean indicating whether the given index corresponds to a
     * vertex in the path
     */
    public boolean contains(int i) {
        
        return Arrays.stream(path)
                .anyMatch(index -> index == i);
        
    }
    
    /**
     * Returns this path with the vertices in reverse order.
     * 
     * @return this path with the vertices in reverse order
     */
    public Path reverse() {
        
            int[] reversedPath = IntStream.range(0, path.length)
                    .map(i -> path[path.length - i - 1])
                    .toArray();
            return new Path(reversedPath, weight);
    }

}
