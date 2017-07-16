package svenmeier.coxswain.heart.generic.communication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.google.common.base.Preconditions;

import svenmeier.coxswain.heart.generic.ConnectionStatus;
import svenmeier.coxswain.heart.generic.ConnectionStatusListener;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateCommunication;
import svenmeier.coxswain.heart.generic.HeartRateListener;

/**
 *  Send heart rate readings through broadcasts.
 *  This may be a bit overkill.
 */
public class BroadcastCommunication implements HeartRateCommunication {
    public static final String ACTION_HEART_RATE_MEASUREMENT = BroadcastCommunication.class.getName() + ".HeartMeasurement";
    public static final String ACTION_BATTERY = BroadcastCommunication.class.getName() + ".Battery";
    public static final String ACTION_PROGRESS = BroadcastCommunication.class.getName() + ".Progress";

    public static final IntentFilter HEART_MEASUREMENT_FILTER = new IntentFilter(ACTION_HEART_RATE_MEASUREMENT);
    public static final IntentFilter BATTERY_FILTER = new IntentFilter(ACTION_BATTERY);
    public static final IntentFilter PROGRESS_FILTER = new IntentFilter(ACTION_PROGRESS);

    private static final String IMPL = "Impl";
    private static final String CONNECTION_STATUS = "ConnectionStatus";
    private static final String DEVICE = "Device";
    private static final String MESSAGE = "Message";

    @Override
    public Reader makeReader(final @Nullable BatteryStatusListener batteryStatusListener, final @Nullable ConnectionStatusListener connectionStatusListener, final @Nullable HeartRateListener heartRateListener) {
        return new BroadcastReader(batteryStatusListener, connectionStatusListener, heartRateListener);
    }

    @Override
    public Writer makeWriter() {
        return new BroadcastWriter();
    }

    @Override
    public void destroy() {

    }

    public static class BroadcastWriter implements HeartRateCommunication.Writer {
        private Context context;

        @Override
        public void bind(Context context) {
            this.context = context;
        }

        @Override
        public void acceptHeartRate(int bpm) {
            Preconditions.checkNotNull(context, "Need to bind first");
            final Intent broadcast = new Intent(ACTION_HEART_RATE_MEASUREMENT);
            broadcast.putExtra(ACTION_HEART_RATE_MEASUREMENT, bpm);
            context.sendBroadcast(broadcast);
        }

        @Override
        public void acceptBatteryStatus(int percent) {
            Preconditions.checkNotNull(context, "Need to bind first");
            final Intent broadcast = new Intent(ACTION_BATTERY);
            broadcast.putExtra(ACTION_BATTERY, percent);
            context.sendBroadcast(broadcast);
        }

        @Override
        public void acceptConnectionStatus(final Class impl, final ConnectionStatus connectionStatus, @Nullable String device, @Nullable String message) {
            Preconditions.checkNotNull(context, "Need to bind first");
            final Intent broadcast = new Intent(ACTION_PROGRESS);
            broadcast.putExtra(IMPL, impl);
            broadcast.putExtra(CONNECTION_STATUS, connectionStatus.name());
            broadcast.putExtra(DEVICE, device);
            broadcast.putExtra(MESSAGE, message);
            context.sendBroadcast(broadcast);
        }

        @Override
        public void destroy() {
            context = null;
        }
    }

    public static class BroadcastReader extends BroadcastReceiver implements HeartRateCommunication.Reader {
        private final BatteryStatusListener batteryStatusListener;
        private final ConnectionStatusListener connectionStatusListener;
        private final HeartRateListener heartRateListener;
        private Context context;

        public BroadcastReader(final @Nullable BatteryStatusListener batteryStatusListener, final @Nullable ConnectionStatusListener connectionStatusListener, final @Nullable HeartRateListener heartRateListener) {
            this.batteryStatusListener = batteryStatusListener;
            this.connectionStatusListener = connectionStatusListener;
            this.heartRateListener = heartRateListener;
        }

        @Override
        public Intent bind(final Context context) {
            this.context = context;
            return context.registerReceiver(this, getIntentFilter());
        }

        @Override
        public void destroy() {
            context.unregisterReceiver(this);
            context = null;
        }

        public IntentFilter getIntentFilter() {
            final IntentFilter ret = new IntentFilter();
            if (batteryStatusListener != null) {
                ret.addAction(ACTION_BATTERY);
            }
            if (connectionStatusListener != null) {
                ret.addAction(ACTION_PROGRESS);
            }
            if (heartRateListener != null) {
                ret.addAction(ACTION_HEART_RATE_MEASUREMENT);
            }
            return ret;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            //Log.d(Coxswain.TAG, "Received broadcast " + action);
            if (BATTERY_FILTER.matchAction(action)) {
                final int percentage = intent.getIntExtra(action, -1);
                if ((percentage >= 0) && (batteryStatusListener != null)) {
                    batteryStatusListener.onBatteryStatus(percentage);
                }
            } else if (PROGRESS_FILTER.matchAction(action)) {
                final Class impl = (Class) intent.getSerializableExtra(IMPL);
                final String connectionStatus = intent.getStringExtra(CONNECTION_STATUS);
                final String device = intent.getStringExtra(DEVICE);
                final String message = intent.getStringExtra(MESSAGE);
                if ((connectionStatus != null) && (connectionStatusListener != null)) {
                    connectionStatusListener.onConnectionStatusChange(impl, ConnectionStatus.valueOf(connectionStatus), device, message);
                }
            } else if (HEART_MEASUREMENT_FILTER.matchAction(action)) {
                final int heartBpm = intent.getIntExtra(action, -1);
                if ((heartBpm >= 0) && (heartRateListener != null)) {
                    heartRateListener.onHeartRate(heartBpm);
                }
            }
        }
    }
}
