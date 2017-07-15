package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.function.BiFunction;
import java.util.function.Function;

@RequiresApi(api = Build.VERSION_CODES.N)
public enum CharacteristicToHeart implements BiFunction<BluetoothGattCharacteristic, Throwable, GattHeartRateMeasurement> {
    INSTANCE;

    @Override
    public GattHeartRateMeasurement apply(final BluetoothGattCharacteristic bytes, final Throwable throwable) {
        return new GattHeartRateMeasurement(bytes);
    }

    @Override
    public <V> BiFunction<BluetoothGattCharacteristic, Throwable, V> andThen(Function<? super GattHeartRateMeasurement, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}


