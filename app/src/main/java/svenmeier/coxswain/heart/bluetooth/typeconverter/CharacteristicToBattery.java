package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BiFunction;
import java.util.function.Function;

import svenmeier.coxswain.heart.bluetooth.reading.GattBatteryStatus;

@RequiresApi(api = Build.VERSION_CODES.N)
public enum  CharacteristicToBattery implements BiFunction<BluetoothGattCharacteristic, Throwable, GattBatteryStatus> {
    INSTANCE;

    @Override
    public GattBatteryStatus apply(final BluetoothGattCharacteristic bytes, final Throwable throwable) {
        return new GattBatteryStatus(bytes);
    }

    @Override
    public <V> BiFunction<BluetoothGattCharacteristic, Throwable, V> andThen(Function<? super GattBatteryStatus, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}
