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
 * Measurement of rowing values.
 */
public class Measurement {

    /**
     * seconds
     */
    public int duration;

    /**
     * meters
     */
    public int distance;

    public int strokes;

    /**
     * kilo calories
     */
    public int energy;

    /**
     * centimeters per seconds
     */
    public int speed;

    /**
     * beats per second
     */
    public int pulse;

    /**
     * strokes per minute
     */
    public int strokeRate;

    /**
     * watts
     */
    public int power;

    public int strokeRatio;

    public Measurement() {
    }

    public Measurement(Measurement measurement) {
        this.duration = measurement.duration;
        this.distance = measurement.distance;
        this.strokes = measurement.strokes;
        this.energy = measurement.energy;
        this.speed = measurement.speed;
        this.pulse = measurement.pulse;
        this.strokeRate = measurement.strokeRate;
        this.power = measurement.power;
        this.strokeRatio = measurement.strokeRatio;
    }
}