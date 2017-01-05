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

import svenmeier.coxswain.rower.Rower;

/**
 */
public class MockRower extends Rower {

    public static MockRower openMock;

    private long startAt = 0;

    private double speedTemp;

    private double strokesTemp;

    private double energyTemp;

    private boolean open;

    public MockRower() {
    }

    @Override
    public boolean open() {
        open = true;

        openMock = this;

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
        super.reset();

        startAt = System.currentTimeMillis() + 2000;

        speedTemp = 2.5 + Math.random();
        strokesTemp = 0;
        energyTemp = 0;
    }

    @Override
    public boolean row() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }

        if (open) {
            long now = System.currentTimeMillis();
            if (now > startAt) {
                // delay before achieving anything

                duration = (int)(now - startAt) / 1000;

                distance = (int)((now - startAt) * speedTemp) / 1000;

                strokesTemp += 0.04;
                strokes = (int) strokesTemp;

                energyTemp += 0.015;
                energy = (int) energyTemp;
            }

            speed = (int)(speedTemp * 100);

            strokeRate = (int)(26 +  (Math.random() * 3));

            strokeRatio = (int)(10 +  (Math.random() * 5));

            pulse = (int)(80 +  (Math.random() * 10));

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        open = false;

        if (openMock == this) {
            openMock = null;
        }
    }
}
