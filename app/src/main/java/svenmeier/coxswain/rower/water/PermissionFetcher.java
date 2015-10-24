package svenmeier.coxswain.rower.water;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * Fetch permission to use an {@link UsbDevice}.
 * <p>
 * Not used currently, since we start an activity USB_DEVICE_ATTACHED and
 * Android will ask the user for permissions automatically.
 */
public class PermissionFetcher extends BroadcastReceiver {

	private static final String ACTION_USB_PERMISSION = "svenmeier.coxswain.USB_PERMISSION";

	private Context context;

	public PermissionFetcher(Context context) {
		this.context = context;

		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		context.registerReceiver(this, filter);
	}

	public void destroy() {
		context.unregisterReceiver(this);
		context = null;
	}

	public void fetch(UsbDevice device) {
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		manager.requestPermission(device, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
	}

	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (ACTION_USB_PERMISSION.equals(action)) {
			onFetched(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
		}
	}

	protected void onFetched(boolean granted) {
	}
}