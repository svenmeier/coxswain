package svenmeier.coxswain;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.view.WindowManager;

import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.SegmentsData;
import svenmeier.coxswain.view.SegmentsView;
import svenmeier.coxswain.view.ValueContainer;
import svenmeier.coxswain.view.ValueView;


/**
 */
public class WorkoutActivity extends FragmentActivity {

    public static final int DELAY_MILLIS = 500;
    private Gym gym;

    private SegmentsView segmentsView;

    private LevelView levelView;

    private ValueView durationView;
    private ValueView distanceView;
    private ValueView strokesView;
    private ValueView speedView;
    private ValueView strokeRateView;
    private ValueView pulseView;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_workout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        segmentsView = (SegmentsView) findViewById(R.id.workout_segments);
        segmentsView.setData(new SegmentsData(gym.program));

        levelView = (LevelView) findViewById(R.id.workout_progress);

        durationView = (ValueView) findViewById(R.id.target_duration);
        distanceView = (ValueView) findViewById(R.id.target_distance);
        strokesView = (ValueView) findViewById(R.id.target_strokes);
        speedView = (ValueView) findViewById(R.id.target_speed);
        strokeRateView = (ValueView) findViewById(R.id.target_strokeRate);
        pulseView = (ValueView) findViewById(R.id.target_pulse);
    }

    @Override
    public void onResume() {
        super.onResume();

        handler = new Handler();
        valueUpdate.run();
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler = null;
    }

    private Runnable valueUpdate = new Runnable() {
        public void run() {
            if (handler == null) {
                return;
            }

            updateLevel();

            updateValues();

            handler.postDelayed(this, DELAY_MILLIS);
        }
    };

    private void updateValues() {
        Snapshot snapshot = gym.getLastSnapshot();

        int achieved = 0;
        int duration = 0;
        int distance = 0;
        int strokes = 0;
        int speed = 0;
        int strokeRate = 0;
        int pulse = 0;

        if (gym.current != null) {
            Segment segment = gym.current.segment;

            achieved = gym.current.achieved();

            duration = segment.duration.get();
            distance = segment.distance.get();
            strokes = segment.strokes.get();
            speed = segment.speed.get();
            strokeRate = segment.strokeRate.get();
            pulse = segment.pulse.get();
        }

        target(durationView, gym.workout.duration.get(), duration, achieved);
        target(distanceView, snapshot.distance, distance, achieved);
        target(strokesView, snapshot.strokes, strokes, achieved);
        limit(speedView, snapshot.speed, speed);
        limit(strokeRateView, snapshot.strokeRate, strokeRate);
        limit(pulseView, snapshot.pulse, pulse);
    }

    private void target(ValueView view, int memory, int segment, int achieved) {
        ValueContainer container = (ValueContainer)view.getParent();
        if (segment > 0) {
            container.setState(R.attr.field_target);

            view.setValue(Math.max(0, segment - achieved));
        } else {
            container.clearState();

            view.setValue(memory);
        }
    }

    private void limit(ValueView view, int memory, int segment) {
        ValueContainer container = (ValueContainer)view.getParent();
        if (segment > 0) {
            int difference = memory - segment;
            if (difference < 0) {
                container.setState(R.attr.field_low);
            } else {
                container.setState(R.attr.field_high);
            }

            view.setPattern(view.getPattern().replace('-', '+'));
            view.setValue(difference);
        } else {
            container.clearState();

            view.setPattern(view.getPattern().replace('+', '-'));
            view.setValue(memory);
        }
    }

    private void updateLevel() {
        float value = 0f;
        float total = 0f;
        Gym.Current current = gym.current;
        for (Segment segment : gym.program.segments.get()) {
            float segmentValue = segment.asDuration();

            if (current != null && current.segment == segment) {
                value = total + current.completion() * segmentValue;
            }

            total += segmentValue;
        }
        if (current == null) {
            value = total;
        }
        levelView.setLevel(Math.round(value * 10000 / total));
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, WorkoutActivity.class);

        context.startActivity(intent);
    }
}