package svenmeier.coxswain.heart.generic;

import android.support.annotation.Nullable;

public interface ConnectionStatusListener {
    void onConnectionStatusChange(final Class impl, final ConnectionStatus newStatus, final @Nullable String deviceName, final @Nullable String message);
}
