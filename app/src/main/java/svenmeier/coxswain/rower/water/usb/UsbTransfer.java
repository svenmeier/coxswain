package svenmeier.coxswain.rower.water.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

/**
 */
public class UsbTransfer implements ITransfer {

	private static final int SET_DATA_REQUEST_TYPE = 0x40;
	private static final int SET_BAUD_RATE = 0x03;

	private final UsbDeviceConnection connection;
	private final UsbEndpoint output;
	private final UsbEndpoint input;

	private byte[] buffer;

	private int timeout;

	public UsbTransfer(UsbDeviceConnection connection, UsbEndpoint input, UsbEndpoint output) {
		this.connection = connection;

		this.input = input;
		this.output = output;

		this.buffer = new byte[Math.min(output.getMaxPacketSize(), input.getMaxPacketSize())];
	}

	public void setBaudRate(int baudRate) {
		int divisor = 3000000 / baudRate;

		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, SET_BAUD_RATE, divisor, 0, null, 0, 0);
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public byte[] buffer() {
		return buffer;
	}

	public int bulkInput() {
		return connection.bulkTransfer(input, buffer, buffer.length, timeout);
	}

	public void bulkOutput(int length) {
		connection.bulkTransfer(output, buffer, length, timeout);
	}
}
