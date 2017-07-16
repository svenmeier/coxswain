package svenmeier.coxswain.heart.generic;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface HeartRateCommunication {
    interface Reader {

        Intent bind(Context context);
    }

    interface Writer {
        void bind(Context context);

        void acceptHeartRate(final int bpm);
        void acceptBatteryStatus(final int percent);
        void acceptConnectionStatus(final Class impl, @NonNull final ConnectionStatus connectionStatus, final @Nullable String device, final @Nullable String message);
    }

    Reader makeReader(@Nullable BatteryStatusListener batteryStatusListener, @Nullable ConnectionStatusListener connectionStatusListener, @Nullable HeartRateListener heartRateListener);
    Writer makeWriter();
}
