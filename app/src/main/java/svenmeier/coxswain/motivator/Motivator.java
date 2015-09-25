package svenmeier.coxswain.motivator;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import propoid.util.content.Preference;
import svenmeier.coxswain.Event;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

/**
 */
public class Motivator implements TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    private static final String WHISTLE = "[whistle]";

    private final Context context;

    private Gym gym;

    private final Vibrator vibrator;

    private TextToSpeech speech;

    private AudioManager audio;

    private final Preference<Boolean> whistlePreference;

    private final Preference<Boolean> vibratePreference;

    private boolean initialized;

    private Event pending;

    public Motivator(Context context) {
        this.context = context;

        this.gym = Gym.instance(context);

        speech = new TextToSpeech(context, this);

        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);

        whistlePreference = Preference.getBoolean(context, R.string.preference_motivator_whistle);
        vibratePreference = Preference.getBoolean(context, R.string.preference_motivator_vibrate);
    }

    public void onEvent(final Event event) {
        if (initialized == false) {
            this.pending = event;
            return;
        }

        switch (event) {
            case PROGRAM_START:
            case SEGMENT_CHANGED:
                Gym.Current current = gym.current;
                if (current != null) {
                    String target = current.describeTarget();
                    String limit = current.describeLimit();

                    StringBuilder text = new StringBuilder(target);
                    if (limit.isEmpty() == false) {
                        text.append('\n');
                        text.append(limit);
                    }
                    toast(text.toString());

                    int ordinal = current.segment.difficulty.get().ordinal();
                    for (int o = 0; o <= ordinal; o++) {
                        whistle();
                    }
                    pause();
                    speak(target);
                    pause();
                    speak(limit);
                    vibrate(500);
                }
                break;
            case PROGRAM_FINISHED:
                toast(context.getString(R.string.gym_finished));

                for (int i = 0; i < 3; i++) {
                    if (whistle()) {
                        pause();
                    }
                }
                vibrate(1000);
                break;
        }
    }

    private void vibrate(int milliseconds) {
        if (vibratePreference.get()) {
            vibrator.vibrate(milliseconds);
        }
    }

    private void toast(String text) {
        Toast toast = makeText(context, text, LENGTH_SHORT);

        TextView view = (TextView) toast.getView().findViewById(android.R.id.message);
        if(view != null) {
            view.setGravity(Gravity.CENTER);
        }

        toast.show();
    }

    private void speak(String text) {
        speech.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    private void pause() {
        speech.playSilence(500, TextToSpeech.QUEUE_ADD, null);
    }

    private boolean whistle() {
        if (whistlePreference.get()) {
            speech.playEarcon(WHISTLE, TextToSpeech.QUEUE_ADD, null);
            return true;
        }
        return false;
    }

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

            initialized = true;

            if (pending != null) {
                onEvent(pending);
                pending = null;
            }
        }
    }
}
