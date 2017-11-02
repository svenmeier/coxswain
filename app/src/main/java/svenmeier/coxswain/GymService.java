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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.motivator.DefaultMotivator;
import svenmeier.coxswain.motivator.Motivator;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.mock.MockRower;
import svenmeier.coxswain.rower.water.WaterRower;

public class GymService extends Service {

    private Gym gym;

    private Handler handler = new Handler();

    private Preference<Boolean> openEnd;

    private Rowing rowing;

    private Foreground foreground;

    public GymService() {
    }

    @Override
    public void onCreate() {
        gym = Gym.instance(this);

        openEnd = Preference.getBoolean(this, R.string.preference_open_end);

        foreground = new Foreground();
    }

    @Override
    public void onDestroy() {
        foreground.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (this.rowing != null) {
            endRowing();
        }

        startRowing(device);

        return START_NOT_STICKY;
    }

    private void startRowing(UsbDevice device) {

        Rower rower;
        if (device == null) {
            rower = new MockRower();
        } else {
            rower = new WaterRower(this, device);
        }

        rowing = new Rowing(rower);
        new Thread(rowing).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void endRowing() {
        this.rowing = null;
    }

    private void rowingEnded(Rowing rowing) {
        if (this.rowing == rowing) {
            this.rowing = null;
        }

        if (this.rowing == null) {
            stopSelf();
        }
    }

    /**
     * Current rowing on rower.
     */
    private class Rowing implements Runnable {

        private final Rower rower;

        private Heart heart;

        private final Motivator motivator;

        private Program program;

        public Rowing(Rower rower) {
            this.rower = rower;

            this.heart = Heart.create(GymService.this, rower);

            this.motivator = new DefaultMotivator(getApplicationContext());
        }

        public void run() {
            if (rower.open()) {
                while (true) {
                    if (GymService.this.rowing != this) {
                        break;
                    }

                    if (gym.program != program) {
                        // program changed
                        program = gym.program;

                        rower.reset();
                    }

                    if (rower.row() == false) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                gym.deselect();
                            }
                        });
                        break;
                    }

                    heart.pulse();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (GymService.this.rowing != Rowing.this) {
                                // no longer current
                                return;
                            }

                            if (gym.program ==  null) {
                                foreground.connected(String.format(getString(R.string.gym_notification_connected), rower.getName()));
                                return;
                            } else if (gym.program != program) {
                                // program changed
                                return;
                            }

                            String text = program.name.get();
                            float completion = 0;
                            if (gym.progress != null) {
                                text += " - " +  gym.progress.describe();
                                completion = gym.progress.completion();
                            }
                            foreground.workout(text, completion);

                            Event event = gym.onMeasured(rower);
                            motivator.onEvent(event);

                            if (event == Event.PROGRAM_FINISHED && openEnd.get() == false) {
                                gym.deselect();
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

                    heart.destroy();

                    rowingEnded(Rowing.this);
                }
            });
        }

    }

    private class Foreground {

        private Preference<Boolean> headsup;

        private String text;

        private int progress = -1;

        private long headsupSince;

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        private Notification.Builder builder;

        public Foreground() {
            headsup = Preference.getBoolean(GymService.this, R.string.preference_integration_headsup);

            builder = new Notification.Builder(GymService.this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setOngoing(true)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setPriority(Notification.PRIORITY_DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String id = "gym";
                NotificationChannel channel = new NotificationChannel(id,
                            "Gym", NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);

                builder.setChannelId(id);
            }

            builder.setContentText(getString(R.string.gym_notification_connecting));
            startForeground(1, builder.build());
        }

        public void connected(String text) {
            GymService service = GymService.this;

            if (text.equals(this.text)) {
                return;
            }

            builder.setContentIntent(PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setContentText(text);
            builder.setProgress(0, 0, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            notificationManager.notify(1, builder.build());

            this.text = text;
            this.progress = -1;
        }

        public void workout(String text, float completion) {
            GymService service = GymService.this;

            int progress = (int)(completion * 100);

            if (text.equals(this.text) && progress == this.progress) {
                return;
            }

            builder.setContentIntent(PendingIntent.getActivity(service, 1, new Intent(service, WorkoutActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setContentText(text);
            builder.setProgress(100, progress, false);

            notificationManager.notify(1, builder.build());

            this.text = text;
            this.progress = progress;
        }

        public void stop() {
            text = null;
            progress = -1;

            stopForeground(true);
        }
    }

    public static void start(Context context, UsbDevice device) {
        Intent serviceIntent = new Intent(context, GymService.class);

        if (device != null) {
            serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
