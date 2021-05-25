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
import android.util.Log;
import android.widget.Toast;

import propoid.util.content.Preference;
import svenmeier.coxswain.BuildConfig;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;

/**
 */
public abstract class Rower {

    private final Handler handler = new Handler();

    private final Context context;

    protected final Measurement measurement;

    private final Runnable onMeasurement = new Runnable() {
        @Override
        public void run() {
            callback.onMeasurement(measurement);
        }
    };

    protected final Callback callback;

    protected ITrace trace = new NullTrace();

    protected Rower(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;

        this.measurement = createMeasurement();

        if (Preference.getBoolean(context, R.string.preference_hardware_trace).get()) {
            try {
                trace = new FileTrace(context);
            } catch (Exception e) {
                Log.e(Coxswain.TAG, "cannot open trace", e);
            }
        }
        trace.comment(String.format("coxswain %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }

    protected Measurement createMeasurement() {
        Measurement measurement = new Measurement();

        if (Preference.getBoolean(context, R.string.preference_adjust_speed).get()) {
            measurement = new SpeedAdjuster(measurement);
        }

        if (Preference.getBoolean(context, R.string.preference_adjust_energy).get()) {
            measurement = new EnergyAdjuster(measurement, Preference.getInt(context, R.string.preference_weight).fallback(90).get());
        }

        return measurement;
    }

    public abstract String getName();

    /**
     * Open the rower.
     */
    public abstract void open();

    public Measurement getMeasurement() {
        return measurement;
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
        // prevent piling up
        handler.removeCallbacks(onMeasurement);
        handler.post(onMeasurement);
    }

    /**
     * Close the rower.
     */
    public void close() {
        if (trace != null) {
            trace.close();
            trace = new NullTrace();
        }
    }

    public interface Callback {
        void onConnected();

        void onMeasurement(Measurement measurement);

        void onDisconnected();
    }
}