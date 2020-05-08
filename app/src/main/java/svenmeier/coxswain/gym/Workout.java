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
package svenmeier.coxswain.gym;

import android.location.Location;

import propoid.core.Property;
import propoid.core.Propoid;
import propoid.db.LookupException;

/**
 */
public class Workout extends Propoid {

    public final Property<Program> program = property();

    public final Property<Location> location = property();

    public final Property<Long> start = property();

    /**
     * seconds
     */
    public final Property<Integer> duration = property();

    /**
     * meters
     */
    public final Property<Integer> distance = property();

    public final Property<Integer> strokes = property();

    /**
     * kilocalories
     */
    public final Property<Integer> energy = property();

    /**
     * evaluate in performance
     */
    public final Property<Boolean> evaluate = property();

    public Workout() {
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(0);
        this.energy.set(0);
        this.evaluate.set(true);
    }

    public Workout(Program program) {
        this();

        this.program.set(program);
        this.start.set(System.currentTimeMillis());
    }

	/**
     * Handle a new measurement.
     *
     * @param measurement
     *
     * @throws IllegalArgumentException if measurement is illegal, i.e. is lower than current values
     */
    public void onMeasured(Measurement measurement) {
        update(this.distance, measurement.getDistance());
        update(this.duration, measurement.getDuration());
        update(this.strokes, measurement.getStrokes());
        update(this.energy, measurement.getEnergy());
    }

    private void update(Property<Integer> property, int value) {
        int oldValue = property.get();
        if (value < oldValue) {
            throw new IllegalArgumentException(String.format("%s: %s < %s", property.meta().name, value, oldValue));
        }
        property.set(value);
    }


    /**
     * Get the name of this workout's program.
     *
     * @param fallback name to use if program no longer exists
     */
    public String programName(String fallback) {
        String name = fallback;
        try {
            name = program.get().name.get();
        } catch (Exception noProgramSetOrAlreadyDeleted) {
        }
        return name;
    }

	/**
     * Can this workout be repeated, i.e. does its program still exist.
     */
    public boolean canRepeat() {
        try {
            return (program.get() != null);
        } catch (LookupException programAlreadyDeleted) {
            return false;
        }
    }
}
