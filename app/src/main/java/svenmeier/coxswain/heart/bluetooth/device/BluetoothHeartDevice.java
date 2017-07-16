package svenmeier.coxswain.heart.bluetooth.device;

import java.util.concurrent.CompletableFuture;

import svenmeier.coxswain.heart.bluetooth.reading.GattBatteryStatus;
import svenmeier.coxswain.heart.bluetooth.reading.GattBodySensorLocation;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateListener;
import svenmeier.coxswain.util.Destroyable;

/**
 *  Readable values to be exposed.
 *
 *  @see AbstractBluetoothHeartAdditionalReadingsDevice
 */
public interface BluetoothHeartDevice extends Destroyable {
    Destroyable watch(HeartRateListener heartRateConsumer);
    CompletableFuture<GattBatteryStatus> readBattery();
    CompletableFuture<GattBodySensorLocation> readBodySensorLocation();
    void readBattery(BatteryStatusListener listener);
}
