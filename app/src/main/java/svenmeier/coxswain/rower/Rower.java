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

import propoid.core.Property;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Workout;

/**
 */
public abstract class Rower extends Measurement {

    /**
     * Open the rower.
     */
    public abstract boolean open();

    public abstract boolean isOpen();

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

    /**
     * Row.
     *
     * @return whether still rowing
     */
    public abstract boolean row();

    /**
     * Close the rower.
     */
    public abstract void close();

    public abstract String getName();
}
