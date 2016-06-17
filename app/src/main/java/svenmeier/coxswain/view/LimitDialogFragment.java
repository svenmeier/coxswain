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

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public class LimitDialogFragment extends AbstractValueFragment {

    public LimitDialogFragment() {

        // speed 0.1[0] - 5.0[0]
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.speed_label);
            }

            @Override
            public int getCount() {
                return 50;
            }

            @Override
            public String getPattern() {
                return getString(R.string.speed_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1) * 10;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.speed.get() / 10 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setSpeed(getValue(index));
            }
        });

        // pulse 001 - 200
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.pulse_label);
            }

            @Override
            public int getCount() {
                return 200;
            }

            @Override
            public String getPattern() {
                return getString(R.string.pulse_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1);
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.pulse.get() / 1 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setPulse(getValue(index));
            }
        });

        // strokerate 01 - 60
        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.strokeRate_label);
            }

            @Override
            public int getCount() {
                return 60;
            }

            @Override
            public String getPattern() {
                return getString(R.string.strokeRate_pattern);
            }

            @Override
            public int getValue(int index) {
                return (index + 1);
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return segment.strokeRate.get() / 1 - 1;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.setStrokeRate(getValue(index));
            }
       });

        addTab(new Tab() {
            @Override
            public CharSequence getTitle() {
                return getString(R.string.none_label);
            }

            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public String getPattern() {
                return "";
            }

            @Override
            public int getValue(int index) {
                return 0;
            }

            @Override
            public int segmentToIndex(Segment segment) {
                return (segment.getLimit() > 0) ? -1 : 0;
            }

            @Override
            public void indexToSegment(Segment segment, int index) {
                segment.clearLimit();
            }
        });
    }
}