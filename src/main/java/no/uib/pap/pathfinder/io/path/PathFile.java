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
public class PathFile {

    /**
     * The random access file.
     */
    private final RandomAccessFile raf;
    /**
     * The channel to the output file.
     */
    private final FileChannel fc;
    /**
     * The path indexes.
     */
    private final long[] indexes;
    /**
     * The current index.
     */
    private long currentIndex = 0;

    /**
     * Constructor.
     *
     * @param pathFile the file containing the paths
     * @param nVertices the number of vertices
     */
    public PathFile(File pathFile, int nVertices) {

        try {

            raf = new RandomAccessFile(pathFile, "rw");
            fc = raf.getChannel();

            int nPath = nVertices * (nVertices - 1) / 2;
            indexes = new long[nPath + 1];

            Arrays.fill(indexes, -1l);

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
    }

    /**
     * Returns the index where to save the path.
     *
     * @param from the index of the first vertex in the path
     * @param to the index of the last vertex in the path
     *
     * @return the index where to save the path
     */
    public int getIndex(int from, int to) {

        int low, high;

        if (from < to) {
            low = from;
            high = to;
        } else {
            low = to;
            high = from;
        }

        return high * (high - 1) / 2 + low;

    }

    /**
     * Reads the path from the file.
     *
     * @param from the index of the first vertex in the path
     * @param to the index of the last vertex in the path
     *
     * @return the path ending on the desired vertex
     */
    public Path getPath(int from, int to) {

        if (from == to) {
            return null;
        }

        try {

            int pathIndex = getIndex(from, to);
            long startIndex = indexes[pathIndex];
            int size = (int) (indexes[pathIndex+1] - startIndex);

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, startIndex, size);

            double weight = buffer.getDouble();

            int nVertices = buffer.getInt();

            int[] pathIndexes = new int[nVertices];

            for (int i = 0; i < nVertices; i++) {

                int vertex = buffer.getInt();
                pathIndexes[i] = vertex;

            }

            return new Path(pathIndexes, weight);

        } catch (Exception e) {

            System.out.println("Path from " + from + " to " + to + " failed.");
            throw new RuntimeException(e);

        }

    }

    /**
     * Saves the path to the file.
     *
     * @param path the path to save
     */
    public void setPath(Path path) {

        try {

            int[] pathIndexes = path.getPath();
            int nIndexes = pathIndexes.length;

            long index = currentIndex;
            int size = 8 + 4 + 4 * nIndexes;

            int pathIndex = getIndex(pathIndexes[0], pathIndexes[nIndexes - 1]);
            indexes[pathIndex] = index;
            indexes[pathIndex + 1] = index + size;

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, index, size);

            buffer.putDouble(path.getWeight());
            
            buffer.putInt(nIndexes);

            for (int i = 0; i < nIndexes; i++) {

                buffer.putInt(pathIndexes[i]);

            }
            
            closeBuffer(buffer);

            currentIndex += size;

        } catch (Exception e) {

            throw new RuntimeException(e);

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
