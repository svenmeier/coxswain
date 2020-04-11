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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import propoid.util.content.Preference;


public class AbstractActivity extends AppCompatActivity {

    protected Preference<Boolean> darkTheme;

    private boolean darkWhenCreated;

    private BroadcastReceiver closePictureInPicture = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (isInPictureInPictureMode()) {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        darkTheme = Preference.getBoolean(this, R.string.preference_theme_dark);
        darkWhenCreated = darkTheme.get();
        if (darkWhenCreated) {
            setTheme(R.style.DarkTheme);
        }

        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if ((this instanceof MainActivity) == false) {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (darkTheme.get() != darkWhenCreated) {
            recreate();
            return;
        }

        darkTheme.listen(new Preference.OnChangeListener() {
            @Override
            public void onChanged() {
                recreate();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            String action = "close_picture_in_picture";

            LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);
            broadcasts.registerReceiver(closePictureInPicture, new IntentFilter(action));
            if (!isInPictureInPictureMode()) {
                broadcasts.sendBroadcast(new Intent(action));
            }
        }
    }

    @Override
    protected void onStop() {
        darkTheme.listen(null);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(this);

            broadcasts.unregisterReceiver(closePictureInPicture);
        }

        super.onStop();
    }
}