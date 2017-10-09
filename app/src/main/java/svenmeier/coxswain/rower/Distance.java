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

import propoid.util.content.Preference;
import svenmeier.coxswain.R;

/**
 */
public class Distance {

    private static final float M_TO_MI = 0.000621371f;

    private static final float M_TO_YD = 1.09361f;

    private static final float M_TO_FT = 3.28084f;

    private Context context;

    private int m;

    public String formatted() {
        Preference<String> unit = Preference.getString(context, R.string.preference_distance_unit);

        switch (unit.get()) {
            case "mi":
                return String.format(context.getString(R.string.distance_miles), mi());
            case "yd":
                return String.format(context.getString(R.string.distance_yards), yd());
            case "ft":
                return String.format(context.getString(R.string.distance_feets), ft());
            default:
                return String.format(context.getString(R.string.distance_meters), m);
        }
    }

    public int mi() {
        return Math.round(m * M_TO_MI);
    }

    public int yd() {
        return Math.round(m * M_TO_YD);
    }

    public int ft() {
        return Math.round(m * M_TO_FT);
    }

    public int m() {
        return m;
    }

    public static Distance m(Context context, int m) {
        Distance distance = new Distance();

        distance.context = context;
        distance.m = m;

        return distance;
    }
}