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
package svenmeier.coxswain.rower.wired;

import svenmeier.coxswain.gym.Measurement;

public class RatioCalculator {

    // actually would be 10 for one decimal place, but S4
    // reports pulls too late and recover too early -
    // thus we reduce the recovering phase
    public static final int MULTIPLIER = 8;

    public static final int MAX = 99;

    public boolean pulling = true;

    private long start = 0;

    private long pullDuration;

    private long recoverDuration;

    public void clear(long now) {
        pulling = true;
        start = now;

        pullDuration = 0;
        recoverDuration = 0;
    }

    public void strokeStart(Measurement measurement, long now) {
        if (pulling == false) {
            pulling = true;

            recoverDuration = (now - start);
            start = now;
        }
    }

    public void strokeEnd(Measurement measurement, long now) {
        if (pulling) {
            pulling = false;

            pullDuration = (now - start);
            start = now;

            int ratio = Math.min((int) (MULTIPLIER * recoverDuration / pullDuration), MAX);
            measurement.setStrokeRatio(ratio);
        }
    }
}
