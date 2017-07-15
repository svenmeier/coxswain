package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothGattCharacteristic;

public interface BluetoothNotificationListener {
    void onNotification(final BluetoothGattCharacteristic chr);
}
