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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ControlTransfer;

public class Protocol4 implements IProtocol {

    private static final int TIMEOUT = 50;

    private static final long OUTPUT_THROTTLE = 25;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint input;
    private final UsbEndpoint output;

    private byte[] buffer;

    private final Writer trace;

    private int cycle = 0;

    private Queue<Field> queued = new LinkedList<>();

    private List<Field> cycled = new ArrayList<>();

    private long last = 0;

    public Protocol4(UsbDeviceConnection connection, UsbEndpoint output, UsbEndpoint input, Writer trace) {
        this.connection = connection;
        this.output = output;
        this.input = input;

        new ControlTransfer(connection).setBaudRate(115200);

        this.buffer = new byte[Math.min(output.getMaxPacketSize(), input.getMaxPacketSize())];

        this.trace = trace;

        cycled.add(new Field(null, "ERROR") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
            }
        });

        cycled.add(new Field(null, "SS") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                memory.drive.set(true);
            }
        });

        cycled.add(new Field(null, "SE") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                memory.drive.set(false);
            }
        });

        cycled.add(new NumberField(0x140, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokes.set(value);
            }
        });

        cycled.add(new NumberField(0x057, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.distance.set(value);
            }
        });

        cycled.add(new NumberField(0x14A, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.speed.set(value);
            }
        });

        cycled.add(new NumberField(0x1A9, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokeRate.set(value);
            }
        });

        cycled.add(new NumberField(0x1A0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.pulse.set(value);
            }
        });

        cycled.add(new NumberField(0x08A, NumberField.TRIPLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.energy.set(value / 1000);
            }
        });

        queued.offer(new Field("USB", "_WR_"));

        queued.offer(new Field("IV?", "IV"));
    }

    private Field nextField() {
        Field field = queued.poll();
        if (field != null) {
            return field;
        }

        while (true) {
            field = cycled.get(cycle);

            cycle = (cycle + 1) % cycled.size();

            if (field.request != null) {
                return field;
            }
        }
    }

    private void updateField(Snapshot memory, String message) {
        for (int f = 0; f < cycled.size(); f++) {
            cycled.get(f).update(message, memory);
        }
    }

    public void transfer(Snapshot memory) {

        output();

        input(memory);
    }

    private void output() {
        if (System.currentTimeMillis() - last < OUTPUT_THROTTLE) {
            return;
        }

        Field field = nextField();
        if (field != null) {
            String request = field.request;

            trace('>', request);

            int c = 0;
            for (; c < request.length(); c++) {
                buffer[c] = (byte)request.charAt(c);
            }
            buffer[c++] = '\r';
            buffer[c++] = '\n';

            connection.bulkTransfer(output, buffer, request.length(), TIMEOUT);
        }

        last = System.currentTimeMillis();
    }

    private void input(Snapshot memory) {
        int length = connection.bulkTransfer(input, buffer, buffer.length, TIMEOUT);
        StringBuilder response = new StringBuilder();
        for (int c = 0; c < length; c++) {
            char character = (char)buffer[c];
            if (character == '\n' || character == '\r') {
                if (response.length() > 0) {
                    String message = response.toString();

                    trace('<', message);

                    updateField(memory, message);

                    response.setLength(0);
                }
            } else {
                response.append(character);
            }
        }
    }

    private void trace(char prefix, String message) {
        try {
            trace.append(prefix);
            trace.append(' ');
            trace.append(message);
            trace.write('\n');
            trace.flush();
        } catch (IOException ignore) {
        }
    }

    @Override
    public void reset() {
        queued.offer(new Field("RESET", null));
    }
}
