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
package svenmeier.coxswain.rower.wired;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.ITrace;
import svenmeier.coxswain.rower.wired.usb.Consumer;
import svenmeier.coxswain.rower.wired.usb.ITransfer;

public class Protocol4 implements IProtocol {

    public static final String VERSION_UNKOWN = null;

    private static final long DEFAULT_THROTTLE = 25;

    private final ITransfer transfer;

    private final ITrace trace;

    private List<Field> fields = new ArrayList<>();

    private RatioCalculator ratioCalculator;

    private PowerCalculator powerCalculator;

    private int cycle = 0;

    private long throttle = DEFAULT_THROTTLE;

    private String version = VERSION_UNKOWN;

    private boolean resetting;

    private long lastTransfer = 0;

    private long lastPing = 0;

    private long lastDuration = 0;

    public Protocol4(ITransfer transfer, ITrace aTrace) {
        this.transfer = transfer;

        transfer.setTimeout(50);
        transfer.setBaudrate(115200);

        this.trace = aTrace;
        aTrace.comment("protocol 4");

        this.ratioCalculator = new RatioCalculator();
        this.powerCalculator = new PowerCalculator(trace);

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

                removeField(this);
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

                removeField(this);
            }
        });

        fields.add(new Field(null, "PING") {
            @Override
            protected void onInput(String message, Measurement measurement) {
                lastPing = System.currentTimeMillis();
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
                ratioCalculator.strokeStart(measurement, System.currentTimeMillis());
                powerCalculator.strokeStart(measurement, System.currentTimeMillis());
            }
        });

        fields.add(new Field(null, "SE") {
            @Override
            protected void onInput(String message, Measurement measurement) {
                ratioCalculator.strokeEnd(measurement, System.currentTimeMillis());
            }
        });

        fields.add(new NumberField(0x140, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (resetting == false) {
                    measurement.setStrokes(value);
                }
            }
        });

        fields.add(new NumberField(0x057, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (resetting == false) {
                    measurement.setDistance(value);
                }
            }
        });

        fields.add(new NumberField(0x14A, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (isIdleNotPaused(value)) {
                    value = 0;
                }

                measurement.setSpeed(value);
            }
        });

        fields.add(new NumberField(0x1A9, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (isIdleNotPaused(value)) {
                    value = 0;
                }

                measurement.setStrokeRate(value);
            }
        });

        fields.add(new NumberField(0x1A0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (value > 0) {
                    // Waterrower is sending pulse
                    measurement.setPulse(value);
                }
            }
        });

        fields.add(new NumberField(0x088, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (isIdleNotPaused(value)) {
                    measurement.setPower(0);
                } else {
                    powerCalculator.power(value);
                }
            }
        });

        fields.add(new NumberField(0x08A, NumberField.TRIPLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                if (resetting == false) {
                    measurement.setEnergy(value / 1000);
                }
            }
        });

        fields.add(new NumberField(0x1E0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Measurement measurement) {
                // duration hundredth
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
                if (value != measurement.getDuration()) {
                    lastDuration = System.currentTimeMillis();
                }

                if (resetting == false) {
                    measurement.setDuration(value);
                }
            }
        });
    }

    /**
     * When the rower is idle (i.e. no rudder movement) and not paused - used to clear
     * values as the S4 does it too.
     */
    private boolean isIdleNotPaused(int value) {
        long now = System.currentTimeMillis();

        boolean idle = (now - lastPing) < 2000;
        boolean paused = (now - lastDuration) > 2000;

        return idle && !paused;
    }

    public void setThrottle(long throttle) {
        this.throttle = throttle;
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

        if (System.currentTimeMillis() - lastTransfer < throttle) {
            return;
        }
        lastTransfer = System.currentTimeMillis();

        output();
    }

    private void output() {
        Field field = nextField();
        if (field != null) {
            String request = field.request;

            trace.onOutput(request);

            byte[] output = new byte[request.length() + 2];
            int c = 0;
            for (; c < request.length(); c++) {
                output[c] = (byte)request.charAt(c);
            }
            output[c++] = (byte)'\r';
            output[c++] = (byte)'\n';
            transfer.produce(output);

            field.onAfterOutput();
        }
    }

    private void input(Measurement measurement) {

        Consumer consumer = transfer.consumer();
        while (consumer.hasNext()) {
            char character = (char)consumer.next();
            if (character == '\n' || character == '\r') {
                String message = new String(consumer.consumed()).trim();
                if (message.isEmpty() == false) {
                    trace.onInput(message);

                    if (inputField(measurement, message) == false) {
                        trace.comment("unrecognized");
                    }
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
                removeField(this);

                resetting = false;
            }
        };
        addField(reset);

        // prevent duration, distance and strokes from being read until reset was send
        resetting = true;

        ratioCalculator.clear(System.currentTimeMillis());
    }

    private void addField(Field field) {
        fields.add(field);
    }

    private void removeField(Field field) {
        fields.remove(field);
    }
}