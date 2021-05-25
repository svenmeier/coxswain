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
    private int duration;

    /**
     * meters
     */
    private int distance;

    private int strokes;

    /**
     * kilo calories
     */
    private int energy;

    /**
     * centimeters per seconds
     */
    private int speed;

    /**
     * beats per second
     */
    private int pulse;

    /**
     * strokes per minute
     */
    private int strokeRate;

    /**
     * watts
     */
    private int power;

    private int strokeRatio;

    public Measurement() {
    }

    public Measurement(Measurement measurement) {
        this.duration = measurement.getDuration();
        this.distance = measurement.getDistance();
        this.strokes = measurement.getStrokes();
        this.energy = measurement.getEnergy();
        this.speed = measurement.getSpeed();
        this.pulse = measurement.getPulse();
        this.strokeRate = measurement.getStrokeRate();
        this.power = measurement.getPower();
        this.strokeRatio = measurement.getStrokeRatio();
    }

    public void reset() {
        duration = 0;
        distance = 0;
        strokes = 0;
        energy = 0;

        speed = 0;
        pulse = 0;
        strokeRate = 0;
        power = 0;

        strokeRatio = 0;
    }

    public boolean anyTargetValue() {
        return distance + duration + strokes + energy > 0;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getStrokes() {
        return strokes;
    }

    public void setStrokes(int strokes) {
        this.strokes = strokes;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getPulse() {
        return pulse;
    }

    public void setPulse(int pulse) {
        this.pulse = pulse;
    }

    public int getStrokeRate() {
        return strokeRate;
    }

    public void setStrokeRate(int strokeRate) {
        this.strokeRate = strokeRate;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getStrokeRatio() {
        return strokeRatio;
    }

    public void setStrokeRatio(int strokeRatio) {
        this.strokeRatio = strokeRatio;
    }
}