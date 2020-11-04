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
package svenmeier.coxswain.rower.wired;

import java.util.LinkedList;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.ITrace;

public class PowerCalculator {

    private final ITrace trace;
            ;
    private LinkedList<Integer> powerHistory = new LinkedList<>();
    
    private int maxPower;

    public PowerCalculator(ITrace trace) {
        this.trace = trace;
    }

    public void power(int power) {
        // waterrower might report different values during single stroke
        maxPower = Math.max(maxPower, power);
    }

    public void strokeStart(Measurement measurement, long when) {
        // stroke has finished
        if (maxPower > 0) {
            addHistory(maxPower);
            maxPower = 0;
        }

        int meanPower = getHistoryMean();
        measurement.setPower(meanPower);
        trace.comment("power mean of " + powerHistory + " + is " + meanPower);
    }

    private int getHistoryMean() {
        if (powerHistory.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for (int i = 0; i < powerHistory.size(); i++) {
            sum += powerHistory.get(i);
        }

        return sum / powerHistory.size();
    }

    private void addHistory(int power) {
        powerHistory.addLast(power);

        if (powerHistory.size() > 6) {
            powerHistory.removeFirst();
        }
    }
}
