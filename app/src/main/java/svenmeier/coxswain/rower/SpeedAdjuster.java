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
 * Ignore speed and calculate from power instead.
 */
public class SpeedAdjuster extends Adjuster {

    private int cms;

    public SpeedAdjuster(Measurement measurement) {
        super(measurement);
    }

    @Override
    public void reset() {
        super.reset();

        cms = 0;
    }

    @Override
    public void setSpeed(int untrustedSpeed) {
        // magic formula see:
        // http://www.concept2.com/indoor-rowers/training/calculators/watts-calculator
        float mps = 0.709492f * (float) Math.pow(getPower(), 1d / 3d);

        super.setSpeed(Math.round(mps * 100));
    }

    @Override
    public void setDistance(int untrustedDistance) {
        if (untrustedDistance == 0) {
            super.setDistance(0);
        }
    }

    @Override
    public void setDuration(int newDuration) {
        int oldDuration = super.getDuration();

        super.setDuration(newDuration);

        if (newDuration > oldDuration) {
            int delta = newDuration - oldDuration;

            cms += delta * getSpeed();

            super.setDistance(cms / 100);
        }
    }
}
