package no.uib.pap.pathfinder.io.path;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.stream.IntStream;
import static no.uib.pap.pathfinder.io.util.MemoryMappedFileUtils.closeBuffer;
import no.uib.pap.pathfinder.model.graph.Path;

/**
 * A memory mapped file where paths of fixed length can be stored.
 *
 * @author Marc Vaudel
 */
public class TempPathFile {

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
     * The path length.
     */
    private final int fixedLength;
    /**
     * The number of bytes used by one path.
     */
    private final int pathSize;

    /**
     * Constructor.
     *
     * @param pathFile the file containing the paths
     * @param nVertices the number of vertices
     * @param fixedLength the depth if fixed, -1 otherwise
     */
    public TempPathFile(File pathFile, int nVertices, int fixedLength) {

        try {

            raf = new RandomAccessFile(pathFile, "rw");
            fc = raf.getChannel();

            this.nVertices = nVertices;
            this.fixedLength = fixedLength;
            this.pathSize = 8 + fixedLength * 4;

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
    public long getIndex(int from, int to) {

        int pathIndex = to * (to - 1) / 2 + from;

        return pathIndex * pathSize;

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

        if (from > to) {

            Path reversePath = getPath(to, from);
            
            return reversePath == null ? null : reversePath.reverse();

        }

        try {

            long index = getIndex(from, to);

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, index, pathSize);

            double weight = buffer.getDouble();

            if (weight == 0.0) {
                return null;
            } else if (weight < 0.0) {
                throw new IllegalArgumentException("Unexpected weight: " + weight + ".");
            }

            int[] pathIndexes = new int[fixedLength];

            int end = -1;

            for (int i = 0; i < fixedLength; i++) {

                int vertice = buffer.getInt();

                if (vertice < 0 || vertice > nVertices) {
                    throw new IllegalArgumentException("Unexpected weight: " + weight + ".");
                }

                pathIndexes[i] = vertice;

                if (vertice == to) {

                    end = i;
                    break;

                }
            }

            if (end != -1) {

                pathIndexes = Arrays.copyOf(pathIndexes, end + 1);

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

        try {

            int start = path.getStart();
            int end = path.getEnd();

            if (end < start) {

                path = path.reverse();
                start = path.getStart();
                end = path.getEnd();

            }

            long index = getIndex(start, end);

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, index, pathSize);

            buffer.putDouble(path.getWeight());

            int[] pathIndexes = path.getPath();

            for (int i = 0; i < pathIndexes.length; i++) {

                buffer.putInt(pathIndexes[i]);

            }

            closeBuffer(buffer);

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
