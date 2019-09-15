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
package svenmeier.coxswain.rower;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;

/**
 */
public abstract class Rower extends Measurement {

    private final Handler handler = new Handler();

    private final Context context;

    private List<ICalculator> calculators = new ArrayList<>();

    protected final Callback callback;

    protected Rower(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;

        if (Preference.getBoolean(context, R.string.preference_adjust_speed).get()) {
            calculators.add(new SpeedCalculator());
        }

        if (Preference.getBoolean(context, R.string.preference_adjust_energy).get()) {
            calculators.add(new EnergyCalculator(Preference.getInt(context, R.string.preference_weight).fallback(90).get()));
        }
    }

    public abstract String getName();

    /**
     * Open the rower.
     */
    public abstract void open();

    public void reset() {
        duration = 0;
        distance = 0;
        strokes = 0;
        energy = 0;

        speed = 0;
        pulse = 0;
        strokeRate = 0;
        power = 0;

        strokeRatio = 0;
    }

    protected void toast(final String text) {
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void notifyMeasurement() {

        for (ICalculator calculator : calculators) {
            calculator.adjust(this);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onMeasurement();
            }
        });
    }

    /**
     * Close the rower.
     */
    public abstract void close();

    public interface Callback {
        void onConnected();

        void onMeasurement();

        void onDisconnected();
    }
}
