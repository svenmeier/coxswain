package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 *  Listens for data whose transmission is initiated by the BT device.
 *
 *  The devices has to be configured to send notifications for a given characteristic, before
 *  this will work.
 *
 *  @see svenmeier.coxswain.heart.bluetooth.device.AbstractBluetoothHeartDevice#enableNotifications(svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics, svenmeier.coxswain.heart.bluetooth.device.BluetoothNotificationListener)
 */
public interface BluetoothNotificationListener {
    void onNotification(final BluetoothGattCharacteristic chr);
}
