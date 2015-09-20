package svenmeier.coxswain.gym;

import java.util.ArrayList;
import java.util.List;

import propoid.core.Property;
import propoid.core.Propoid;

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
}
