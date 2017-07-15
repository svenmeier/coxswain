package svenmeier.coxswain.heart.bluetooth.device;

import java.util.function.Consumer;

import svenmeier.coxswain.util.Destroyable;

public interface BluetoothHeartDevice extends Destroyable {
    Destroyable watch(Consumer<Integer> heartRateConsumer);
}
