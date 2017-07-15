package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import svenmeier.coxswain.heart.bluetooth.constants.GattHeartRateMeasurement;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BytesToHeart implements BiFunction<List<Byte>, Throwable, GattHeartRateMeasurement> {
    @Override
    public GattHeartRateMeasurement apply(final List<Byte> bytes, final Throwable throwable) {
        return new GattHeartRateMeasurement(bytes);
    }

    @Override
    public <V> BiFunction<List<Byte>, Throwable, V> andThen(Function<? super GattHeartRateMeasurement, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}


