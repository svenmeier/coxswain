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

import svenmeier.coxswain.gym.Measurement;

/**
 * Adjust energy relative to weight.
 */
public class EnergyAdjuster extends Adjuster {

    private static final int WEIGHT_MIN = 40;

    private static final int WEIGHT_MAX = 160;

    private static final int DEFAULT_WEIGHT = 68;

    private static final double S4_CALORIES_FOR_WEIGHT = 257.1;

    private static final double CALORIES_FACTOR = 1.714;

    private static final double KG_TO_POUNDS = 2.20462;

    private int weight;

    private Measurement measurement;

	/**
     * Calculate with {@link #DEFAULT_WEIGHT}, i.e. don't adjust.
     */
    public EnergyAdjuster(Measurement measurement) {
        this(measurement, DEFAULT_WEIGHT);
    }

    public EnergyAdjuster(Measurement measurement, int weight) {
        super(measurement);

        if (weight < WEIGHT_MIN) {
            weight = WEIGHT_MIN;
        }

        if (weight > WEIGHT_MAX) {
            weight = WEIGHT_MAX;
        }

        this.weight = weight;
    }

    @Override
    public void setEnergy(int energy) {
	    if (measurement.getDistance() == 0) {
            // leave unadjusted
	        super.setEnergy(energy);
        } else {
            double adjusted = ((double)energy) - S4_CALORIES_FOR_WEIGHT + (CALORIES_FACTOR * (weight * KG_TO_POUNDS));

            super.setEnergy(Math.max(0, (int)adjusted));
        }
    }
}
