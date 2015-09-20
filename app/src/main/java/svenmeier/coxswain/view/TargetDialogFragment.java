package svenmeier.coxswain.view;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public class TargetDialogFragment extends AbstractValueFragment {

    public TargetDialogFragment() {

        // duration 01:[00] - 60:[00]
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.target_duration);
            }

            @Override
            public int getCount() {
                return 60;
            }

            @Override
            public String getPattern() {
                return getString(R.string.duration_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1) * 60;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.duration.get() / 60 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setDuration(getValue(index));
            }
        });

        // distance 001[00] - 100[00]
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.target_distance);
            }

            @Override
            public int getCount() {
                return 100;
            }

            @Override
            public String getPattern() {
                return getString(R.string.distance_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1) * 100;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.distance.get() / 100 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setDistance(getValue(index));
            }
        });

        // strokes 001[0] - 100[0]
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.target_strokes);
            }

            @Override
            public int getCount() {
                return 100;
            }

            @Override
            public String getPattern() {
                return getString(R.string.strokes_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1) * 10;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.strokes.get() / 10 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setStrokes(getValue(index));
            }
        });
    }

    public String getTitle() {
        return getString(R.string.target);
    }
}