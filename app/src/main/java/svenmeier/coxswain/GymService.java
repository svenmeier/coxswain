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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.motivator.DefaultMotivator;
import svenmeier.coxswain.motivator.Motivator;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.mock.MockRower;
import svenmeier.coxswain.rower.water.WaterRower;

public class GymService extends Service {

    public static final String ACTION_STOP = "svenmeier.coxswain.GYM_STOP";

    private BroadcastReceiver receiver;

    private Gym gym;

    private Handler handler = new Handler();

    private Snapshot memory = new Snapshot();

    private Rowing rowing;

    public GymService() {
    }

    @Override
    public void onCreate() {
        gym = Gym.instance(this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_STOP.equals(action)) {
                    Gym.instance(GymService.this).select(null);
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(ACTION_STOP));
    }

    @Override
    public void onDestroy() {
        if (this.rowing != null) {
            endRowing();
        }

        unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (this.rowing != null) {
            endRowing();
            if (device == null) {
                return START_NOT_STICKY;
            }
        }

        startRowing(device);

        return START_NOT_STICKY;
    }

    private void startRowing(UsbDevice device) {

        Rower rower;
        if (device == null) {
            rower = new MockRower(memory);
        } else {
            rower = new WaterRower(this, memory, device);
        }

        rowing = new Rowing(rower);
        new Thread(rowing).start();
    }

    private void endRowing() {
        this.rowing = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("NewApi")
    private void startForeground(String text, Class<?> activity, String action) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(this, activity), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pendingIntent);

        if (action != null) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);

            // uncomment for heads-up notification
            // builder.setPriority(Notification.PRIORITY_MAX);

            try {
                builder.addAction(R.drawable.ic_stop_black_24dp,
                        getString(R.string.gym_notification_stop),
                        PendingIntent.getBroadcast(this, 0, new Intent(action), 0));
            } catch (NoSuchMethodError notApi14) {
            }
        }

        // notApi14
        startForeground(1, builder.getNotification());
    }

    /**
     * Current rowing on rower.
     */
    private class Rowing implements Runnable {

        private final Rower rower;

        private final Motivator motivator;

        private Program program;

        private String text;

        public Rowing(Rower rower) {
            this.rower = rower;

            motivator = new DefaultMotivator(GymService.this);
        }

        public void run() {
            if (rower.open()) {
                while (true) {
                    if (gym.program != program) {
                        // program changed
                        program = gym.program;

                        memory.clear();
                        rower.reset();
                    }

                    if (GymService.this.rowing != this|| rower.row() == false) {
                        break;
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (GymService.this.rowing != Rowing.this) {
                                // no longer current
                                return;
                            }

                            Gym.Current current = gym.current;
                            if (current == null) {
                                showNotification(String.format(getString(R.string.gym_notification_connected), rower.getName()), MainActivity.class, null);
                                return;
                            }

                            if (gym.program != program) {
                                // program changed
                                return;
                            }

                            String name = program.name.get();
                            String description = current.describe();
                            showNotification(name + " - " + description, WorkoutActivity.class, ACTION_STOP);

                            Event event = gym.addSnapshot(memory);
                            motivator.onEvent(event);

                            if (event == Event.PROGRAM_FINISHED) {
                                gym.select(null);
                            }
                        }
                    });
                }

                rower.close();
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    motivator.destroy();

                    showNotification(null, null, null);
                }
            });
        }

        private void showNotification(String text, Class<?> activity, String action) {
            if (text == null) {
                if (this.text != null) {
                    stopForeground(true);
                }
            } else {
                if (text.equals(this.text) == false) {
                    startForeground(text, activity, action);
                }
            }

            this.text = text;
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
