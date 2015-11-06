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
package svenmeier.coxswain.motivator;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import propoid.util.content.Preference;
import svenmeier.coxswain.Event;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;

/**
 */
public class DefaultMotivator implements Motivator, TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    public static final int LIMIT_LATENCY = 20000;

    private static final String WHISTLE = "[whistle]";

    private static final String TICK = "[tick]";

    private final Context context;

    private Gym gym;

    private final Vibrator vibrator;

    private TextToSpeech speech;

    private AudioManager audio;

    private final Preference<Boolean> whistlePreference;

    private final Preference<Boolean> speakPreference;

    private final Preference<Boolean> ratioTickPreference;

    private final Preference<Float> ratioPreference;

    private boolean initialized;

    private Event pending;

    private Ratio ratio = new Ratio();

    private Limit limit = new Limit();

    public DefaultMotivator(Context context) {
        this.context = context;

        this.gym = Gym.instance(context);

        speech = new TextToSpeech(context, this);

        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);

        whistlePreference = Preference.getBoolean(context, R.string.preference_motivator_whistle);
        speakPreference = Preference.getBoolean(context, R.string.preference_motivator_speak);
        ratioTickPreference = Preference.getBoolean(context, R.string.preference_motivator_ratio_tick);
        ratioPreference = Preference.getFloat(context, R.string.preference_motivator_ratio).range(1f, 3f);
    }

    @Override
    public void onEvent(Event event) {
        if (initialized == false) {
            if (this.pending == null || event != Event.SNAPPED) {
                this.pending = event;
            }
            return;
        }

        Gym.Current current = gym.current;
        switch (event) {
            case PROGRAM_START:
            case SEGMENT_CHANGED:
                if (current != null) {
                    changed(current);
                }
                break;
            case SNAPPED:
                if (current != null) {
                    snapped(current);
                }
                break;
            case PROGRAM_FINISHED:
                for (int i = 0; i < 3; i++) {
                    boolean pause = whistle();
                    pause(pause);
                }
                break;
        }
    }

    private void snapped(Gym.Current current) {
        ratio.analyse(current);

        limit.analyse(current);
    }

    private void changed(Gym.Current current) {
        String target = current.describeTarget();
        String limit = current.describeLimit();

        boolean pause = false;
        int ordinal = current.segment.difficulty.get().ordinal();
        for (int o = 0; o <= ordinal; o++) {
            pause |= whistle();
        }
        pause(pause);
        pause = speak(target);
        pause(pause);
        pause = speak(limit);

        this.limit.reset();
        this.ratio.reset();
    }

    private boolean speak(String text) {
        if (speakPreference.get()) {
            speech.speak(text, TextToSpeech.QUEUE_ADD, null);

            return true;
        }

        return false;
    }

    private void pause() {
        pause(true);
    }

    private void pause(boolean required) {
        if (required) {
            speech.playSilence(500, TextToSpeech.QUEUE_ADD, null);
        }
    }

    private boolean whistle() {
        if (whistlePreference.get()) {
            speech.playEarcon(WHISTLE, TextToSpeech.QUEUE_ADD, null);

            return true;
        }

        return false;
    }

    @Override
    public void destroy() {
        speech.shutdown();
        speech = null;

        audio.abandonAudioFocus(this);
        audio = null;
    }

    /**
     * AudioManager.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
    }

    /**
     * TextToSpeech.
     */
    @Override
    public void onInit(int status) {
        if (status == 0) {
            speech.setLanguage(Locale.getDefault());
            speech.addEarcon(WHISTLE, context.getPackageName(), R.raw.whistle);
            speech.addEarcon(TICK, context.getPackageName(), R.raw.tick);

            initialized = true;

            if (pending != null) {
                onEvent(pending);
                pending = null;
            }
        }
    }

    private class Limit {

        private long underLimitSince = -1;

        public void analyse(Gym.Current current) {
            if (current.inLimit()) {
                underLimitSince = -1;
            } else {
                long now = System.currentTimeMillis();

                if (underLimitSince == -1) {
                    underLimitSince = now;
                } else if ((now - underLimitSince) > LIMIT_LATENCY) {
                    String limit = current.describeLimit();
                    if (limit.isEmpty() == false) {
                        speak(limit);
                    }

                    underLimitSince = now;
                }
            }

        }

        public void reset() {
            underLimitSince = -1;
        }
    }

    private class Ratio {

        private boolean drive;

        private long drawTime = -1;

        private long catchTime = -1;

        private long duration = -1;

        public void analyse(Gym.Current current) {
            long now = System.currentTimeMillis();

            if (gym.snapshot.drive == false && this.drive == true) {
                if (drawTime != -1) {
                    duration = now - drawTime;
                }

                drawTime = now;

                float ratio = ratioPreference.get();
                catchTime = now + Math.round(duration / (1 + ratio) * ratio);

                speech.playEarcon(TICK, TextToSpeech.QUEUE_ADD, null);
            }

            this.drive = gym.snapshot.drive;

            if (catchTime != -1 && now > catchTime) {
                catchTime = -1;

                speech.playEarcon(TICK, TextToSpeech.QUEUE_ADD, null);
            }
        }

        public void reset() {
            drawTime = -1;
            catchTime = -1;
            duration = -1;
        }
    }
}
