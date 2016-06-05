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

/**
 */
public abstract class NumberField extends Field {

    private static final int CODEPOINT_0 = 48;
    private static final int CODEPOINT_A = 65;

    public static final int SINGLE_BYTE = 1;
    public static final int DOUBLE_BYTE = 2;
    public static final int TRIPLE_BYTE = 3;

    /**
     * @param address memory address
     * @param size data size SINGLE_BYTE, DOUBLE_BYTE or TRIPLE_BYTE
     */
    NumberField(int address, int size) {
        String ach = toAscii(address, 3, 16);

        switch (size) {
            case 1:
            this.request = "IRS" + ach;
            this.response = "IDS" + ach;
                break;
            case 2:
            this.request = "IRD" + ach;
            this.response = "IDD" + ach;
                break;
            case 3:
            this.request = "IRT" + ach;
            this.response = "IDT" + ach;
                break;
            default:
                throw new IllegalArgumentException("unkown size " + size);
        }
    }

    @Override
    protected void onInput(String message, Snapshot memory) {
        onUpdate(fromAscii(message, response.length()), memory);
    }

    protected abstract void onUpdate(int value, Snapshot memory);

    private static int fromAscii(String ach, int start) {
        int total = 0;

        for (int c = start; c < ach.length(); c++) {
            total *= 16;

            int codepoint = ach.codePointAt(c);
            int digit = codepoint - CODEPOINT_0;
            if (digit > 9) {
                digit = 10 + (codepoint - CODEPOINT_A);
            }

            total += digit;
        }

        return total;
    }

    private static String toAscii(int value, int length, int base) {
        String s = Integer.toString(value, base).toUpperCase();

        while (s.length() < length) {
            s = '0' + s;
        }

        return s;
    }
}
