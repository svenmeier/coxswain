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

    private static long distance;

    private static long strokes;

    private final Snapshot memory;

    private boolean open;

    public MockRower(Snapshot memory) {
        this.memory = memory;
    }

    @Override
    public synchronized void open() {
        open = true;

        onStart();
    }

    @Override
    public synchronized boolean isOpen() {
        return open;
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
            distance += (int) (Math.random() * 3);
            memory.distance = (short)(distance / 8);

            strokes++;
            memory.strokes = (short)(strokes / 18);

            memory.speed = (short)(250 +  (Math.random() * 100));

            memory.strokeRate = (short)(26 +  (Math.random() * 3));

            memory.pulse = (short)(80 +  (Math.random() * 10));

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
    }

    @Override
    public synchronized void close() {
        open = false;
    }
}
