package svenmeier.coxswain.heart.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothHeartDiscoveryListener {
    static final int UNKNOWN_STRENGTH = -1;

    void onDiscovered(BluetoothDevice device, int SignalStrength, boolean supportsConnectionLess);
    default void onLost(String deviceId) {};
}
