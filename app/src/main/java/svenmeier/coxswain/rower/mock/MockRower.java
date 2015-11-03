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
package svenmeier.coxswain.rower.mock;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class MockRower implements Rower {

    private static double distance;

    private static double strokes;

    private final Snapshot memory;

    private boolean open;

    public MockRower(Snapshot memory) {
        this.memory = memory;
    }

    @Override
    public synchronized boolean open() {
        open = true;

        return true;
    }

    @Override
    public synchronized boolean isOpen() {
        return open;
    }

    @Override
    public String getName() {
        return "Mockrower";
    }

    @Override
    public synchronized void reset() {
        distance = 0;
        strokes = 0;
    }

    @Override
    public synchronized boolean row() {
        try {
            this.wait(100);
        } catch (InterruptedException ignore) {
        }

        if (open) {
            distance += Math.random() * 0.5;
            memory.distance = (short)(distance);

            strokes += Math.random() * 0.15;
            memory.strokes = (short)(strokes);

            memory.speed = (short)(250 +  (Math.random() * 100));

            memory.strokeRate = (short)(26 +  (Math.random() * 3));

            memory.pulse = (short)(80 +  (Math.random() * 10));

            memory.pull = (strokes % 1.0) < 0.5;

            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        open = false;
    }
}
