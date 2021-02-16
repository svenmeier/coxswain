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

import android.content.Context;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;

import static java.util.concurrent.TimeUnit.SECONDS;

    /**
 */
public class Duration {

    private int seconds;

    public String formatted() {
        return String.format("%d:%02d:%02d", SECONDS.toHours(seconds), SECONDS.toMinutes(seconds) % 60, seconds % 60);
    }

    public static Duration seconds(Context context, int seconds) {
        Duration duration = new Duration();

        duration.seconds = seconds;

        return duration;
    }
}