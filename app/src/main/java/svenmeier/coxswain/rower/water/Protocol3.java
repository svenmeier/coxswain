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

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ITransfer;

public class Protocol3 implements IProtocol {

    private static final int TIMEOUT = 50;

    private final ITrace trace;

    private final ITransfer transfer;

    public Protocol3(ITransfer transfer, ITrace trace) {
        this.transfer = transfer;

        transfer.setBaudRate(1200);
        transfer.setTimeout(TIMEOUT);

        this.trace = trace;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean transfer(Snapshot memory) {
        int length = transfer.bulkInput();

        byte[] buffer = transfer.buffer();
        for (int c = 0; c < length; c++) {
            byte control = buffer[c];

            switch (control) {
                case (byte)0xFB:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.pulse.set(buffer[++c] & 0xFF);
                    }
                    continue;
                case (byte)0xFC:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.drive.set(false);
                        memory.strokes.set(memory.strokes.get() + (buffer[++c] & 0xFF));
                    }
                    continue;
                case (byte)0xFD:
                    if (c + 2 < length) {
                        trace(buffer, c, 3);

                        memory.drive.set(true);
                    }
                    continue;
                case (byte)0xFE:
                    if (c + 1 < length) {
                        trace(buffer, c, 2);

                        memory.distance.set(memory.distance.get() + (buffer[++c] & 0xFF) * 10);
                    }
                    continue;
                case (byte)0xFF:
                    if (c + 2 < length) {
                        trace(buffer, c, 3);

                        memory.strokeRate.set(buffer[++c] & 0xFF);
                        memory.speed.set((buffer[++c] & 0xFF) * 10);
                    }
                    continue;
            }

            trace(buffer, c, 1);

            // TODO calculate energy
        }

        return true;
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

        trace.onOutput(string);
    }

    private static final char[] hex = "0123456789ABCDEF".toCharArray();
}
