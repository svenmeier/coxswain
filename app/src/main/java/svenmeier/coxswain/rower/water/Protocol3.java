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

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.water.usb.ITransfer;

public class Protocol3 implements IProtocol {

    private static final int TIMEOUT = 100;

    private final ITrace trace;

    private final ITransfer transfer;

    private int distanceInDecimeters;

    public final RatioCalculator ratioCalculator = new RatioCalculator();

    public Protocol3(ITransfer transfer, ITrace trace) {
        this.transfer = transfer;

        transfer.setTimeout(TIMEOUT);
        transfer.setBaudrate(1200);
        transfer.setData(8, ITransfer.PARITY_NONE, ITransfer.STOP_BIT_1_0, false);

        this.trace = trace;
        trace.comment("protocol 3");
    }

    @Override
    public void reset() {
        distanceInDecimeters = 0;

        ratioCalculator.clear(System.currentTimeMillis());
    }

    @Override
    public void transfer(Measurement measurement) {
        int length = transfer.bulkInput();

        byte[] buffer = transfer.buffer();
        for (int c = 0; c < length; c++) {

            // TODO calculate energy

            switch (buffer[c]) {
                case (byte)0xFB:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        measurement.pulse = buffer[++c] & 0xFF;
                    }
                    continue;
                case (byte)0xFC:
                    trace(buffer, c, 1);

                    measurement.strokes = measurement.strokes + 1;

                    ratioCalculator.recovering(measurement, System.currentTimeMillis());

                    continue;
                case (byte)0xFD:
                    if (c + 2 < length) {
                        trace(buffer, c, 3);

                        ratioCalculator.pulling(measurement, System.currentTimeMillis());

                        // voltage not used
                        c += 2;
                    }
                    continue;
                case (byte)0xFE:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        distanceInDecimeters += (buffer[++c] & 0xFF);

                        measurement.distance = distanceInDecimeters / 10;
                    }
                    continue;
                case (byte)0xFF:
                    if (c + 2 < length) {
                        trace(buffer, c, 3);

                        measurement.strokeRate = buffer[++c] & 0xFF;
                        measurement.speed = (buffer[++c] & 0xFF) * 10;
                    }
                    continue;
            }

            trace(buffer, c, 1);
        }
    }

    private void trace(byte[] buffer, int start, int length) {
        StringBuilder string = new StringBuilder(length * 3);

        for (int c = 0; c < length; c++) {
            if (c > 0) {
                string.append(' ');
            }

            int b = buffer[start + c] & 0xFF;

            string.append(hex[b >>> 4]);
            string.append(hex[b & 0x0F]);
        }

        trace.onInput(string);
    }

    private static final char[] hex = "0123456789ABCDEF".toCharArray();
}
