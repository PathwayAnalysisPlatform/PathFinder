package no.uib.pap.pathfinder.io.util;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

/**
 * Utility functions to handle memory mapped files.
 *
 * @author Marc Vaudel
 */
public class MemoryMappedFileUtils {


    /**
     * Attempts at closing a buffer to avoid memory issues. Taken from
     * https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java.
     *
     * @param buffer the buffer to close
     */
    public static void closeBuffer(MappedByteBuffer buffer) {

        if (buffer == null || !buffer.isDirect()) {
            return;
        }

        try {
            
            Method cleaner = buffer.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
            clean.setAccessible(true);
            clean.invoke(cleaner.invoke(buffer));

        } catch (Exception ex) {
        }
    }
}
