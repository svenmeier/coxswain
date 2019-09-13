package svenmeier.coxswain.rower.wired.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

/**
 */
public class UsbTransfer implements ITransfer {

	private static final int SET_DATA_REQUEST_TYPE = 0x40;

	private static final int SET_BAUD_RATE = 0x03;

	private static final int SET_DATA = 0x04;

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

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setBaudrate(int baudrate) {
		int divisor = divisor(baudrate);

		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, SET_BAUD_RATE, divisor, 0, null, 0, timeout);
	}

	public void setData(int dataBits, int parity, int stopBits, boolean tx) {
		int data = data(dataBits, parity, stopBits, tx);

		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, SET_DATA, data, 0, null, 0, timeout);
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

	public static int divisor(int baudrate) {
		return 3000000 / baudrate;
	}

	public static int data(int dataBits, int parity, int stopBits, boolean tx) {
		return (tx ? 1 << 14 : 0) | (stopBits << 11) | (parity << 8) | dataBits;
	}
}
