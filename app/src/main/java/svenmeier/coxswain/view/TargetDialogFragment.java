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
package svenmeier.coxswain.view;

import propoid.ui.list.ViewsAdapter;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public class TargetDialogFragment extends AbstractValueFragment {

    public TargetDialogFragment() {

        // duration 01:[00] - 60:[00]
        addTab(new Tab() {
            @Override
            public int getCount() {
                return 2 + 60;
            }

            @Override
            public ValueBinding getBinding() {
                return ValueBinding.DURATION;
            }

            @Override
            public int getValue(int index) {
                if (index == 0) {
                    return 30;
                } else if (index == 1) {
                    return 60;
                } else if (index == 2) {
                    return 90;
                } else {
                    return (index - 1) * 60;
                }
            }

            @Override
            public int segmentToIndex(Segment segment) {
                int duration = segment.duration.get();

                if (duration == 0) {
                    return -1;
                } else if (duration <= 30) {
                    return 0;
                } else if (duration <= 60) {
                    return 1;
                } else if (duration <= 90) {
                    return 2;
                } else {
                    return duration / 60 + 1;
                }
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setDuration(getValue(index));
            }
        });

        // distance 001[00] - 100[00]
        addTab(new Tab() {
            @Override
            public int getCount() {
                return 100;
            }

            @Override
            public ValueBinding getBinding() {
                return ValueBinding.DISTANCE;
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
            public int getCount() {
                return 100 + 2;
            }

            @Override
            public ValueBinding getBinding() {
                return ValueBinding.STROKES;
            }

            @Override
            public int getValue(int index) {
                if (index == 0) {
                    return 5;
                } else if (index == 1) {
                    return 10;
                } else if (index == 2) {
                    return 15;
                } else {
                    return (index - 1) * 10;
                }
            }

            @Override
            public int segmentToIndex(Segment segment) {
                int strokes = segment.strokes.get();

                if (strokes == 0) {
                    return -1;
                } else if (strokes <= 5) {
                    return 0;
                } else if (strokes <= 10) {
                    return 1;
                } else if (strokes <= 15) {
                    return 2;
                } else {
                    return strokes / 10 + 1;
                }

            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setStrokes(getValue(index));
            }
        });

        // energy 001[0] - 100[0]
        addTab(new Tab() {
            @Override
            public int getCount() {
                return 100;
            }

            @Override
            public ValueBinding getBinding() {
                return ValueBinding.ENERGY;
            }

            @Override
            public int getValue(int index) {
                return (index + 1) * 10;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.energy.get() / 10 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setEnergy(getValue(index));
            }
        });
    }
}