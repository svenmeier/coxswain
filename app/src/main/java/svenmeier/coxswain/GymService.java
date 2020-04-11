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
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;

import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.motivator.DefaultMotivator;
import svenmeier.coxswain.motivator.Motivator;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.mock.MockRower;
import svenmeier.coxswain.rower.wireless.BluetoothRower;
import svenmeier.coxswain.rower.wired.UsbRower;

public class GymService extends Service implements Gym.Listener, Rower.Callback, Heart.Callback {

    private static final String CONNECTOR_USB = "CONNECTOR_USB";

    public static final String CONNECTOR_BLUETOOTH = "CONNECTOR_BLUETOOTH";

    public static final String CONNECTOR_MOCK = "CONNECTOR_MOCK";

    public static final String CONNECTOR_NONE = "CONNECTOR_NONE";

    private Gym gym;

    private Handler handler = new Handler();

    private Preference<Boolean> openEnd;

    private Foreground foreground;

    private Rower rower;

    private Heart heart;

    private Motivator motivator;

    private Program program;

    @Override
    public void onCreate() {
        gym = Gym.instance(this);

        openEnd = Preference.getBoolean(this, R.string.preference_open_end);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (this.rower != null) {
            endRowing();
        }
        
        if (!startRowing(intent)) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private boolean startRowing(Intent intent) {

        if (intent.getBooleanExtra(CONNECTOR_NONE, false)) {
            return false;
        } else if (intent.getBooleanExtra(CONNECTOR_BLUETOOTH, false)) {
            rower = new BluetoothRower(this, this);
        } else if (intent.getBooleanExtra(CONNECTOR_MOCK, false)) {
            rower = new MockRower(this, this);
        } else {
            rower = new UsbRower(this, (UsbDevice) intent.getParcelableExtra(CONNECTOR_USB), this);
        }

        this.foreground = new Foreground();

        this.motivator = new DefaultMotivator(getApplicationContext());

        gym.addListener(this);

        rower.open();

        return true;
    }

    private void endRowing() {
        this.rower.close();
        this.rower = null;

        this.motivator.destroy();
        this.motivator = null;

        if (this.heart != null) {
            this.heart.destroy();
            this.heart = null;
        }

        this.foreground.stop();
        this.foreground = null;

        this.program = null;

        gym.removeListener(this);

        // do not keep program
        gym.deselect();
    }

    @Override
    public void changed(Object scope) {
        if (rower == null) {
            return;
        }

        if (gym.program != this.program) {
            this.program = gym.program;

            rower.reset();

            foreground.changed();
        }
    }

    @Override
    public void onConnected() {
        if (rower == null) {
            return;
        }

        this.heart = Heart.create(GymService.this, rower, this);

        foreground.connected();

        this.program = gym.program;
        if (this.program != null) {
            rower.reset();

            foreground.changed();
        }
    }

    @Override
    public void onMeasurement(Measurement measurement) {
        if (rower == null) {
            return;
        }

        Event event = gym.onMeasured(measurement);
        motivator.onEvent(event, rower, gym.progress);

        if (event == Event.REJECTED) {
            Toast.makeText(this, R.string.rowing_measurement_rejected, Toast.LENGTH_LONG).show();
            gym.deselect();
        } else if (event == Event.PROGRAM_FINISHED && openEnd.get() == false) {
            Toast.makeText(this, R.string.rowing_program_finished, Toast.LENGTH_LONG).show();
            gym.deselect();
        } else if (program != null){
            foreground.progress();
        }
    }

    @Override
    public void onDisconnected() {
        if (rower == null) {
            return;
        }

        endRowing();

        stopSelf();
    }

    private class Foreground {

        private String text;

        private int progress = -1;

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        private Notification.Builder builder;

        public Foreground() {
            builder = new Notification.Builder(GymService.this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setOngoing(true)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setPriority(Notification.PRIORITY_DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String id = "gym";
                NotificationChannel channel = new NotificationChannel(id,
                            "Gym", NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);

                builder.setChannelId(id);
            }

            PendingIntent intent = PendingIntent.getService(getApplicationContext(), 0,
                    createIntent(getApplicationContext(), CONNECTOR_NONE), PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, getString(R.string.gym_notification_disconnect),intent);

            builder.setContentText(getString(R.string.gym_notification_connecting, rower.getName()));
            builder.setOnlyAlertOnce(false);
            
            startForeground(1, builder.build());
        }

        public void connected() {
            GymService service = GymService.this;

            String text = String.format(getString(R.string.gym_notification_connected), rower.getName());

            if (text.equals(this.text)) {
                return;
            }

            builder.setContentIntent(PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setContentText(text);
            builder.setProgress(0, 0, false);
            builder.setOnlyAlertOnce(true);
            notificationManager.notify(1, builder.build());

            this.text = text;
            this.progress = -1;
        }

        public void progress() {
            String text = program.name.get();
            float completion = 0;
            if (gym.progress != null) {
                text += " - " +  gym.progress.describe();
                completion = gym.progress.completion();
            }

            GymService service = GymService.this;

            int progress = (int)(completion * 100);

            if (text.equals(this.text) && progress == this.progress) {
                return;
            }

            builder.setContentIntent(PendingIntent.getActivity(service, 1, new Intent(service, WorkoutActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            builder.setContentText(text);
            builder.setProgress(100, progress, false);
            builder.setOnlyAlertOnce(text.equals(this.text));
            notificationManager.notify(1, builder.build());

            this.text = text;
            this.progress = progress;
        }

        public void changed() {
            if (progress != -1 && program == null) {
                connected();
            }
        }

        public void stop() {
            text = null;
            progress = -1;

            stopForeground(true);
        }
    }

    @NonNull
    private static Intent createIntent(Context context, Object connector) {
        Intent intent = new Intent(context, GymService.class);

        if (connector instanceof UsbDevice) {
            intent.putExtra(CONNECTOR_USB, (UsbDevice)connector);
        } else if (connector instanceof String){
            intent.putExtra((String)connector, true);
        } else {
            throw new IllegalArgumentException(connector.toString());
        }
        return intent;
    }

    public static void start(Context context, Object connector) {
        Intent intent = createIntent(context, connector);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}