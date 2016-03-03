package svenmeier.coxswain.rower.water.usb;

import android.hardware.usb.UsbDeviceConnection;

/**
 */
public class ControlTransfer {

	private static final int SET_DATA_REQUEST_TYPE = 0x40;
	private static final int SET_BAUD_RATE = 0x03;

	private final UsbDeviceConnection connection;

	public ControlTransfer(UsbDeviceConnection connection) {
		this.connection = connection;
	}

	public void setBaudRate(int baudRate) {
		int divisor = 3000000 / baudRate;

		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, SET_BAUD_RATE, divisor, 0, null, 0, 0);
	}
}
