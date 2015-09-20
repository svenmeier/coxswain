package svenmeier.coxswain.gym;

import java.util.ArrayList;
import java.util.List;

import propoid.core.Property;
import propoid.core.Propoid;

/**
 */
public class Workout extends Propoid {

    public final Property<String> name = property();

    public final Property<Long> start = property();

    public final Property<Integer> duration = property();

    public final Property<Integer> distance = property();

    public final Property<Integer> strokes = property();

    public Workout() {
    }

    public Workout(Program program) {
        this.name.set(program.name.get());
        this.start.set(System.currentTimeMillis());
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(0);
    }

    public void onSnapshot(Snapshot snapshot) {
        this.duration.set(this.duration.get() + 1);
        this.distance.set((int)snapshot.distance);
        this.strokes.set((int)snapshot.strokes);
    }
}