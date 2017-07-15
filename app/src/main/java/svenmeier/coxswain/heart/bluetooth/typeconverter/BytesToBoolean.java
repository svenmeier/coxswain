package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BytesToBoolean implements BiFunction<List<Byte>, Throwable, Boolean> {
    @Override
    public Boolean apply(final List<Byte> bytes, final Throwable throwable) {
        return bytes != null &&
                bytes.size() == 1 &&
                bytes.get(0) > 0;
    }

    @Override
    public <V> BiFunction<List<Byte>, Throwable, V> andThen(Function<? super Boolean, ? extends V> after) {
        throw new UnsupportedOperationException("Unavailable in Java 1.7 backport");
    }
}

