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
public abstract class Field {

    private static final int CODEPOINT_0 = 48;
    private static final int CODEPOINT_A = 65;

    public static final int SINGLE = 1;
    public static final int DOUBLE = 2;
    public static final int TRIPLE = 3;

    public static final int DEZ = 10;
    public static final int HEX = 16;

    private int base;

    final String request;

    final String response;

    /**
     * @param address memory address
     * @param size data size SINGLE, DOUBLE or TRIPLE
     * @param base radix of number encoding
     */
    Field(int address, int size, int base) {
        String ach = toAscii(address, TRIPLE, HEX);

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

        this.base = base;
    }

    Field(String response, int base) {
        this.request = null;
        this.response = response;
        this.base = base;
    }

    void update(String message, Snapshot memory) {
        if (message.startsWith(response)) {
            onUpdate(fromAscii(message, response.length(), base), memory);
        }
    }

    protected abstract void onUpdate(short value, Snapshot memory);

    private static short fromAscii(String ach, int start, int base) {
        short total = 0;

        for (int c = start; c < ach.length(); c++) {
            total *= base;

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
