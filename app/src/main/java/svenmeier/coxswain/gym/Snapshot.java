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
package svenmeier.coxswain.gym;

/**
 */
public class Snapshot {

    public boolean pull;

    public short distance;

    public short strokes;

    public short speed;

    public short pulse;

    public short strokeRate;

    public Snapshot() {
    }

    public Snapshot(Snapshot snapshot) {
        this.distance = snapshot.distance;
        this.strokes = snapshot.strokes;
        this.speed = snapshot.speed;
        this.pulse = snapshot.pulse;
        this.strokeRate = snapshot.strokeRate;
    }

    public void clear() {
        this.distance = 0;
        this.strokes = 0;
        this.speed = 0;
        this.pulse = 0;
        this.strokeRate = 0;
    }
}
