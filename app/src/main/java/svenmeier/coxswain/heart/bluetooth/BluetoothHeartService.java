package svenmeier.coxswain.heart.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartAdditionalReadingsDevice;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothHeartService extends Service implements BluetoothHeartDiscoveryListener, BatteryStatusListener, HeartRateListener {
    public static final IntentFilter HEART_MEASUREMENT_FILTER = new IntentFilter("HeartMeasurement");
    public static final IntentFilter BATTERY_FILTER = new IntentFilter("Battery");
    public static final IntentFilter PROGRESS_FILTER = new IntentFilter("BtProgress");
    public static final String CONNECTION_STATUS = "ConnectionStatus";

    private static final String TAG = Coxswain.TAG + "BT";

    private Destroyable currentScan;
    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void initialize() {
        scan();
    }

    private void scan() {
        Log.i(Coxswain.TAG, "Start bluetooth-scan...");
        final BluetoothLeHeartScanner scanner = new BluetoothLeHeartScanner(this);
        publishProgress(ConnectionStatus.SCANNING);
        currentScan = scanner.scan(this);
        Log.i(Coxswain.TAG, "SCAN returned");
    }

    @Override
    public void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess) {
        publishProgress(ConnectionStatus.CONNECTING);
        Log.i(TAG, "Using first discovered device: " + device.getAddress() + " -> " + device.getName());

        destroyCurrentScan();

        // TODO: Does not cover connection-less as we have to scan again using scanner.find()
        connect(device);
    }

    private void connect(final BluetoothDevice device) {
        Log.i(TAG, "Connecting to heart " + device.getName());

        final AbstractBluetoothHeartAdditionalReadingsDevice dev = new BluetoothHeartDeviceFactory(this).make(device);
        dev.readBattery(this);
        //dev.readAnarobicHeartRateLowerLimit();
        //dev.readAnarobicHeartRateUpperLimit();

        Log.i(Coxswain.TAG, "Start watching");
        dev.watch(this);
    }

    //
    //  Listeners, will broadcast to the subscribers...
    //

    @Override
    public void onLost(String deviceId) {
        publishProgress(ConnectionStatus.CONNECTING);
    }

    @Override
    public void onBatteryStatus(final int percentageLeft) {
        final String action = BATTERY_FILTER.getAction(0);
        final Intent broadcast = new Intent(action);
        broadcast.putExtra(action, percentageLeft);
        sendBroadcast(broadcast);
    }

    @Override
    public void onHeartRate(final int bpm) {
        final String action = HEART_MEASUREMENT_FILTER.getAction(0);
        final Intent broadcast = new Intent(action);
        broadcast.putExtra(action, bpm);
        sendBroadcast(broadcast);
    }

    private void publishProgress(ConnectionStatus status) {
        final String action = PROGRESS_FILTER.getAction(0);
        final Intent broadcast = new Intent(action);
        broadcast.putExtra(CONNECTION_STATUS, status.name());
        sendBroadcast(broadcast);
    }


    //
    // Ducks feed. Does this translate to english?! ... anyways
    //

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyCurrentScan();
    }

    private synchronized void destroyCurrentScan() {
        if (currentScan != null) {
            currentScan.destroy();
            currentScan = null;
        }
    }


    public class LocalBinder extends Binder {
        BluetoothHeartService getService() {
            return BluetoothHeartService.this;
        }
    }
}
