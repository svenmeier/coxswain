package svenmeier.coxswain.heart;

import svenmeier.coxswain.Heart;

public interface ConnectionStatusListener {
    void onConnectionStatusChange(final Heart impl, final ConnectionStatus newStatus);
}
