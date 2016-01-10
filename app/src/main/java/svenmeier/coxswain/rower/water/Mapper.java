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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.Application;
import svenmeier.coxswain.gym.Snapshot;

public class Mapper {

    public static final String INIT = "USB";
    public static final String RESET = "RESET";
    public static final String VERSION = "IV?";

    private int cycle = 0;

    private List<Field> fields = new ArrayList<>();

    public Mapper() {
        fields.add(new Field(null, "SS") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                memory.drive = true;
            }
        });

        fields.add(new Field(null, "SE") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                memory.drive = false;
            }
        });

        fields.add(new NumberField(0x140, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokes = (short)value;
            }
        });

        fields.add(new NumberField(0x057, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.distance = (short)value;
            }
        });

        fields.add(new NumberField(0x14A, NumberField.DOUBLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.speed = (short)value;
            }
        });

        fields.add(new NumberField(0x1A9, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.strokeRate = (short)value;
            }
        });

        fields.add(new NumberField(0x1A0, NumberField.SINGLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.pulse = (short)value;
            }
        });

        fields.add(new NumberField(0x08A, NumberField.TRIPLE_BYTE) {
            @Override
            protected void onUpdate(int value, Snapshot memory) {
                memory.energy = (short)Math.round(value / 1000f);
            }
        });
    }

    public String nextRequest() {
        while (true) {
            Field field = fields.get(cycle);

            cycle = (cycle + 1) % fields.size();

            if (field.request != null) {
                return field.request;
            }
        }
    }

    public void map(String message, Snapshot memory) {
        // don't use iterator, this method is called rapidly
        for (int f = 0; f < fields.size(); f++) {
            fields.get(f).update(message, memory);
        }
    }
}
