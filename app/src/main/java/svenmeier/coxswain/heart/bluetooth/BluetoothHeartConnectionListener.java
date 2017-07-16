package svenmeier.coxswain.heart.bluetooth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Emitted by the AbstractHeartDevice. The scanner will use BluetoothHeartDiscoveryListener instead.
 *
 * @see svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartDevice
 * @see BluetoothHeartDiscoveryListener
 */
public interface BluetoothHeartConnectionListener {
    void onConnected(@Nullable String deviceName, @NonNull String deviceId);
    void onDisconnected(@Nullable String deviceName, @NonNull String deviceId);
}
