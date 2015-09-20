package svenmeier.coxswain.rower.water;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import svenmeier.coxswain.MainActivity;
import svenmeier.coxswain.view.ProgramsFragment;

/**
 */
public class Output {

    private static final int PROCESSING_DELAY = 25; // milliseconds

    private static final int TIMEOUT = 25;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpoint;
    private final Object monitor;

    public byte[] buffer;

    private long last;

    public Output(UsbDeviceConnection connection, UsbEndpoint endpoint, Object monitor) {
        this.connection = connection;
        this.endpoint = endpoint;
        this.monitor = monitor;

        this.buffer = new byte[endpoint.getMaxPacketSize()];
    }

    public void write(String message) {
        // must be synchronized for wait below

        Log.d(MainActivity.TAG, String.format("writing %s", message));

        int length = message.length();
        if (length > buffer.length - 2) {
            throw new IllegalArgumentException("max length exceeded " + buffer.length);
        }

        try {
            long throttle = PROCESSING_DELAY - (System.currentTimeMillis() - last);
            if (throttle > 0) {
                monitor.wait(throttle);
            }
        } catch (InterruptedException ignore) {
        }

        for (int c = 0; c < length; c++) {
            buffer[c] = (byte)message.charAt(c);
        }

        buffer[length] = '\r';
        buffer[length + 1] = '\n';

        connection.bulkTransfer(endpoint, buffer, length + 2, TIMEOUT);

        last = System.currentTimeMillis();
    }
}
