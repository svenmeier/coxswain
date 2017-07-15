package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BiFunction;
import java.util.function.Function;

import svenmeier.coxswain.heart.bluetooth.reading.GattBodySensorLocation;

@RequiresApi(api = Build.VERSION_CODES.N)
public enum CharacteristicToBodySensorLocation implements BiFunction<BluetoothGattCharacteristic, Throwable, GattBodySensorLocation> {
    INSTANCE;

    @Override
    public GattBodySensorLocation apply(final BluetoothGattCharacteristic bytes, final Throwable throwable) {
        return new GattBodySensorLocation(bytes);
    }

    @Override
    public <V> BiFunction<BluetoothGattCharacteristic, Throwable, V> andThen(Function<? super GattBodySensorLocation, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}
