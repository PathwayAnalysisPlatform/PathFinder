package no.uib.pap.pathfinder.io.path;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import static no.uib.pap.pathfinder.io.util.MemoryMappedFileUtils.closeBuffer;
import no.uib.pap.pathfinder.model.graph.Path;

/**
 * A memory mapped file where paths of fixed length can be stored.
 *
 * @author Marc Vaudel
 */
public class SeedPathFile {

    /**
     * The file.
     */
    public final File file;
    /**
     * The random access file.
     */
    private final RandomAccessFile raf;
    /**
     * The channel to the output file.
     */
    private final FileChannel fc;
    /**
     * The number of vertices.
     */
    private final int nVertices;
    /**
     * The number of bytes used by one path.
     */
    private final int pathSize;
    /**
     * The weights of the paths.
     */
    private final double[] weights;
    /**
     * The lengths of the paths.
     */
    private final int[] lengths;
    /**
     * The seed vertice.
     */
    public final int origin;

    /**
     * Constructor.
     *
     * @param pathFile the file containing the paths
     * @param nVertices the number of vertices
     * @param fixedLength the depth if fixed, -1 otherwise
     * @param origin the seed vertice
     */
    public SeedPathFile(File pathFile, int nVertices, int fixedLength, int origin) {

        try {

            file = pathFile;

            raf = new RandomAccessFile(pathFile, "rw");
            fc = raf.getChannel();

            this.nVertices = nVertices;
            this.pathSize = (fixedLength - 2) * 4;

            this.weights = new double[nVertices];
            Arrays.fill(weights, Double.NaN);

            this.lengths = new int[nVertices];
            Arrays.fill(lengths, -1);

            this.origin = origin;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
    }

    /**
     * Returns the index where to save the path.
     *
     * @param index the index of the last vertex in the path
     *
     * @return the index where to save the path
     */
    public long getIndex(int index) {

        return index * pathSize;

    }

    /**
     * Returns the weight of a path.
     *
     * @param endPath the end index of the path.
     *
     * @return the weight of a path
     */
    public double getWeight(int endPath) {

        return weights[endPath];

    }

    /**
     * Returns the number of vertices in the path.
     *
     * @param endPath the end index of the path
     *
     * @return the number of vertices in the path
     */
    public int getLength(int endPath) {

        return lengths[endPath];

    }
    
    /**
     * Indicates whether the given path has already been stored.
     * 
     * @param endPath the end index of the path
     * 
     * @return a boolean indicating whether the given path has already been stored
     */
    public boolean hasPath(int endPath) {
        
        return lengths[endPath] != -1;
        
    }

    /**
     * Reads the path from the file.
     *
     * @param endPath the index of the last vertex in the path
     *
     * @return the path ending on the desired vertex
     */
    public Path getPath(int endPath) {

        double weight = getWeight(endPath);

        if (weight == Double.NaN) {
            return null;
        }

        int length = getLength(endPath);

        if (length == 2) {

            int[] pathIndexes = new int[]{origin, endPath};

        }

        try {

            long index = getIndex(endPath);

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, index, pathSize);

            int[] pathIndexes = new int[length];

            pathIndexes[0] = origin;
            pathIndexes[length - 1] = endPath;

            for (int i = 1; i < length - 1; i++) {

                int vertice = buffer.getInt();

                if (vertice < 0 || vertice > nVertices) {
                    throw new IllegalArgumentException("Unexpected vertex index: " + vertice + ".");
                }

                pathIndexes[i] = vertice;
            }

            closeBuffer(buffer);

            return new Path(pathIndexes, weight);

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

    /**
     * Saves the path to the file.
     *
     * @param path the path to save
     */
    public void setPath(Path path) {

        int lastVertex = path.getEnd();

        weights[lastVertex] = path.getWeight();

        int length = path.length();

        lengths[lastVertex] = length;

        if (length > 2) {

            try {

                long index = getIndex(lastVertex);

                MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, index, pathSize);

                int[] pathIndexes = path.getPath();

                for (int i = 1; i < length - 1; i++) {

                    buffer.putInt(pathIndexes[i]);

                }

                closeBuffer(buffer);

            } catch (Exception e) {

                throw new RuntimeException(e);

            }
        }
    }

    /**
     * Closes the connection to the file.
     */
    public void close() {

        try {

            fc.close();
            raf.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
