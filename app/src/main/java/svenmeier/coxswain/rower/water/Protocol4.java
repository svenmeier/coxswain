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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ITransfer;

public class Protocol4 implements IProtocol {

    private static final int TIMEOUT = 50;

    private static final long DEFAULT_OUTPUT_THROTTLE = 25;

    private final ITransfer transfer;

    private final ITrace trace;

    private List<Field> fields = new ArrayList<>();

    private int cycle = 0;

    private long outputThrottle = DEFAULT_OUTPUT_THROTTLE;

    private long lastOutput = 0;

    private String version;

    public Protocol4(ITransfer transfer, ITrace trace) {
        this.transfer = transfer;

        transfer.setBaudRate(115200);
        transfer.setTimeout(TIMEOUT);

        this.trace = trace;

        fields.add(new Field("USB", "_WR_") {
            /**
             * Remove on input and setup other fields.
             */
            @Override
            protected void onInput(String message, Snapshot memory) {
                fields.remove(this);

                init();
            }
        });
    }

    private void init() {
        cycle = 0;

        fields.add(new Field("IV?", "IV") {
            /**
             * Remove on input and keep version.
             */
            @Override
            protected void onInput(String message, Snapshot memory) {
                fields.remove(this);

                version = message.substring(response.length());

                trace.comment("version " + version);
            }
        });

        fields.add(new Field(null, "ERROR") {
            @Override
            protected void onInput(String message, Snapshot memory) {
            }
        });

        fields.add(new Field(null, "SS") {
            @Override
            protected void onInput(String message, Snapshot memory) {
                memory.drive.set(true);
            }
        });

        fields.add(new Field(null, "SE") {
            @Override
            protected void onInput(String message, Snapshot memory) {
                memory.drive.set(false);
            }
        });

        fields.add(new NumberField(0x140, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokes.set(value);
            }
        });

        fields.add(new NumberField(0x057, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.distance.set(value);
            }
        });

        fields.add(new NumberField(0x14A, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.speed.set(value);
            }
        });

        fields.add(new NumberField(0x1A9, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokeRate.set(value);
            }
        });

        fields.add(new NumberField(0x1A0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.pulse.set(value);
            }
        });

        fields.add(new NumberField(0x08A, NumberField.TRIPLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.energy.set(value / 1000);
            }
        });
    }

    public void setOutputThrottle(long outputThrottle) {
        this.outputThrottle = outputThrottle;
    }

    public String getVersion() {
        return version;
    }

    private Field nextField() {
        for (int f = 0; f < fields.size(); f++) {
            cycle = cycle % fields.size();

            Field field = fields.get(cycle);

            cycle++;

            if (field.request != null) {
                return field;
            }
        }

        return null;
    }

    private void inputField(Snapshot memory, String message) {
        for (int f = 0; f < fields.size(); f++) {
            fields.get(f).input(message, memory);
        }
    }

    public void transfer(Snapshot memory) {

        input(memory);

        output();
    }

    private void output() {
        if (System.currentTimeMillis() - lastOutput < outputThrottle) {
            return;
        }
        lastOutput = System.currentTimeMillis();

        Field field = nextField();
        if (field != null) {
            String request = field.request;

            trace.onOutput(request);

            byte[] buffer = transfer.buffer();
            int c = 0;
            for (; c < request.length(); c++) {
                buffer[c] = (byte)request.charAt(c);
            }
            buffer[c++] = '\r';
            buffer[c++] = '\n';

            transfer.bulkOutput(c);

            field.onAfterOutput();
        }
    }

    private void input(Snapshot memory) {
        int length = transfer.bulkInput();
        if (length > 0) {
            byte[] buffer = transfer.buffer();

            StringBuilder response = new StringBuilder();
            for (int c = 0; c < length; c++) {
                char character = (char)buffer[c];
                if (character == '\n' || character == '\r') {
                    if (response.length() > 0) {
                        String message = response.toString();

                        trace.onInput(message);

                        inputField(memory, message);

                        response.setLength(0);
                    }
                } else {
                    response.append(character);
                }
            }
        }
    }

    @Override
    public void reset() {
        cycle = 0;

        fields.add(new Field("RESET", null) {
            /**
             * Remove after output.
             */
            @Override
            protected void onAfterOutput() {
                fields.remove(this);
            }
        });
    }
}
