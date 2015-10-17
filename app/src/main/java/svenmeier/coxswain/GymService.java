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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.motivator.DefaultMotivator;
import svenmeier.coxswain.motivator.Motivator;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.mock.MockRower;
import svenmeier.coxswain.rower.water.WaterRower;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class GymService extends Service {

    public static final String ACTION_ROWING_STARTED = "svenmeier.coxswain.ROWING_STARTED";

    public static final String ACTION_ROWING_ENDED = "svenmeier.coxswain.ROWING_ENDED";

    private Gym gym;

    private Handler handler = new Handler();

    private Snapshot memory = new Snapshot();

    private Motivator motivator;

    private Rower rower;

    private String text;

    public GymService() {
    }

    @Override
    public void onCreate() {
        motivator = new DefaultMotivator(this);

        gym = Gym.instance(this);
    }

    @Override
    public void onDestroy() {
        if (this.rower != null) {
            endRowing();
        }

        motivator.destroy();
        motivator = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (this.rower != null) {
            endRowing();
            if (device == null) {
                return START_NOT_STICKY;
            }
        }

        startRowing(device);

        return START_NOT_STICKY;
    }

    private void startRowing(UsbDevice device) {

        broadcast(ACTION_ROWING_STARTED);

        showNotification(getString(R.string.gym_notification_ready), MainActivity.class);

        if (device == null) {
            rower = new MockRower(memory) {
                @Override
                public void onStart() {
                    new Beats(this);
                }
            };
        } else {
            rower = new WaterRower(this, memory, device) {
                @Override
                protected void onFailed(String message) {
                    makeText(GymService.this, message, LENGTH_SHORT).show();

                    endRowing();
                }

                @Override
                public void onStart() {
                    new Beats(this);
                }

                @Override
                public void onEnd() {
                    endRowing();
                }
            };
        }

        rower.open();
    }

    private void showNotification(String text, Class<?> activity) {
        if (text == null) {
            stopForeground(true);
        } else {
            if (text.equals(this.text)) {
                return;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(this, activity), 0);

            Notification notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(text)
                    .setContentIntent(pendingIntent).getNotification();
            startForeground(1, notification);
        }

        this.text = text;
    }


    private void endRowing() {
        this.rower.close();
        this.rower = null;

        broadcast(ACTION_ROWING_ENDED);

        showNotification(null, null);
    }

    private void broadcast(String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action).addCategory(Intent.CATEGORY_DEFAULT));
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class Beats implements Runnable {

        private final Rower rower;

        public Beats(Rower rower) {
            this.rower = rower;

            new Thread(this).start();
        }

        public void run() {
            while (rower.row()) {
                final Snapshot snapshot = new Snapshot();
                snapshot.distance = (short) memory.distance;
                snapshot.strokes = (short) memory.strokes;
                snapshot.speed = (short) memory.speed;
                snapshot.pulse = (short) memory.pulse;
                snapshot.strokeRate = (short) memory.strokeRate;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (rower.isOpen()) {
                            // still rowing

                            if (gym.current == null) {
                                showNotification(getString(R.string.gym_notification_ready), MainActivity.class);
                                return;
                            }
                            showNotification(gym.program.name.get(), WorkoutActivity.class);

                            if (gym.workout.duration.get() == 0) {
                                rower.reset();
                                snapshot.distance = 0;
                                snapshot.strokes = 0;
                                snapshot.speed = 0;
                                snapshot.strokeRate = 0;
                                snapshot.pulse = 0;
                            }

                            Event event = gym.addSnapshot(snapshot);
                            if (event != Event.REJECTED) {
                                gym.mergeWorkout(gym.workout);
                            }

                            motivator.onEvent(event);
                        }
                    }
                });
            }
        }
    }

    public static void start(Context context, UsbDevice device) {
        Intent serviceIntent = new Intent(context, GymService.class);

        if (device != null) {
            serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        }

        context.startService(serviceIntent);
    }
}
