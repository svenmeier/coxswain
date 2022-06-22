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

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;

public class SegmentsData implements SegmentsView.Data {

    private final Program program;

    public SegmentsData(Program program) {
        this.program = program;
    }

    @Override
    public int length() {
        return program == null ? 0 : program.getSegmentsCount();
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
