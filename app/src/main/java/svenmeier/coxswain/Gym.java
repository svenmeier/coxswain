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
package svenmeier.coxswain;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Match;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.aspect.Row;
import propoid.db.cascading.DefaultCascading;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

public class Gym {

    private static Gym instance;

    private Context context;

    private Repository repository;

    public Program program;

    public Workout workout;

    public List<Snapshot> snapshots = new ArrayList<>();

    public Current current;

    private Gym(Context context) {

        this.context = context;

        repository = new Repository(context, "gym");
        ((DefaultCascading)repository.cascading).setCascaded(new Program().segments);

        Match<Program> query = repository.query(new Program());
        if (query.first() == null) {
            repository.insert(new Program("Beginner"));
        }

        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
    }

    public Match<Program> getPrograms() {
        return  repository.query(new Program());
    }

    public Program getProgram(Reference<Program> reference) {
        return repository.lookup(reference);
    }

    public void mergeProgram(Program program) {
        repository.merge(program);
    }

    public void deleteProgram(Program program) {
        repository.delete(program);
    }

    public Match<Workout> getWorkouts() {
        return repository.query(new Workout());
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void select(Program program) {
        this.program = program;

        snapshots.clear();

        if (program == null) {
            workout = null;

            current = null;
        } else {
            workout = new Workout(program);

            Snapshot first = new Snapshot();
            snapshots.add(first);
            current = new Current(program.getSegment(0), 0, first);
        }
    }

    public boolean isSelected(Program program) {
        return this.program != null && Row.getID(this.program) == Row.getID(program) && current != null;
    }

    public Event addSnapshot(Snapshot snapshot) {
        Event event = Event.REJECTED;

        if (current != null) {
            event = Event.SNAPPED;

            snapshots.add(snapshot);

            if (workout.duration.get() == 0) {
                event = Event.PROGRAM_START;
            }
            workout.onSnapshot(snapshot);

            if (current.completion() == 1.0f) {
                Segment next = program.getNextSegment(current.segment);
                if (next == null) {
                    current = null;


                    event = Event.PROGRAM_FINISHED;
                } else {
                    current = new Current(next, workout.duration.get(), snapshot);

                    event = Event.SEGMENT_CHANGED;
                }
            }
        }

        return event;
    }

    public Snapshot getLastSnapshot() {
        return snapshots.get(snapshots.size() - 1);
    }

    public class Current {

        public final Segment segment;

        final int duration;

        final Snapshot snapshot;

        public Current(Segment segment, int duration, Snapshot snapshot) {
            this.segment = segment;
            this.duration = duration;
            this.snapshot = snapshot;
        }

        public float completion() {
            float achieved = achieved();
            float target = segment.getTarget();

            return Math.min(achieved / target, 1.0f);
        }

        public int achieved() {
            int lastDuration = workout.duration.get();
            Snapshot lastSnapshot = getLastSnapshot();

            return achieved(lastSnapshot, lastDuration) - achieved(current.snapshot, current.duration);
        }

        private int achieved(Snapshot snapshot, int duration) {
            if (segment.distance.get() > 0) {
                return snapshot.distance;
            } else if (segment.strokes.get() > 0) {
                return snapshot.strokes;
            } else if (segment.duration.get() > 0){
                return duration;
            }
            return 0;
        }

        public String describeTarget() {
            StringBuilder description = new StringBuilder();

            if (segment.distance.get() > 0) {
                description.append(String.format(context.getString(R.string.distance_meters), segment.distance.get()));
            } else if (segment.strokes.get() > 0) {
                description.append(String.format(context.getString(R.string.strokes_count), segment.strokes.get()));
            } else if (segment.duration.get() > 0) {
                description.append(String.format(context.getString(R.string.duration_minutes), Math.round(segment.duration.get() / 60f)));
            }
            return description.toString();
        }

        public String describeLimit() {
            StringBuilder description = new StringBuilder();

            if (segment.strokeRate.get() > 0) {
                description.append(String.format(context.getString(R.string.strokeRate_strokesPerMinute), segment.strokeRate.get()));
            } else if (segment.speed.get() > 0) {
                description.append(String.format(context.getString(R.string.speed_metersPerSecond), Math.round(segment.speed.get() / 100f)));
            } else if (segment.pulse.get() > 0){
                description.append(String.format(context.getString(R.string.pulse_beatsPerMinute), segment.pulse.get()));
            }

            return description.toString();
        }
    }

    public static Gym instance(Context context) {
        if (instance == null) {
            instance = new Gym(context.getApplicationContext());
        }

        return instance;
    }

}