package svenmeier.coxswain.heart;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.common.base.Strings;

import svenmeier.coxswain.Heart;

public class ToastConnectionStatusListener implements ConnectionStatusListener {
    private final Context context;
    private final Handler toastHadnler;

    public ToastConnectionStatusListener(Context context) {
        this.context = context;
        this.toastHadnler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onConnectionStatusChange(final Heart impl, final ConnectionStatus newStatus, final @Nullable String deviceName, final @Nullable String msg) {
        final String message = prettyPrint(impl, newStatus, deviceName, msg);
        if (!Strings.isNullOrEmpty(message)) {
            toast(message);
        }
    }

    private String prettyPrint(final Heart impl, final ConnectionStatus newStatus, final @Nullable String deviceName, final @Nullable String message) {
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

    private String prettyPrint(final Heart impl) {
        return impl.getClass().getSimpleName();
    }

    private void toast(String text) {
        if (isOnUiThread()) {
            toastNow(text);
        } else {
            toastHadnler.post(makeDelayedToast(text));
        }
    }

    private Runnable makeDelayedToast(final String text) {
        return new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void toastNow(String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	private boolean isOnUiThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
