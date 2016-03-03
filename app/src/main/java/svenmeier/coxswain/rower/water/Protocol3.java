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
package svenmeier.coxswain.rower.water;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ControlTransfer;

public class Protocol3 implements IProtocol {

    private static final int TIMEOUT = 50;

    private final Writer trace;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpoint;
    private byte[] buffer;

    public Protocol3(UsbDeviceConnection connection, UsbEndpoint endpoint, Writer trace) {
        this.connection = connection;
        this.endpoint = endpoint;
        this.buffer = new byte[endpoint.getMaxPacketSize()];

        new ControlTransfer(connection).setBaudRate(1200);

        this.trace = trace;
    }

    @Override
    public void reset() {
    }

    @Override
    public void transfer(Snapshot memory) {
        int length = connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);

        for (int c = 0; c < length; c++) {
            byte control = buffer[c];

            switch (control) {
                case (byte)0xFE:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.distance.set(memory.distance.get() + (int)buffer[++c]);
                    }
                    continue;
                case (byte)0xFC:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.strokes.set(memory.strokes.get() + (int) buffer[++c]);
                    }
                    continue;
                case (byte)0xFB:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.pulse.set((int) buffer[++c]);
                    }
                    continue;
                case (byte)0xFF:
                    if (c + 2 < length) {
                        trace(buffer, c, 3);

                        memory.strokeRate.set((int)buffer[++c]);
                        memory.speed.set(((int)buffer[++c]) * 10);
                    }
                    continue;
            }

            trace(buffer, c, 1);

            // TODO calculate energy
        }
    }

    private void trace(byte[] buffer, int start, int length) {
        try {
            trace.write("< ");

            for (int c = 0; c < length; c++) {
                trace.write(Byte.toString(buffer[start + c]));
                trace.write(' ');
            }

            trace.write('\n');
            trace.flush();
        } catch (IOException ignore) {
        }
    }
}
