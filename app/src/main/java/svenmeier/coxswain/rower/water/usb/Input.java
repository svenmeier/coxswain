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

        return read;
    }
}
