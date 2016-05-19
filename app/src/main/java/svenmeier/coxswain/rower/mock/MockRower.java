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

    private long resettedAt = 0;

    private static double distance;

    private static double strokes;

    private static double energy;

    private final Snapshot memory;

    private boolean open;

    public MockRower(Snapshot memory) {
        this.memory = memory;
    }

    @Override
    public boolean open() {
        open = true;

        return true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getName() {
        return "Mockrower";
    }

    @Override
    public void reset() {
        resettedAt = System.currentTimeMillis();

        distance = 0;
        strokes = 0;
        energy = 0;
    }

    @Override
    public boolean row() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }

        if (open) {
            if (System.currentTimeMillis() > resettedAt + 5000) {
                // delay before achieveing anything

                distance += Math.random() * 0.5;
                memory.distance.set((int)distance);

                strokes += 0.04;
                memory.strokes.set((int)strokes);

                energy += 0.015;
                memory.energy.set((int)energy);
            }

            memory.speed.set((int)(250 +  (Math.random() * 100)));

            memory.strokeRate.set((int)(26 +  (Math.random() * 3)));

            memory.pulse.set((int)(80 +  (Math.random() * 10)));

            memory.drive.set((strokes % 1.0) < 0.3);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        open = false;
    }
}
