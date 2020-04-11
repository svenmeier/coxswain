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
import androidx.annotation.UiThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import propoid.core.Propoid;
import propoid.db.LookupException;
import propoid.db.Match;
import propoid.db.Order;
import propoid.db.Range;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.Transaction;
import propoid.db.Where;
import propoid.db.aspect.Row;
import propoid.db.cascading.DefaultCascading;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.io.Export;

import static propoid.db.Where.all;
import static propoid.db.Where.equal;
import static propoid.db.Where.greaterEqual;
import static propoid.db.Where.lessThan;

public class Gym {

    private static Gym instance;

    private Context context;

    private Preference<Boolean> external;

    private Repository repository;

    private List<Listener> listeners = new ArrayList<>();

    /**
     * The last measurement.
     */
    private Measurement measurement = new Measurement();

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
     * Progress of current workout.
     */
    public Progress progress;

    private Gym(final Context context) {

        this.context = context;

        repository = new Repository(context, new GymLocator(context), new GymVersioning());

        external = Preference.getBoolean(context, R.string.preference_data_external);
        external.listen(new Preference.OnChangeListener() {
            @Override
            public void onChanged() {
                repository.close();

                repository.open();

                fireChanged(null);
            }
        });
    }

    void initialize() {
        // programs cascade to their segments
        ((DefaultCascading) repository.cascading).setCascaded(new Program().segments);

        // index workout by start
        Workout workoutIndex = new Workout();
        repository.index(workoutIndex, false, Order.descending(workoutIndex.start));

        // index snapshots by workout
        Snapshot snapshotIndex = new Snapshot();
        repository.index(snapshotIndex, false, Order.ascending(snapshotIndex.workout));
        
        Match<Program> query = repository.query(new Program());
        if (query.count() == 0) {
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 500), 500, Difficulty.EASY));
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 1000), 1000, Difficulty.EASY));
            repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 2000), 2000, Difficulty.MEDIUM));

            repository.insert(Program.kilocalories(String.format(context.getString(R.string.energy_kilocalories), 200), 200, Difficulty.MEDIUM));

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
    }

    /**
     * Compact workouts.
     *
     * @param count maximum count of workouts to compact
     */
    public void compact(int count) {
        int days = Preference.getInt(context, R.string.preference_compact).fallback(180).get();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -days);
        Workout workout = new Workout();
        Snapshot snapshot = new Snapshot();
        Where where =
                Where.all(
                    Where.lessEqual(workout.start, calendar.getTimeInMillis()),
                        Where.is(snapshot.workout, Where.any())
                );
        for (Workout compact : repository.query(workout, where).list(Range.limit(count), Order.ascending(workout.start))) {
            repository.query(snapshot, Where.equal(snapshot.workout, compact)).delete();
        }

        repository.vacuum();
    }

    /**
     * Add a listener - has to be called on main thread.
     * 
     * @param listener listener of changes in the gym
     */
    @UiThread
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

                // imported workouts are not evaluated by default
                workout.evaluate.set(false);

                workout.program.set(repository.query(example, equal(example.name, programName)).first());
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

    public void mergeSegment(Segment segment) {
        repository.merge(segment);
    }

    public Match<Workout> getWorkouts() {
        Workout prototype = new Workout();

        if (program == null) {
            return repository.query(prototype);
        } else if (Row.getID(program) == Row.TRANSIENT) {
            return repository.query(prototype, Where.none());
        } else {
            return repository.query(prototype, Where.equal(prototype.program, program));
        }
    }

    public Match<Workout> getWorkouts(long from, long to) {
        Workout prototype = new Workout();

        // evaluated workouts only
        if (program == null) {
            return repository.query(prototype, all(
                    equal(prototype.evaluate, true),
                    greaterEqual(prototype.start, from),
                    lessThan(prototype.start, to))
            );
        } else if (Row.getID(program) == Row.TRANSIENT) {
            return repository.query(prototype, Where.none());
        } else  {
            return repository.query(prototype, all(
                    equal(prototype.program, program),
                    equal(prototype.evaluate, true),
                    greaterEqual(prototype.start, from),
                    lessThan(prototype.start, to))
            );
        }
    }

    public void delete(Propoid propoid) {
        if (propoid instanceof Workout) {
            // delete all snapshots of workout
            Snapshot prototype = new Snapshot();
            repository.query(prototype, equal(prototype.workout, (Workout) propoid)).delete();
        }

        repository.delete(propoid);

        if (propoid instanceof Program) {
            // keep one program at least
            if (repository.query(propoid).count() == 0) {
                newProgram();
            }
        }
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void deselect() {
        if (current != null) {
            Export.start(context, current);
        }

        if (program != null) {
            this.pace = null;
            this.program = null;

            this.measurement = new Measurement();
            this.current = null;
            this.progress = null;

            fireChanged(null);
        }
    }

    public void select(Program program) {
        this.pace = null;
        this.program = program;

        this.measurement = new Measurement();
        this.current = null;
        this.progress = null;

        fireChanged(null);
    }

    public void repeat(Workout pace) {
        Program program;
        try {
            program = pace.program.get();
        } catch (LookupException programAlreadyDeleted) {
            // fall back to challenge
            challenge(pace);
            return;
        }

        this.pace = pace;
        this.program = program;

        this.measurement = new Measurement();
        this.current = null;
        this.progress = null;

        fireChanged(null);
    }

    public void challenge(Workout pace) {
        this.pace = pace;
        this.program = Program.meters(context.getString(R.string.action_challenge), pace.distance.get(), Difficulty.NONE);

        this.measurement = new Measurement();
        this.current = null;
        this.progress = null;

        fireChanged(null);
    }

	/**
     * A new measurement.
     *
     * @param measurement the measurement
     */
    public Event onMeasured(Measurement measurement) {
        Event event = Event.ACKNOWLEDGED;

        this.measurement = measurement;

        if (program != null) {
            // program is selected

            if (measurement.hasTarget()) {
                // delay workout creation

                event = analyse(measurement);
            }
        }

        fireChanged(measurement);

        return event;
    }

    private Event analyse(Measurement measurement) {
        Event event = Event.ACKNOWLEDGED;

        if (current == null) {
            current = program.newWorkout();
            current.location.set(getLocation());
            mergeWorkout(current);

            progress = new Progress(program.getSegment(0), new Measurement());

            event = Event.PROGRAM_START;
        }

        int seconds = current.duration.get();
        try {
            current.onMeasured(measurement);
        } catch (IllegalArgumentException ex) {
            Log.d(Coxswain.TAG, "illegal measurement " + ex.getMessage());

            return Event.REJECTED;
        }

        seconds = (current.duration.get() - seconds);
        if (seconds > 0) {
            mergeWorkout(current);

            // limit snapshots so this does not take forever
            for (seconds = Math.min(seconds, 10); seconds > 0; seconds--) {
                Snapshot snapshot = new Snapshot(measurement);
                snapshot.workout.set(current);
                repository.insert(snapshot);
            }
        }

        if (progress != null && progress.completion() == 1.0f) {
            Segment next = program.getNextSegment(progress.segment);
            if (next == null) {
                mergeWorkout(current);

                progress = null;

                event = Event.PROGRAM_FINISHED;
            } else {
                progress = new Progress(next, measurement);

                event = Event.SEGMENT_CHANGED;
            }
        }

        return event;
    }

    public Match<Snapshot> getSnapshots(Workout workout) {
        Snapshot prototype = new Snapshot();

        return repository.query(prototype, equal(prototype.workout, workout));
    }

    public Location getLocation() {
        Location bestLocation = null;

        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			if (manager != null) {
				for (String provider : manager.getProviders(true)) {
					Location location = manager.getLastKnownLocation(provider);
					if (location == null) {
						continue;
					}

					if (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy()) {
						bestLocation = location;
					}
				}
			}
        } catch (SecurityException ignored) {
        }

        return bestLocation;
    }

    public Program newProgram() {
        Program program = new Program(context.getString(R.string.program_name_new));
        mergeProgram(program);
        return program;
    }

    public Program duplicateProgram(final Program original) {

        final Program duplicate = new Program(context.getString(R.string.program_name_new));

        repository.transactional(new Transaction() {
            @Override
            public void doTransactional() {
                duplicate.getSegments().clear();

                for (Segment segment : original.getSegments()) {
                    duplicate.addSegment(segment.duplicate());
                }

                repository.merge(duplicate);
            }
        });
        return duplicate;
    }

    public class Progress {

        public final Segment segment;

        /**
         * Measurement of start of segment
         */
        private final Measurement startMeasurement;

        Progress(Segment segment, Measurement measurement) {
            this.segment = segment;

            this.startMeasurement = new Measurement(measurement);
        }

        public float completion() {
            float achieved = achieved();
            float target = segment.getTarget();

            return Math.min(achieved / target, 1.0f);
        }

        public int achieved() {
            return achieved(measurement) - achieved(startMeasurement);
        }

        private int achieved(Measurement measurement) {
            if (segment.distance.get() > 0) {
                return measurement.getDistance();
            } else if (segment.strokes.get() > 0) {
                return measurement.getStrokes();
            } else if (segment.energy.get() > 0) {
                return measurement.getEnergy();
            } else if (segment.duration.get() > 0){
                return measurement.getDuration();
            }
            return 0;
        }

        public boolean inLimit() {
            if (measurement.getSpeed() < progress.segment.speed.get()) {
                return false;
            } else if (measurement.getPulse() < progress.segment.pulse.get()) {
                return false;
            } else if (measurement.getStrokeRate() < progress.segment.strokeRate.get()) {
                return false;
			} else if (measurement.getPower() < progress.segment.power.get()) {
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
                target = String.format(context.getString(R.string.energy_kilocalories), segment.energy.get());
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
                limit = String.format(context.getString(R.string.speed_metersPerSecond), segment.speed.get() / 100f);
            } else if (segment.pulse.get() > 0){
                 limit = String.format(context.getString(R.string.pulse_beatsPerMinute), segment.pulse.get());
            } else if (segment.power.get() > 0){
                limit = String.format(context.getString(R.string.power_watts), segment.power.get());
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

    private void fireChanged(Object scope) {
        for (Listener listener : listeners) {
            listener.changed(scope);
        }
    }

    /**
     * Get the singelton Gym - has to be called on the main thread.
     */
    @UiThread
    public synchronized static Gym instance(Context context) {
        if (instance == null) {
            instance = new Gym(context.getApplicationContext());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    instance.initialize();
                }
            }).start();
        }

        return instance;
    }

    public interface Listener {
        void changed(Object scope);
    }
}
