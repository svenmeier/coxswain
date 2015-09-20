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

    public final Property<Integer> speed = property();

    public final Property<Integer> strokeRate = property();

    public final Property<Integer> pulse = property();

    public Segment() {
    }

    public Segment(Difficulty difficulty) {
        this.difficulty.set(difficulty);
        distance.set(1000);
        duration.set(0);
        strokes.set(0);
        speed.set(0);
        strokeRate.set(0);
        pulse.set(0);
    }

    public int asDuration() {
        if (distance.get() > 0) {
            return distance.get() / 4;
        } else if (strokes.get() > 0) {
            return strokes.get();
        } else {
            return duration.get();
        }
    }

    public int getTarget() {
        if (distance.get() > 0) {
            return distance.get();
        } else if (strokes.get() > 0) {
            return strokes.get();
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
    }

    public void setDuration(int duration) {
        this.duration.set(duration);
        this.distance.set(0);
        this.strokes.set(0);
    }

    public void setDistance(int distance) {
        this.duration.set(0);
        this.distance.set(distance);
        this.strokes.set(0);
    }

    public void setStrokes(int strokes) {
        this.duration.set(0);
        this.distance.set(0);
        this.strokes.set(strokes);
    }

    public void setSpeed(int speed) {
        this.speed.set(speed);
        this.strokeRate.set(0);
        this.pulse.set(0);
    }

    public void setStrokeRate(int strokeRate) {
        this.speed.set(0);
        this.strokeRate.set(strokeRate);
        this.pulse.set(0);
    }

    public void setPulse(int pulse) {
        this.speed.set(0);
        this.strokeRate.set(0);
        this.pulse.set(pulse);
    }
}