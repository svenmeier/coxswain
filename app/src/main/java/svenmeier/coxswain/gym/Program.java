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

import java.util.ArrayList;
import java.util.List;

import propoid.core.Property;
import propoid.core.Propoid;
import propoid.db.aspect.Row;

/**
 */
public class Program extends Propoid {

    public final Property<String> name = property();

    public final Property<List<Segment>> segments = property();

    public Program() {
    }

    public Program(String name) {
        this.name.set(name);

        this.segments.set(new ArrayList<Segment>());
        this.segments.get().add(new Segment(Difficulty.EASY));
    }

    public List<Segment> getSegments() {
        return segments.get();
    }

    public int getSegmentsCount() {
        return segments.get().size();
    }

    public Segment getSegment(int index) {
        return segments.get().get(index);
    }

    public Segment getNextSegment(Segment segment) {
        int index = segments.get().indexOf(segment);
        if (index == segments.get().size() - 1) {
            return null;
        }
        index++;
        return segments.get().get(index);
    }

    public void addSegment(Segment segment) {
        segments.get().add(segment);
    }

    public int asDuration() {
        int duration = 0;

        for (int s = 0; s < segments.get().size(); s++) {
            duration += segments.get().get(s).asDuration();
        }

        return duration;
    }

    public void removeSegment(Segment segment) {
        segments.get().remove(segment);

        if (segments.get().isEmpty()) {
            segments.get().add(new Segment(Difficulty.EASY));
        }
    }

    public void createSegmentBefore(Segment segment) {
        int index = segments.get().indexOf(segment);

        segments.get().add(index, new Segment(Difficulty.EASY));
    }

    public void createSegmentAfter(Segment segment) {
        int index = segments.get().indexOf(segment);

        segments.get().add(index + 1, new Segment(Difficulty.EASY));
    }

    public Segment duplicateSegmentAfter(Segment segment) {
        int index = segments.get().indexOf(segment);
        Segment duplicate = segment.copy();
        segments.get().add(index + 1, duplicate);
        return duplicate;
    }


    public static Program kilocalories(String name, int kcal, Difficulty difficulty) {
        Program program = new Program(name);

        program.getSegment(0).setEnergy(kcal);
        program.getSegment(0).difficulty.set(difficulty);

        return program;
    }

    public static Program meters(String name, int meters, Difficulty difficulty) {
        Program program = new Program(name);

        program.getSegment(0).setDistance(meters);
        program.getSegment(0).difficulty.set(difficulty);

        return program;
    }

    public static Program strokes(String name, int count, Difficulty difficulty) {
        Program program = new Program(name);

        program.getSegment(0).setStrokes(count);
        program.getSegment(0).difficulty.set(difficulty);

        return program;
    }

    public static Program minutes(String name, int minutes, Difficulty difficulty) {
        Program program = new Program(name);

        program.getSegment(0).setDuration(minutes * 60);
        program.getSegment(0).difficulty.set(difficulty);

        return program;
    }

    /**
     * Create a new {@link Workout}, linking with this program if not transient.
     *
     * @return workout
     */
    public Workout newWorkout() {
        return new Workout((Row.getID(this) == Row.TRANSIENT) ? null : this);
    }
}