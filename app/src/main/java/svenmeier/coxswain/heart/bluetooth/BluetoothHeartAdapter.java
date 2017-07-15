package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.Heart;
import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.device.BluetoothHeartDevice;
import svenmeier.coxswain.util.Destroyable;

public class BluetoothHeartAdapter extends Heart implements BluetoothHeartDiscoveryListener {
    final Destroyable currentScan;
    Destroyable heartRateListener = null;

    public BluetoothHeartAdapter(Context context, Measurement measurement) {
        super(context, measurement);

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
        updateConnectionStatus(ConnectionStatus.CONNECTING);
        Log.i(Coxswain.TAG, "Using first discovered device: " + device.getAddress() + " -> " + device.getName());
        currentScan.destroy();
        // TODO: Does not cover connection-less as we have to scan again using scanner.find()

        final BluetoothHeartDevice dev = new BluetoothHeartDeviceFactory(context).make(device);
        heartRateListener = dev.watch(this::onHeartRate);
    }

    @Override
    public void destroy() {
        if (heartRateListener != null) {
            heartRateListener.destroy();
        }
        super.destroy();
    }
}
