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
package svenmeier.coxswain.rower;

import svenmeier.coxswain.gym.Measurement;

/**
 * Calculator.
 */
public class Adjuster extends Measurement {

    private final Measurement measurement;

    public Adjuster(Measurement measurement) {
        this.measurement = measurement;
    }

    @Override
    public void reset() {
        measurement.reset();
    }

    @Override
    public boolean anyTargetValue() {
        return measurement.anyTargetValue();
    }

    @Override
    public int getDuration() {
        return measurement.getDuration();
    }

    @Override
    public void setDuration(int duration) {
        measurement.setDuration(duration);
    }

    @Override
    public int getDistance() {
        return measurement.getDistance();
    }

    @Override
    public void setDistance(int distance) {
        measurement.setDistance(distance);
    }

    @Override
    public int getStrokes() {
        return measurement.getStrokes();
    }

    @Override
    public void setStrokes(int strokes) {
        measurement.setStrokes(strokes);
    }

    @Override
    public int getEnergy() {
        return measurement.getEnergy();
    }

    @Override
    public void setEnergy(int energy) {
        measurement.setEnergy(energy);
    }

    @Override
    public int getSpeed() {
        return measurement.getSpeed();
    }

    @Override
    public void setSpeed(int speed) {
        measurement.setSpeed(speed);
    }

    @Override
    public int getPulse() {
        return measurement.getPulse();
    }

    @Override
    public void setPulse(int pulse) {
        measurement.setPulse(pulse);
    }

    @Override
    public int getStrokeRate() {
        return measurement.getStrokeRate();
    }

    @Override
    public void setStrokeRate(int strokeRate) {
        measurement.setStrokeRate(strokeRate);
    }

    @Override
    public int getPower() {
        return measurement.getPower();
    }

    @Override
    public void setPower(int power) {
        measurement.setPower(power);
    }

    @Override
    public int getStrokeRatio() {
        return measurement.getStrokeRatio();
    }

    @Override
    public void setStrokeRatio(int strokeRatio) {
        measurement.setStrokeRatio(strokeRatio);
    }
}
