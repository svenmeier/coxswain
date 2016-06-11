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
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import propoid.util.content.Preference;
import svenmeier.coxswain.Event;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Difficulty;

/**
 */
public class DefaultMotivator implements Motivator, TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    /**
     * Time before limit is repeated.
     */
    public static final int LIMIT_LATENCY = 20000;

    private final Context context;

    private Gym gym;

    private TextToSpeech speech;

    private AudioManager audio;

    private boolean initialized;

    private int spoken;

    private Event pending;

    private List<Analyser> analysers = new ArrayList<>();

    public DefaultMotivator(Context context) {
        this.context = context;

        this.gym = Gym.instance(context);

        speech = new TextToSpeech(context, this);
        speech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                spoken--;

                if (spoken == 0 && audio != null) {
                    audio.abandonAudioFocus(DefaultMotivator.this);
                }
            }
        });

        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        analysers.add(new Finish());
        analysers.add(new Change());
        analysers.add(new Limit());
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
        for (int a = 0; a < analysers.size(); a++) {
            analysers.get(a).analyse(event, current);
        }
    }

    private void speak(String text) {
        if (spoken == 0) {
            audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
        spoken++;

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "spoken" + spoken);

        speech.speak(text, TextToSpeech.QUEUE_ADD, parameters);
    }

    private void pause() {
        speech.playSilence(50, TextToSpeech.QUEUE_ADD, null);
    }

    private void ringtone(String name) {
        speech.playEarcon(name, TextToSpeech.QUEUE_ADD, null);
    }

    @Override
    public void destroy() {
        speech.shutdown();
        speech = null;

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

            for (Analyser analyser : analysers) {
                analyser.init();
            }

            initialized = true;

            if (pending != null) {
                onEvent(pending);
                pending = null;
            }
        }
    }

    private void addRingtone(Preference<String> preference, String key) {
        String ringtone = preference.get();
        if (ringtone != null && ringtone.length() > 0) {
            speech.addEarcon(key, ringtone);
        }
    }

    private abstract class Analyser {
        public abstract void init();

		/**
		 * Analyse an event.
         *
         * @param event event
         * @param current current, may be {@code null}
         */
        public abstract void analyse(Event event, Gym.Current current);

        public abstract void reset();
    }

    /**
     * Analyse segment change.
     */
    private class Change extends Analyser {

        private Preference<String> ringtoneEasyPreference = Preference.getString(context, R.string.preference_audio_ringtone_easy);
        private Preference<String> ringtoneMediumPreference = Preference.getString(context, R.string.preference_audio_ringtone_medium);
        private Preference<String> ringtoneHardPreference = Preference.getString(context, R.string.preference_audio_ringtone_hard);

        private Preference<Boolean> speakSegmentPreference = Preference.getBoolean(context, R.string.preference_audio_speak_segment);

        @Override
        public void init() {
            addRingtone(ringtoneEasyPreference, key(Difficulty.EASY));
            addRingtone(ringtoneMediumPreference, key(Difficulty.MEDIUM));
            addRingtone(ringtoneHardPreference, key(Difficulty.HARD));
        }

        private String key(Difficulty difficulty) {
            return "[" + difficulty.toString() + "]";
        }

        public void analyse(Event event, Gym.Current current) {
            if (event == Event.PROGRAM_START || event == Event.SEGMENT_CHANGED) {
                if (current != null) {
                    ringtone(key(current.segment.difficulty.get()));

                    if (speakSegmentPreference.get()) {
                        String describe = current.describe();
                        pause();
                        speak(describe);
                    }

                    for (Analyser analyser : analysers) {
                        analyser.reset();
                    }
                }
            }
        }

        @Override
        public void reset() {
        }
    }

    /**
     * Analyse program finish.
     */
    private class Finish extends Analyser {

        private static final String KEY = "[FINISHED]";

        private Preference<String> ringtoneFinishPreference = Preference.getString(context, R.string.preference_audio_ringtone_finish);

        @Override
        public void init() {
            addRingtone(ringtoneFinishPreference, KEY);
        }

        public void analyse(Event event, Gym.Current current) {
            if (event == Event.PROGRAM_FINISHED) {
                ringtone(KEY);
            }
        }

        @Override
        public void reset() {
        }
    }

    /**
     * Analyse segment limit.
     */
    private class Limit extends Analyser {

        private Preference<Boolean> speakLimitPreference = Preference.getBoolean(context, R.string.preference_audio_speak_limit);

        private long underLimitSince = -1;

        @Override
        public void init() {
        }

        public void analyse(Event event, Gym.Current current) {
            if (event != Event.SNAPPED || current == null || speakLimitPreference.get() == false) {
                return;
            }

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
}
