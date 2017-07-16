package svenmeier.coxswain.heart.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.generic.ConnectionStatus;
import svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartAdditionalReadingsDevice;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateCommunication;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

/**
 *  Anything regarding the bluetooth HRM should go through this class.
 *
 *  The service will communicate through a HeartRateCommunication.Writer, which has to be provided
 *  to the initialize-method. The writer needs to take care about the crossing of thread-boundaries.
 *
 *  @see BluetoothHeartAdapter
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class BluetoothHeartService extends Service implements BluetoothHeartDiscoveryListener, BluetoothHeartConnectionListener, BatteryStatusListener, HeartRateListener {
    private static final String TAG = Coxswain.TAG + "BT";

    private Destroyable currentScan;
    private final IBinder binder = new LocalBinder();
    private HeartRateCommunication.Writer listener;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void initialize(final @NonNull HeartRateCommunication.Writer writeTo) {
        this.listener = writeTo;
        this.listener.bind(this);
        scan();
    }

    private void scan() {
        Log.i(Coxswain.TAG, "Start bluetooth-scan...");
        final BluetoothLeHeartScanner scanner = new BluetoothLeHeartScanner(this);
        publishProgress(ConnectionStatus.SCANNING, null, null);
        currentScan = scanner.scan(this);
        Log.i(Coxswain.TAG, "SCAN returned");
    }

    @Override
    public void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess) {
        publishProgress(ConnectionStatus.CONNECTING, device.getName(), null);
        Log.i(TAG, "Using first discovered device: " + device.getAddress() + " -> " + device.getName());

        destroyCurrentScan();

        // TODO: Does not cover connection-less as we have to scan again using scanner.find()
        connect(device);
    }

    private void connect(final BluetoothDevice device) {
        Log.i(TAG, "Connecting to heart " + device.getName());

        final AbstractBluetoothHeartAdditionalReadingsDevice dev = new BluetoothHeartDeviceFactory(this, this).make(device);
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
    public void onConnected(String deviceName, String deviceId) {
        final String reportName = (deviceName != null) ? deviceName : deviceId;
        publishProgress(ConnectionStatus.CONNECTED, reportName, null);
    }

    @Override
    public void onDisconnected(String deviceName, String deviceId) {
        final String reportName = (deviceName != null) ? deviceName : deviceId;
        publishProgress(ConnectionStatus.CONNECTING, reportName, "Disconnected from " + reportName);
    }

    @Override
    public void onLost(String deviceId, String name) {
        final String reportName = (name != null) ? name : deviceId;
        publishProgress(ConnectionStatus.CONNECTING, reportName, "Lost connection to " + reportName);
    }

    @Override
    public void onBatteryStatus(final int percentageLeft) {
        if (listener != null) {
            listener.acceptBatteryStatus(percentageLeft);
        }
    }

    @Override
    public void onHeartRate(final int bpm) {
        if (listener != null) {
            listener.acceptHeartRate(bpm);
        }
    }

    private void publishProgress(final ConnectionStatus status, final @Nullable String device, final @Nullable String message) {
        if (listener != null) {
            listener.acceptConnectionStatus(BluetoothHeartAdapter.class, status, device, message);
        }
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
