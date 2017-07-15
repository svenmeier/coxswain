package svenmeier.coxswain.heart.bluetooth.device;

import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

public interface BluetoothHeartDevice extends Destroyable {
    Destroyable watch(HeartRateListener heartRateConsumer);
}
