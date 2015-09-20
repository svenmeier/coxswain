package svenmeier.coxswain.view;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;

public class SegmentsData implements SegmentsView.Data {

    private final Program program;

    public SegmentsData(Program program) {
        this.program = program;
    }

    @Override
    public int length() {
        return program.getSegmentsCount();
    }

    @Override
    public float value(int index) {
        return program.getSegment(index).asDuration();
    }

    @Override
    public float total() {
        float value = 0;
        for (Segment segment : program.segments.get()) {
            value += segment.asDuration();
        }
        return value;
    }

    @Override
    public int level(int index) {
        return program.getSegment(index).difficulty.get().ordinal();
    }
}
