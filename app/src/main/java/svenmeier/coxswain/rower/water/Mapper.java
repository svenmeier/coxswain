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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import svenmeier.coxswain.gym.Snapshot;

public class Mapper {

    public final Field INIT;
    public final Field RESET;
    public final Field VERSION;

    private int cycle = 0;

    private Queue<Field> queued = new LinkedList<>();

    private List<Field> fields = new ArrayList<>();

    public Mapper() {
        fields.add(INIT = new Field("USB", "_WR_") {
            @Override
            protected boolean cycle() {
                return false;
            }

            @Override
            protected void onUpdate(String message, Snapshot memory) {
                onInit();
            }
        });

        fields.add(VERSION = new Field("IV?", "IV") {
            @Override
            protected boolean cycle() {
                return false;
            }

            @Override
            protected void onUpdate(String message, Snapshot memory) {
                onVersion(message);
            }
        });

        fields.add(RESET = new Field("RESET", null) {
            @Override
            protected boolean cycle() {
                return false;
            }

            @Override
            protected void onUpdate(String message, Snapshot memory) {
            }
        });

        fields.add(new Field(null, "ERROR") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                onError();
            }
        });

        fields.add(new Field(null, "SS") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
                memory.drive.set(true);
            }
        });

        fields.add(new Field(null, "SE") {
            @Override
            protected void onUpdate(String message, Snapshot memory) {
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

    protected void onInit() {
    }

    protected void onVersion(String version) {
    }

    protected void onError() {
    }

    /**
     * Queue a field that does not cycle.
     *
     * @param field
     *
     * @see Field#cycle()
     */
    public void queue(Field field) {
        if (field.cycle()) {
            throw new IllegalArgumentException("no need to queue cycling field");
        }

        queued.offer(field);
    }

    public String nextRequest() {
        Field field = queued.poll();
        if (field != null) {
            return field.request;
        }

        while (true) {
            field = fields.get(cycle);

            cycle = (cycle + 1) % fields.size();

            if (field.cycle()) {
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
