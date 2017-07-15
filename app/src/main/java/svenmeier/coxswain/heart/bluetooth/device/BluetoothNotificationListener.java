package svenmeier.coxswain.heart.bluetooth.device;

import java.util.List;

public interface BluetoothNotificationListener {
    void onNotification(final List<Byte> bytes);
}
