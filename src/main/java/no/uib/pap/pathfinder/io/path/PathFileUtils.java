package no.uib.pap.pathfinder.io.path;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static no.uib.pap.pathfinder.io.util.MemoryMappedFileUtils.closeBuffer;
import no.uib.pap.pathfinder.model.graph.Path;

/**
 * Generic functions to query the path file.
 *
 * @author Marc Vaudel
 */
public class PathFileUtils {

    /**
     * Returns the index where to save the path.
     *
     * @param from the index of the first vertex in the path
     * @param to the index of the last vertex in the path
     *
     * @return the index where to save the path
     */
    public static int getIndex(int from, int to) {

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
     * @param indexes the file path indexes
     * @param fc the channel to the file
     *
     * @return the path ending on the desired vertex
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading the file
     */
    public static Path getPath(int from, int to, long[] indexes, FileChannel fc) throws IOException {

        if (from == to) {
            return null;
        }

        int pathIndex = getIndex(from, to);
        long startIndex = indexes[pathIndex];
        int size = (int) (indexes[pathIndex + 1] - startIndex);

        MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, startIndex, size);

        double weight = buffer.getDouble();

        int nVertices = buffer.getInt();

        int[] pathIndexes = new int[nVertices];

        for (int i = 0; i < nVertices; i++) {

            int vertex = buffer.getInt();
            pathIndexes[i] = vertex;

        }

        closeBuffer(buffer);

        return new Path(pathIndexes, weight);

    }
}
