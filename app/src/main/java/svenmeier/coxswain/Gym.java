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
import android.location.Location;
import android.location.LocationManager;

import java.util.ArrayList;
import java.util.List;

import propoid.core.Propoid;
import propoid.db.LookupException;
import propoid.db.Match;
import propoid.db.Order;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.Transaction;
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
import static propoid.db.Where.unequal;

public class Gym {

    private static Gym instance;

    private Context context;

    private Repository repository;

    private List<Listener> listeners = new ArrayList<>();

    /**
     * The selected program.
     */
    public Program program;

	/**
     * Optional pace workout.
     */
    public Workout pace;

	/**
     * The current workout.
     */
    public Workout current;

    /**
     * The last snapshot.
     */
    public Snapshot snapshot;

	/**
     * Progress of current workout.
     */
    public Progress progress;

    private Gym(Context context) {

        this.context = context;

        repository = new Repository(context, "gym");

        ((DefaultCascading) repository.cascading).setCascaded(new Program().segments);

        Workout workout = new Workout();
        repository.index(workout, false, Order.descending(workout.start));
        Snapshot snapshot = new Snapshot();
        repository.index(snapshot, false, Order.ascending(snapshot.workout));
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

        Program program = new Program(context.getString(R.string.program_name_segments));
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

    public <P extends Propoid> P get(Reference<P> reference) {
        return repository.lookup(reference);
    }

    public void add(final String programName, final Workout workout, final List<Snapshot> snapshots) {

        repository.transactional(new Transaction() {
            @Override
            public void doTransactional() {
                Program example = new Program();

                workout.program.set(repository.query(example, Where.equal(example.name, programName)).first());
                repository.merge(workout);

                for (Snapshot snapshot : snapshots) {
                    snapshot.workout.set(workout);

                    repository.merge(snapshot);
                }
            }
        });
    }

    public void mergeProgram(Program program) {
        repository.merge(program);
    }

    public Match<Workout> getWorkouts() {
        return repository.query(new Workout());
    }

    public Match<Workout> getWorkouts(long from, long to) {
        Workout propotype = new Workout();

        return repository.query(propotype, all(
                unequal(propotype.program, null),
                greaterEqual(propotype.start, from),
                lessThan(propotype.start, to))
        );
    }

    public void delete(Propoid propoid) {
        if (propoid instanceof Workout) {
            Snapshot prototype = new Snapshot();
            repository.query(prototype, Where.equal(prototype.workout, (Workout) propoid)).delete();
        }

        repository.delete(propoid);
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void deselect() {
        this.pace = null;
        this.program = null;

        this.snapshot = null;
        this.current = null;
        this.progress = null;

        fireChanged();
    }

    public void repeat(Program program) {
        this.pace = null;
        this.program = program;

        this.snapshot = null;
        this.current = null;
        this.progress = null;

        fireChanged();
    }

    public void repeat(Workout pace) {
        Program program = null;
        try {
            program = pace.program.get();
        } catch (LookupException programAlreadyDeleted) {
            // fall back to challenge
            challenge(pace);
            return;
        }

        this.pace = pace;
        this.program = program;

        this.snapshot = null;
        this.current = null;
        this.progress = null;

        fireChanged();
    }

    public void challenge(Workout pace) {
        this.pace = pace;
        this.program = Program.meters(context.getString(R.string.action_challenge), pace.distance.get(), Difficulty.NONE);

        this.snapshot = null;
        this.current = null;
        this.progress = null;

        fireChanged();
    }

    public Event addSnapshot(Snapshot snapshot) {
        Event event = Event.REJECTED;

        this.snapshot = snapshot;

        if (program != null) {
            // program is selected

            if (snapshot.distance.get() > 0 || snapshot.strokes.get() > 0) {
                event = Event.SNAPPED;

                if (current == null) {
                    event = Event.PROGRAM_START;
                    current = program.newWorkout();
                    current.location.set(getLocation());

                    progress = new Progress(program.getSegment(0), 0, new Snapshot());
                }

                if (current.onSnapshot(snapshot)) {
                    mergeWorkout(current);

                    snapshot.workout.set(current);
                    repository.insert(snapshot);
                }

                if (progress != null && progress.completion() == 1.0f) {
                    Segment next = program.getNextSegment(progress.segment);
                    if (next == null) {
                        mergeWorkout(current);

                        progress = null;

                        event = Event.PROGRAM_FINISHED;
                    } else {
                        progress = new Progress(next, current.duration.get(), snapshot);

                        event = Event.SEGMENT_CHANGED;
                    }
                }
            }
        }

        fireChanged();

        return event;
    }

    public Match<Snapshot> getSnapshots(Workout workout) {
        Snapshot prototype = new Snapshot();

        return repository.query(prototype, Where.equal(prototype.workout, workout));
    }

    public Location getLocation() {
        Location bestLocation = null;

        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            for (String provider : manager.getProviders(true)) {
                Location location = manager.getLastKnownLocation(provider);
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

    public class Progress {

        public final Segment segment;

        final int duration;

		/**
         * Start snapshot of the segment.
         */
        final Snapshot start;

        public Progress(Segment segment, int duration, Snapshot snapshot) {
            this.segment = segment;
            this.duration = duration;
            this.start = snapshot;
        }

        public float completion() {
            float achieved = achieved();
            float target = segment.getTarget();

            return Math.min(achieved / target, 1.0f);
        }

        public int achieved() {
            int lastDuration = current.duration.get();
            if (snapshot == null) {
                return 0;
            }

            return achieved(snapshot, lastDuration) - achieved(start, duration);
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
            if (snapshot.speed.get() < progress.segment.speed.get()) {
                return false;
            } else if (snapshot.pulse.get() < progress.segment.pulse.get()) {
                return false;
            } else if (snapshot.strokeRate.get() < progress.segment.strokeRate.get()) {
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