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

import android.os.Handler;

import svenmeier.coxswain.rower.Rower;

/**
 */
public class MockRower extends Rower {

    /**
     * Delay for establishing the connection.
     */
    private static final int CONNECTION_DELAY = 4000;

    /**
     * Delay after rowing starts.
     */
    private static final int ROWING_DELAY = 4000;

    private long startAt;

    private double speedTemp;

    private double strokesTemp;

    private double energyTemp;

    private final Handler handler = new Handler();

    private Runnable update = new Runnable() {
        @Override
        public void run() {
            row();

            callback.onMeasurement();

            handler.postDelayed(this, 500);
        }
    };

    public MockRower(Callback callback) {
        super(callback);
    }
    
    @Override
    public void open() {
        reset();
    }

    @Override
    public String getName() {
        return "Mockrower";
    }

    @Override
    public void reset() {
        super.reset();

        startAt = 0;
        
        speedTemp = 2.5 + Math.random();
        strokesTemp = 0;
        energyTemp = 0;

        handler.removeCallbacks(update);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onConnected();

                handler.postDelayed(update, ROWING_DELAY);
            }
        }, CONNECTION_DELAY);
    }

    private void row() {
        long now = System.currentTimeMillis();

        if (startAt == 0) {
            startAt = now;
        }

        duration = (int)(now - startAt) / 1000;

        distance = (int)((now - startAt) * speedTemp) / 1000;

        strokesTemp += 0.04;
        strokes = (int) strokesTemp;

        energyTemp += 0.015;
        energy = (int) energyTemp;

        speed = (int)(speedTemp * 100);

        strokeRate = (int)(26 +  (Math.random() * 3));

        strokeRatio = (int)(10 +  (Math.random() * 5));

        pulse = (int)(80 +  (Math.random() * 10));

        callback.onMeasurement();
    }

    @Override
    public void close() {
        handler.removeCallbacks(update);
    }
}
