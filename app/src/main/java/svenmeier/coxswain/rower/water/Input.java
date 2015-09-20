package svenmeier.coxswain.rower.water;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import svenmeier.coxswain.MainActivity;
import svenmeier.coxswain.view.ProgramsFragment;

/**
 */
public class Input {

    private static final int TIMEOUT = 25;

    private byte[] buffer;

    private StringBuilder string;

    private int start;
    private int end;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpoint;

    public Input(UsbDeviceConnection connection, UsbEndpoint endpoint) {
        this.connection = connection;
        this.endpoint = endpoint;

        this.buffer = new byte[endpoint.getMaxPacketSize()];
        this.string = new StringBuilder(endpoint.getMaxPacketSize());
    }

    public String read() {
        String read = null;

        // remove leading noise
        while (start < end) {
            if (buffer[start] != (byte)'\r' && buffer[start] != (byte)'\n') {
                break;
            }
            start++;
        }

        if (start >= end) { // end could be -1
            // acquire new data
            start = 0;
            end = connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);
            Log.d(MainActivity.TAG, String.format("acquired %s", end));
        }

        while (start < end) {
            if (buffer[start] == (byte)'\r' || buffer[start] == (byte)'\n') {
                read = string.substring(0, start);
                string.setLength(0);
                break;
            }

            string.append((char)buffer[start]);
            start++;
        }

        Log.d(MainActivity.TAG, String.format("reading %s", read));
        return read;
    }
}
