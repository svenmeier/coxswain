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
package svenmeier.coxswain;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import propoid.util.content.Preference;


public class AbstractActivity extends Activity {

    protected Preference<Boolean> theme;

    private boolean darkWhenCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        theme = Preference.getBoolean(this, R.string.preference_theme_dark);

        darkWhenCreated = theme.get();
        if (darkWhenCreated) {
            setTheme(R.style.DarkTheme);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (theme.get() != darkWhenCreated) {
            recreate();
        } else {
            theme.listen(new Preference.OnChangeListener() {
                @Override
                public void onChanged() {
                    recreate();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        theme.listen(null);

        super.onStop();
    }
}
