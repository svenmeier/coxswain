/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.rower.water.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import svenmeier.coxswain.MainActivity;
import svenmeier.coxswain.view.ProgramsFragment;

/**
 */
public class Output {

    private static final int PROCESSING_DELAY = 25; // milliseconds

    private static final int TIMEOUT = 25;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpoint;

    public byte[] buffer;

    private long last;

    public Output(UsbDeviceConnection connection, UsbEndpoint endpoint) {
        this.connection = connection;
        this.endpoint = endpoint;

        this.buffer = new byte[endpoint.getMaxPacketSize()];
    }

    public void write(String message) {
        int length = message.length();
        if (length > buffer.length - 2) {
            throw new IllegalArgumentException("max length exceeded " + buffer.length);
        }

        try {
            long throttle = PROCESSING_DELAY - (System.currentTimeMillis() - last);
            if (throttle > 0) {
                Thread.sleep(throttle);
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
