package svenmeier.coxswain.util;

public class ByteUtils {
    public static String toHex(byte[] buffer) {
        StringBuilder string = new StringBuilder(buffer.length * 3);

        for (int c = 0; c < buffer.length; c++) {
            if (c > 0) {
                string.append(' ');
            }

            int b = buffer[c] & 0xFF;

            string.append(hex[b >>> 4]);
            string.append(hex[b & 0x0F]);
        }

        return string.toString();
    }

    private static final char[] hex = "0123456789ABCDEF".toCharArray();
}
