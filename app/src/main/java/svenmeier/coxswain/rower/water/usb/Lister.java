package svenmeier.coxswain.rower.water.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by sven on 24.10.15.
 */
public class Lister {

	private final Context context;

	public Lister(Context context) {
		this.context = context;
	}

	public Collection<UsbDevice> list() {
		UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

		return manager.getDeviceList().values();
	}
}