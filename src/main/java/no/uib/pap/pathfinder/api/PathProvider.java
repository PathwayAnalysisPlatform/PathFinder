package no.uib.pap.pathfinder.api;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import no.uib.pap.pathfinder.io.path.PathFileUtils;
import static no.uib.pap.pathfinder.io.util.MemoryMappedFileUtils.closeBuffer;
import no.uib.pap.pathfinder.model.graph.Path;

/**
 * This class provides the shortest path based on a file created by PathFile.
 *
 * Note: the connection to the file seems to remain open for a bit of time after
 * calling close. This might result eg in files not being deleted. gc and waiting
 * seem to help.
 *
 * @author Marc Vaudel
 */
public class PathProvider implements Closeable {

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
     * Constructor.
     *
     * @param pathFile the path file
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading the file.
     */
    public PathProvider(File pathFile) throws IOException {

        // Map file
        raf = new RandomAccessFile(pathFile, "r");
        fc = raf.getChannel();

        // Get the number of paths in the file
        MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, 4);

        int nPath = buffer.getInt();

        closeBuffer(buffer);

        // Get the paths indexes
        indexes = new long[nPath + 1];

        buffer = fc.map(FileChannel.MapMode.READ_ONLY, 4, (nPath + 1) * 8);

        for (int i = 0; i < nPath + 1; i++) {

            indexes[i] = buffer.getLong();

        }

        closeBuffer(buffer);

    }

    /**
     * Reads the path from the file.
     *
     * @param from the index of the first vertex in the path
     * @param to the index of the last vertex in the path
     *
     * @return the path ending on the desired vertex
     *
     * @throws java.io.IOException exception thrown if an error occurred while
     * reading the file.
     */
    public Path getPath(int from, int to) throws IOException {

        return PathFileUtils.getPath(from, to, indexes, fc);
    }

    @Override
    public void close() throws IOException {

        // Close connections to the file
        fc.close();
        raf.close();
    }

}
