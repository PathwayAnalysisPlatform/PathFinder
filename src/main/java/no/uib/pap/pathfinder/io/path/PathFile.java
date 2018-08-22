package no.uib.pap.pathfinder.io.path;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import static no.uib.pap.pathfinder.io.path.PathFileUtils.getIndex;
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
     * The channel to the file.
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

            int size = 8 * (nPath + 1) + 4;

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, currentIndex, size);

            buffer.putInt(nPath);

            for (int i = 0; i < nPath + 1; i++) {

                buffer.putLong(indexes[i]);

            }

            closeBuffer(buffer);

            currentIndex += size;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }
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

        try {

            return PathFileUtils.getPath(from, to, indexes, fc);

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
            setIndex(pathIndex, index);
            setIndex(pathIndex + 1, index + size);

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
     * Sets the index of a path to the array of indexes and to the file.
     * 
     * @param pathIndex the index of the path
     * @param index the index in the file
     */
    public void setIndex(int pathIndex, long index) {

        try {

            indexes[pathIndex] = index;

            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 4 + 8 * pathIndex, 8);

            buffer.putLong(index);

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
