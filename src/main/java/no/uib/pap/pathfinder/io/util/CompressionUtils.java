package no.uib.pap.pathfinder.io.util;

import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import static no.uib.pap.pathfinder.util.Utils.encoding;

/**
 * Utility functions to inflate and deflate values.
 *
 * @author Marc Vaudel
 */
public class CompressionUtils {

    /**
     * Deflates a line.
     *
     * @param line the line to deflate
     *
     * @return the deflated line as byte array
     */
    public static byte[] deflate(String line) {

        try {

            byte[] input = line.getBytes(encoding);

            byte[] output = new byte[8 * line.length()];
            Deflater deflater = new Deflater();
            deflater.setInput(input);
            deflater.finish();
            int newLength = deflater.deflate(output);

            output = Arrays.copyOf(output, newLength);

            return output;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Inflates a line.
     *
     * @param input the deflated line
     *
     * @return the inflated line
     */
    public static String inflate(byte[] input) {

        try {

            Inflater inflater = new Inflater();
            inflater.setInput(input);
            byte[] result = new byte[8 * input.length];
            int newLength = inflater.inflate(result);
            inflater.end();

            return new String(result, 0, newLength, encoding);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
