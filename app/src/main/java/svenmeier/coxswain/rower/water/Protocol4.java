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

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.water.usb.ITransfer;

public class Protocol4 implements IProtocol {

    public static final String VERSION_UNKOWN = null;

    private static final int TIMEOUT = 50;

    private static final long DEFAULT_OUTPUT_THROTTLE = 25;

    private final ITransfer transfer;

    private final ITrace trace;

    private List<Field> fields = new ArrayList<>();

    public final RatioCalculator ratioCalculator = new RatioCalculator();

    public final EnergyCalculator energyCalculator = new EnergyCalculator();

    private int cycle = 0;

    private long outputThrottle = DEFAULT_OUTPUT_THROTTLE;

    private long lastOutput = 0;

    private String version = VERSION_UNKOWN;

    public Protocol4(ITransfer transfer, ITrace aTrace) {
        this.transfer = transfer;

        transfer.setTimeout(TIMEOUT);
        transfer.setBaudrate(115200);

        this.trace = aTrace;
        aTrace.comment("protocol 4");

        fields.add(new Field("USB", "_WR_") {

            /**
             * Output once only.
             */
            @Override
            protected void onAfterOutput() {
                this.request = null;
            }

            @Override
            protected void onInput(String message, Measurement measurement) {
                onHandshake();

                trace.comment("handshake complete");
            }
        });
    }

    private void onHandshake() {
        cycle = 0;

        fields.add(new Field("IV?", "IV") {

            /**
             * Output once only.
             */
            @Override
            protected void onAfterOutput() {
                this.request = null;
            }

            @Override
            protected void onInput(String message, Measurement measurement) {
                version = message.substring(response.length());

                trace.comment("version " + version);
            }
        });

        fields.add(new Field(null, "PING") {
            @Override
            protected void onInput(String message, Measurement measurement) {
            }
        });

        fields.add(new Field(null, "ERROR") {
            @Override
            protected void onInput(String message, Measurement measurement) {
            }
        });

        fields.add(new Field(null, "SS") {
            @Override
            protected void onInput(String message, Measurement measurement) {
                ratioCalculator.pulling(measurement, System.currentTimeMillis());
            }
        });

        fields.add(new Field(null, "SE") {
            @Override
            protected void onInput(String message, Measurement measurement) {
                ratioCalculator.recovering(measurement, System.currentTimeMillis());
            }
        });
        fields.add(new NumberField(0x140, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.strokes = value;
            }
        });

        fields.add(new NumberField(0x057, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.distance = value;
            }
        });

        fields.add(new NumberField(0x14A, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.speed = value;
            }
        });

        fields.add(new NumberField(0x1A9, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.strokeRate = value;
            }
        });

        fields.add(new NumberField(0x1A0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.pulse = value;
            }
        });

        fields.add(new NumberField(0x08A, NumberField.TRIPLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.energy = energyCalculator.energy(value);
            }
        });

        fields.add(new NumberField(0x1E1, NumberField.TRIPLE_BYTE) {
			/**
			 * Duration is sent in decimal representation.
             */
            @Override
            protected int fromAscii(String data, int start) {
                int total = 0;

                for (int c = start; c < data.length(); c++) {
                    total *= c % 2 == 0 ? 6 : 10;

                    int codepoint = (int)data.charAt(c);
                    int digit = codepoint - CODEPOINT_0;

                    total += digit;
                }

                return total;
            }

            @Override
            protected void onUpdate(int value, Measurement measurement) {
                measurement.duration = value;
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

    private boolean inputField(Measurement measurement, String message) {

        for (int f = 0; f < fields.size(); f++) {
            if (fields.get(f).input(message, measurement)) {
                return true;
            }
        }

        return false;
    }

    public void transfer(Measurement measurement) {

        input(measurement);

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

    private void input(Measurement measurement) {
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

                        if (inputField(measurement, message) == false) {
                            trace.comment("unrecognized");
                        }

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
        Field reset = new Field("RESET", null) {
            /**
             * Remove immediately.
             */
            @Override
            protected void onAfterOutput() {
                fields.remove(this);
            }
        };
        fields.add(reset);

        cycle = fields.indexOf(reset);

        ratioCalculator.clear(System.currentTimeMillis());
    }
}
