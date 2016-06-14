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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Match;
import propoid.db.Order;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.Where;
import propoid.db.aspect.Row;
import propoid.db.cascading.DefaultCascading;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

import static propoid.db.Where.all;
import static propoid.db.Where.greaterEqual;
import static propoid.db.Where.lessThan;

public class Gym {

    private static Gym instance;

    private Context context;

    private Repository repository;

    public Program program;

    public Workout workout;

    public Snapshot snapshot;

    public Current current;

    private List<Listener> listeners = new ArrayList<>();

    private Gym(Context context) {

        this.context = context;

        repository = new Repository(context, "gym");

        ((DefaultCascading) repository.cascading).setCascaded(new Program().segments);

        Workout workout = new Workout();
        repository.index(workout, false, Order.descending(workout.start));
    }

    public void defaults() {
        Match<Program> query = repository.query(new Program());
        if (query.count() > 0) {
            return;
        }

        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 500), 500, Difficulty.EASY));
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 1000), 1000, Difficulty.EASY));
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 2000), 2000, Difficulty.MEDIUM));

        repository.insert(Program.calories(String.format(context.getString(R.string.energy_calories), 200), 200, Difficulty.MEDIUM));

        repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 5), 5, Difficulty.EASY));
        repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 10), 10, Difficulty.MEDIUM));

        repository.insert(Program.strokes(String.format(context.getString(R.string.strokes_count), 500), 500, Difficulty.MEDIUM));

        Program program = new Program(context.getString(R.string.segments));
        program.getSegment(0).setDistance(1000);
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        repository.insert(program);
    }

    public boolean hasListener(Class<?> clazz) {
        for (Listener listener : listeners) {
            if (clazz.isInstance(listener)) {
                return true;
            }
        }
        return false;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Match<Program> getPrograms() {
        return repository.query(new Program());
    }

    public Program getProgram(Reference<Program> reference) {
        return repository.lookup(reference);
    }

    public Workout getWorkout(Reference<Workout> reference) {
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

    public Match<Workout> getWorkouts(long from, long to) {
        Workout propotype = new Workout();

        return repository.query(propotype, all(greaterEqual(propotype.start, from), lessThan(propotype.start, to)));
    }

    public void deleteWorkout(Workout workout) {
        repository.delete(workout);
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void select(Program program) {
        this.program = program;

        this.snapshot = null;
        this.workout = null;
        this.current = null;

        fireChanged();
    }

    public boolean isSelected(Program program) {
        return this.program != null && Row.getID(this.program) == Row.getID(program);
    }

    public Event addSnapshot(Snapshot snapshot) {
        Event event = Event.REJECTED;

        this.snapshot = snapshot;

        if (program != null) {
            // program is selected

            if (snapshot.distance.get() > 0 || snapshot.strokes.get() > 0) {
                event = Event.SNAPPED;

                if (workout == null) {
                    event = Event.PROGRAM_START;
                    workout = new Workout(program);
                    workout.location.set(getLocation());

                    current = new Current(program.getSegment(0), 0, new Snapshot());
                }

                if (workout.onSnapshot(snapshot)) {
                    mergeWorkout(workout);

                    snapshot.workout.set(workout);
                    repository.insert(snapshot);
                }

                if (current != null && current.completion() == 1.0f) {
                    Segment next = program.getNextSegment(current.segment);
                    if (next == null) {
                        mergeWorkout(workout);

                        current = null;

                        event = Event.PROGRAM_FINISHED;
                    } else {
                        current = new Current(next, workout.duration.get(), snapshot);

                        event = Event.SEGMENT_CHANGED;
                    }
                }
            }
        }

        fireChanged();

        return event;
    }

    public Snapshot getLastSnapshot() {
        return snapshot;
    }

    public Match<Snapshot> getSnapshots(Workout workout) {
        Snapshot prototype = new Snapshot();

        return repository.query(prototype, Where.equal(prototype.workout, workout));
    }

    public Location getLocation() {
        Location bestLocation = null;

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            for (String provider : locationManager.getProviders(true)) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }

                if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = location;
                }
            }
        } catch (SecurityException ex) {
        }

        return bestLocation;
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
            if (lastSnapshot == null) {
                return 0;
            }

            return achieved(lastSnapshot, lastDuration) - achieved(snapshot, duration);
        }

        private int achieved(Snapshot snapshot, int duration) {
            if (segment.distance.get() > 0) {
                return snapshot.distance.get();
            } else if (segment.strokes.get() > 0) {
                return snapshot.strokes.get();
            } else if (segment.energy.get() > 0) {
                return snapshot.energy.get();
            } else if (segment.duration.get() > 0){
                return duration;
            }
            return 0;
        }

        public boolean inLimit() {
            Snapshot lastSnapshot = getLastSnapshot();

            if (lastSnapshot.speed.get() < current.segment.speed.get()) {
                return false;
            } else if (lastSnapshot.pulse.get() < current.segment.pulse.get()) {
                return false;
            } else if (lastSnapshot.strokeRate.get() < current.segment.strokeRate.get()) {
                return false;
            }

            return true;
        }

        public String describeTarget() {
            String target = "";

            if (segment.distance.get() > 0) {
                target = String.format(context.getString(R.string.distance_meters), segment.distance.get());
            } else if (segment.strokes.get() > 0) {
                target = String.format(context.getString(R.string.strokes_count), segment.strokes.get());
            } else if (segment.energy.get() > 0) {
                target = String.format(context.getString(R.string.energy_calories), segment.energy.get());
            } else if (segment.duration.get() > 0) {
                target = String.format(context.getString(R.string.duration_minutes), Math.round(segment.duration.get() / 60f));
            }
            return target;
        }

        public String describeLimit() {
            String limit = "";

            if (segment.strokeRate.get() > 0) {
                limit = String.format(context.getString(R.string.strokeRate_strokesPerMinute), segment.strokeRate.get());
            } else if (segment.speed.get() > 0) {
                limit = String.format(context.getString(R.string.speed_metersPerSecond), Math.round(segment.speed.get() / 100f));
            } else if (segment.pulse.get() > 0){
                 limit = String.format(context.getString(R.string.pulse_beatsPerMinute), segment.pulse.get());
            }

            return limit;
        }

        public String describe() {
            StringBuilder description = new StringBuilder();

            description.append(describeTarget());

            String limit = describeLimit();
            if (limit.isEmpty() == false) {
                description.append(", ");
                description.append(limit);
            }

            return description.toString();
        }
    }

    private void fireChanged() {
        for (Listener listener : listeners) {
            listener.changed();
        }
    }

    public static Gym instance(Context context) {
        if (instance == null) {
            instance = new Gym(context.getApplicationContext());
        }

        return instance;
    }

    public interface Listener {
        void changed();
    }
}