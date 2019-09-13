package svenmeier.coxswain.rower.wired.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.Collection;

/**
 * Created by sven on 24.10.15.
 */
public class UsbConnector extends BroadcastReceiver {

	private static final String DEVICE_CONNECT = "svenmeier.coxswain.usb.lister";

	private final Context context;

	private final UsbManager manager;

	public UsbConnector(Context context) {
		this.context = context;

		manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		context.registerReceiver(this, new IntentFilter(DEVICE_CONNECT));
	}

	public void destroy() {
		context.unregisterReceiver(this);
	}

	public Collection<UsbDevice> list() {
		return manager.getDeviceList().values();
	}

	/**
	 * Connect to the given device.
	 *
	 * @see #onConnected(UsbDevice) 
	 */
	public void connect(UsbDevice device) {

		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(DEVICE_CONNECT), 0);

		manager.requestPermission(device, intent);
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (DEVICE_CONNECT.equals(action)) {
			synchronized (this) {
				UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					onConnected(device);
				}
			}
		}

	}

	/**
	 * Callback when device was connected.
	 *
	 * @see #connect(UsbDevice)
	 */
	protected void onConnected(UsbDevice device) {

	}
}