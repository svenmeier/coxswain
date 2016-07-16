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

/**
 */
public class Workout extends Propoid {

    public final Property<Program> program = property();

    public final Property<String> name = property();

    public final Property<Location> location = property();

    public final Property<Long> start = property();

    public final Property<Integer> duration = property();

    public final Property<Integer> distance = property();

    public final Property<Integer> strokes = property();

    public final Property<Integer> energy = property();

    public Workout() {
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(0);
        this.energy.set(0);
    }

    public Workout(Program program) {
        this();

        this.program.set(program);
        this.name.set(program.name.get());
        this.start.set(System.currentTimeMillis());
    }

    public boolean onSnapshot(Snapshot snapshot) {
        this.distance.set(snapshot.distance.get());
        this.strokes.set(snapshot.strokes.get());
        this.energy.set(snapshot.energy.get());

        int duration = Math.round((float)(System.currentTimeMillis() - start.get()) / 1000f);

        if (duration != this.duration.get()) {
            this.duration.set(duration);

            return true;
        } else {
            return false;
        }
    }
}