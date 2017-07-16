package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import svenmeier.coxswain.heart.bluetooth.BluetoothHeartConnectionListener;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.reading.GattBatteryStatus;
import svenmeier.coxswain.heart.bluetooth.reading.GattBodySensorLocation;
import svenmeier.coxswain.heart.bluetooth.reading.GattHeartRateValue;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToBattery;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToBodySensorLocation;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToHeartRateValue;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;

/**
 *  Mixes in additional typical requests to the device.
 *
 *  A trait would actually be nice for that, but ... nehhh ... Java
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class AbstractBluetoothHeartAdditionalReadingsDevice extends AbstractBluetoothHeartDevice {
    public AbstractBluetoothHeartAdditionalReadingsDevice(Context context, BluetoothDevice delegate, BluetoothHeartConnectionListener connectionListener) {
        super(context, delegate, connectionListener);
    }

    public AbstractBluetoothHeartAdditionalReadingsDevice(Conversation conversation) {
        super(conversation);
    }


    @Override
    public CompletableFuture<GattBatteryStatus> readBattery() {
        return query(BluetoothHeartCharacteristics.BATTERY_STATUS)
                .handle(CharacteristicToBattery.INSTANCE);
    }

    @Override
    public void readBattery(final BatteryStatusListener listener) {
        readBattery().whenComplete(new BiConsumer<GattBatteryStatus, Throwable>() {
            @Override
            public void accept(GattBatteryStatus gattBatteryStatus, Throwable throwable) {
                final Integer p = gattBatteryStatus.getBatteryPercent();
                if (p != null) {
                    listener.onBatteryStatus(p);
                }
            }
        });
    }

    @Override
    public CompletableFuture<GattBodySensorLocation> readBodySensorLocation() {
        return query(BluetoothHeartCharacteristics.BATTERY_STATUS)
                .handle(CharacteristicToBodySensorLocation.INSTANCE);
    }

    public CompletableFuture<GattHeartRateValue> readRestingHeartRate() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.RESTING_HEART_RATE);
    }

    public CompletableFuture<GattHeartRateValue> readArobicHeartRateLowerLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.AEROBIC_HEART_RATE_LOWER_LIMIT);
    }

    public CompletableFuture<GattHeartRateValue> readArobicHeartRateUpperLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.AEROBIC_HEART_RATE_UPPER_LIMIT);
    }

    public CompletableFuture<GattHeartRateValue> readAnarobicHeartRateLowerLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.ANAEROBIC_HEART_RATE_LOWER_LIMIT);
    }

    public CompletableFuture<GattHeartRateValue> readAnarobicHeartRateUpperLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.ANAEROBIC_HEART_RATE_UPPER_LIMIT);
    }

    public CompletableFuture<GattHeartRateValue> readFatBurnHeartRateLowerLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.FAT_BURN_HEART_RATE_LOWER_LIMIT);
    }

    public CompletableFuture<GattHeartRateValue> readFatBurnHeartRateUpperLimit() {
        return readReadHeatRateValue(BluetoothHeartCharacteristics.FAT_BURN_HEART_RATE_UPPER_LIMIT);
    }

    private CompletableFuture<GattHeartRateValue> readReadHeatRateValue(final BluetoothHeartCharacteristics chr) {
        return query(chr)
                .handle(CharacteristicToHeartRateValue.INSTANCE);
    }
}
