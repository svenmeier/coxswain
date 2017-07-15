package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.ToastConnectionStatusListener;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.util.Destroyable;

/**
 *  Provides the new implementation in the format of the previous Heart-readings
 */
@SuppressWarnings("unused") // Initialized through reflection
public class BluetoothHeartAdapter extends Heart implements BluetoothHeartDiscoveryListener {
    final Destroyable currentScan;
    Destroyable heartRateListener = null;
    private @Nullable String deviceName;

    public BluetoothHeartAdapter(Context context, Measurement measurement) {
        super(context, measurement);

        registerConnectionStatusListener(new ToastConnectionStatusListener(context));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.i(Coxswain.TAG, "Start bluetooth-scan...");
            final BluetoothLeHeartScanner scanner = new BluetoothLeHeartScanner(context);
            updateConnectionStatus(ConnectionStatus.SCANNING);
            currentScan = scanner.scan(this);
        } else {
            Log.w(Coxswain.TAG, "New bluetooth unavailable: Needs Nougat!");
            updateConnectionStatus(ConnectionStatus.UNAVAILABLE_ON_SYSTEM);
            currentScan = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess) {
        deviceName = device.getName();
        updateConnectionStatus(ConnectionStatus.CONNECTING, device.getName(), null);
        Log.i(Coxswain.TAG, "Using first discovered device: " + device.getAddress() + " -> " + device.getName());
        currentScan.destroy();
        // TODO: Does not cover connection-less as we have to scan again using scanner.find()

        final BluetoothHeartDevice dev = new BluetoothHeartDeviceFactory(context).make(device);
        heartRateListener = dev.watch(this);
    }

    @Override
    public void onLost(String deviceId) {
        updateConnectionStatus(ConnectionStatus.CONNECTING, deviceName, "Lost connectivity to bluetooth device");
    }

    @Override
    public void onHeartRate(int heartRate) {
        updateConnectionStatus(ConnectionStatus.CONNECTED, deviceName, null);
        super.onHeartRate(heartRate);
    }

    @Override
    public void destroy() {
        if (heartRateListener != null) {
            heartRateListener.destroy();
        }
        super.destroy();
    }
}
