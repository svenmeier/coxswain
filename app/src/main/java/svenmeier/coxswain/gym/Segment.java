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

import propoid.core.Property;
import propoid.core.Propoid;

/**
 */
public class Segment extends Propoid {

    public final Property<Difficulty> difficulty = property();

    public final Property<Integer> distance = property();

    public final Property<Integer> duration = property();

    public final Property<Integer> strokes = property();

    public final Property<Integer> energy = property();

    public final Property<Integer> speed = property();

    public final Property<Integer> strokeRate = property();

    public final Property<Integer> pulse = property();

    public Segment() {
        difficulty.set(Difficulty.EASY);

        distance.set(1000);
        duration.set(0);
        strokes.set(0);
        energy.set(0);

        speed.set(0);
        strokeRate.set(0);
        pulse.set(0);
    }

    public Segment(Difficulty difficulty) {
        this();

        this.difficulty.set(difficulty);
    }

    public int asDuration() {
        if (distance.get() > 0) {
            return distance.get() / 4;
        } else if (strokes.get() > 0) {
            return strokes.get() * 3;
        } else if (energy.get() > 0) {
            return energy.get() * 6;
        } else {
            return duration.get();
        }
    }

    public int getTarget() {
        if (distance.get() > 0) {
            return distance.get();
        } else if (strokes.get() > 0) {
            return strokes.get();
        } else if (energy.get() > 0) {
            return energy.get();
        } else if (duration.get() > 0) {
            return duration.get();
        }
        return 0;
    }

    public int getLimit() {
        if (speed.get() > 0) {
            return speed.get();
        } else if (strokeRate.get() > 0) {
            return strokeRate.get();
        } else if (pulse.get() > 0) {
            return pulse.get();
        }
        return 0;
    }

    public void clearLimit() {
        this.speed.set(0);
        this.strokeRate.set(0);
        this.pulse.set(0);
        this.energy.set(0);
    }

    public Segment setDuration(int duration) {
        this.duration.set(duration);
        this.distance.set(0);
        this.strokes.set(0);
        this.energy.set(0);

        return this;
    }

    public Segment setDistance(int distance) {
        this.duration.set(0);
        this.distance.set(distance);
        this.strokes.set(0);
        this.energy.set(0);

        return this;
    }

    public Segment setStrokes(int strokes) {
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(strokes);
        this.energy.set(0);

        return this;
    }

    public Segment setEnergy(int energy) {
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(0);
        this.energy.set(energy);

        return this;
    }

    public Segment setSpeed(int speed) {
        this.speed.set(speed);
        this.strokeRate.set(0);
        this.pulse.set(0);

        return this;
    }

    public Segment setStrokeRate(int strokeRate) {
        this.speed.set(0);
        this.strokeRate.set(strokeRate);
        this.pulse.set(0);

        return this;
    }

    public Segment setPulse(int pulse) {
        this.speed.set(0);
        this.strokeRate.set(0);
        this.pulse.set(pulse);

        return this;
    }
}