package svenmeier.coxswain.heart.bluetooth;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.ConnectionStatusListener;
import svenmeier.coxswain.heart.generic.ToastConnectionStatusListener;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateListener;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 *  Provides the new implementation in the format of the previous Heart-readings
 */
@SuppressWarnings("unused") // Initialized through reflection
public class BluetoothHeartAdapter extends Heart implements BatteryStatusListener {
    private final HeartServiceConnection heartService;
    private final ConnectionStatusListener connectionStatusListener;
    private final HeartReceiver listener;

    public BluetoothHeartAdapter(Context uiContext, Measurement measurement) {
        super(uiContext, measurement);

        connectionStatusListener = new ToastConnectionStatusListener(uiContext);
        heartService = new HeartServiceConnection();

        registerConnectionStatusListener(new ToastConnectionStatusListener(uiContext));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.i(Coxswain.TAG, "Contacting bluetooth heart service...");
            listener = new HeartReceiver(this, connectionStatusListener, this);
            uiContext.registerReceiver(listener, listener.getIntentFilter());
            heartService.bind(uiContext);
        } else {
            Log.w(Coxswain.TAG, "New bluetooth unavailable: Needs Nougat!");
            updateConnectionStatus(ConnectionStatus.UNAVAILABLE_ON_SYSTEM, null, "Requires Android Nougat");
            listener = null;
        }
    }

    @Override
    public void onHeartRate(int heartRate) {
        updateConnectionStatus(ConnectionStatus.CONNECTED, null, null);
        super.onHeartRate(heartRate);
    }

    @Override
    public void onBatteryStatus(int percentageLeft) {
        if (percentageLeft < 20) {
            // TODO: Has to be on UI-thred
            Toast.makeText(context, "Battery of HRM: " + percentageLeft + "%", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void destroy() {
        super.destroy();
    }

    private static class HeartServiceConnection implements ServiceConnection {
        private BluetoothHeartService service = null;

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            this.service = ((BluetoothHeartService.LocalBinder) binder).getService();
            this.service.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.service = null;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void bind(final Context context) {
            final Intent intent = new Intent(context, BluetoothHeartService.class);
            if (! context.bindService(intent, this, BIND_AUTO_CREATE)) {
                Log.e(Coxswain.TAG, "Unable to bind to " + BluetoothHeartService.class.getSimpleName());
            }
        }

        @Nullable
        public BluetoothHeartService getService() {
            return service;
        }
    }

    private static class HeartReceiver extends BroadcastReceiver {
        private final BatteryStatusListener batteryStatusListener;
        private final ConnectionStatusListener connectionStatusListener;
        private final HeartRateListener heartRateListener;

        public HeartReceiver(final @Nullable BatteryStatusListener batteryStatusListener, final @Nullable ConnectionStatusListener connectionStatusListener, final @Nullable HeartRateListener heartRateListener) {
            this.batteryStatusListener = batteryStatusListener;
            this.connectionStatusListener = connectionStatusListener;
            this.heartRateListener = heartRateListener;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public IntentFilter getIntentFilter() {
            final IntentFilter ret = new IntentFilter();
            if (batteryStatusListener != null) {
                ret.addAction(BluetoothHeartService.BATTERY_FILTER.getAction(0));
            }
            if (connectionStatusListener != null) {
                ret.addAction(BluetoothHeartService.PROGRESS_FILTER.getAction(0));
            }
            if (heartRateListener != null) {
                ret.addAction(BluetoothHeartService.HEART_MEASUREMENT_FILTER.getAction(0));
            }
            return ret;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            //Log.d(Coxswain.TAG, "Received broadcast " + action);
            if (BluetoothHeartService.BATTERY_FILTER.matchAction(action)) {
                final int percentage = intent.getIntExtra(action, -1);
                if ((percentage >= 0) && (batteryStatusListener != null)) {
                    batteryStatusListener.onBatteryStatus(percentage);
                }
            } else if (BluetoothHeartService.PROGRESS_FILTER.matchAction(action)) {
                final String connectionStatus = intent.getStringExtra(BluetoothHeartService.CONNECTION_STATUS);
                if ((connectionStatus != null) && (connectionStatusListener != null)) {
                    connectionStatusListener.onConnectionStatusChange(null, ConnectionStatus.valueOf(connectionStatus), null, null);
                }
            } else if (BluetoothHeartService.HEART_MEASUREMENT_FILTER.matchAction(action)) {
                final int heartBpm = intent.getIntExtra(action, -1);
                if ((heartBpm >= 0) && (heartRateListener != null)) {
                    heartRateListener.onHeartRate(heartBpm);
                }
            }
        }
    }
}
