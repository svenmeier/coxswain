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
public class Energy {

    private static final float KCAL_TO_WH = 1.163f;

    private static final float KCAL_TO_KJ = 4.185f;

    private Context context;

    private int kcal;

    public String formatted() {
        Preference<String> unit = Preference.getString(context, R.string.preference_energy_unit);

        switch (unit.get()) {
            case "kJ":
                return String.format(context.getString(R.string.energy_kilojoules), kj());
            case "Wh":
                return String.format(context.getString(R.string.energy_watthours), wh());
            default:
                return String.format(context.getString(R.string.energy_kilocalories), kcal);
        }
    }

    public int wh() {
        return Math.round(kcal * KCAL_TO_WH);
    }

    public int kj() {
        return Math.round(KCAL_TO_KJ * kcal);
    }

    public int kcal() {
        return kcal;
    }

    public static Energy kcal(Context context, int kcal) {
        Energy energy = new Energy();

        energy.context = context;
        energy.kcal = kcal;

        return energy;
    }
}