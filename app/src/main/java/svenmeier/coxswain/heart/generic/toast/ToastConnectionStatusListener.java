package svenmeier.coxswain.heart.generic.toast;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Strings;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.ConnectionStatus;
import svenmeier.coxswain.heart.ConnectionStatusListener;

public class ToastConnectionStatusListener implements ConnectionStatusListener {
    private final Context context;

    public ToastConnectionStatusListener(Context context) {
        this.context = context;
    }

    @Override
    public void onConnectionStatusChange(final Class impl, final ConnectionStatus newStatus, final @Nullable String deviceName, final @Nullable String msg) {
        final String message = prettyPrint(impl, newStatus, deviceName, msg);
        if (!Strings.isNullOrEmpty(message)) {
            toast(message);
        }
    }

    private String prettyPrint(final Class impl, final ConnectionStatus newStatus, final @Nullable String deviceName, final @Nullable String message) {
        if (Strings.isNullOrEmpty(message)) {
            switch (newStatus) {
                case INITIAL:
                    return "";
                case SCANNING:
                    return "Scanning on " + prettyPrint(impl);
                case CONNECTING:
                    return "Connecting" + ((Strings.isNullOrEmpty(deviceName)) ? "" : " to " + deviceName) + " using " + prettyPrint(impl);
                case CONNECTED:
                    return "Connected" + ((Strings.isNullOrEmpty(deviceName)) ? "" : " to " + deviceName) + " using " + prettyPrint(impl);
                case UNAVAILABLE_ON_SYSTEM:
                    return prettyPrint(impl) + " is unavailable";
                default:
                    return "";
            }
        } else {
            return message;
        }
    }

    private String prettyPrint(final Class impl) {
        if (impl != null) {
            return impl.getSimpleName();
        } else {
            return "UNKNOWN";
        }
    }

    private void toast(String text) {
        if (isOnUiThread()) {
            toastNow(text);
        } else {
            // TODO: Send message to UI-thread
            Log.w(Coxswain.TAG, "Skipped toast: " + text);
        }
    }

    private void toastNow(String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	private boolean isOnUiThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
