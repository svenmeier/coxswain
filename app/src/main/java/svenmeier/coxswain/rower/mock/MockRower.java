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

import android.content.Context;
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
    private static final int START_DELAY = 4000;

    /**
     * Delay after rowing starts.
     */
    private static final int ROW_DELAY = 500;

    private long startAt;

    private double speedTemp;

    private double strokesTemp;

    private double energyTemp;

    private final Handler handler = new Handler();

    private Runnable delayed;

    private class Connected implements Runnable {
        @Override
        public void run() {
            callback.onConnected();

            delay(new Start(), START_DELAY);
        }
    };

    private class Start implements Runnable {
        @Override
        public void run() {
            delay(new Row(), ROW_DELAY);
        }
    };

    private class Row implements Runnable {
        @Override
        public void run() {
            row();

            notifyMeasurement();

            delay(this, ROW_DELAY);
        }
    };

    public MockRower(Context context, Callback callback) {
        super(context, callback);
    }

    private void delay(Runnable runnable, int delay) {
        if (delayed != null) {
            handler.removeCallbacks(delayed);
            this.delayed = null;
        }

        if (runnable != null) {
            this.delayed = runnable;
            handler.postDelayed(runnable, delay);
        }
    }

    @Override
    public void open() {
        reset();

        delay(new Connected(), CONNECTION_DELAY);
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

        delay(new Start(), START_DELAY);
    }

    private void row() {
        long now = System.currentTimeMillis();

        if (startAt == 0) {
            startAt = now;
        }

        setDuration((int)(now - startAt) / 1000);

        setDistance((int)((now - startAt) * speedTemp) / 1000);

        strokesTemp += 0.04;
        setStrokes((int) strokesTemp);

        energyTemp += 0.015;
        setEnergy((int) energyTemp);

        setSpeed((int)(speedTemp * 100));

        setStrokeRate((int)(26 +  (Math.random() * 3)));

        setStrokeRatio((int)(10 +  (Math.random() * 5)));

        setPulse((int)(80 +  (Math.random() * 10)));
    }

    @Override
    public void close() {
        delay(null, 0);
    }
}
