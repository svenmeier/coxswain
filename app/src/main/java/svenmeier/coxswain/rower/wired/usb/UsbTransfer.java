package svenmeier.coxswain.rower.wired.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

/**
 */
public class UsbTransfer implements ITransfer {

	private static final int SET_DATA_REQUEST_TYPE = 0x40;

	private static final int CLEAR = 0x00;

	private static final int SET_FLOW = 0x02;

	private static final int SET_BAUD_RATE = 0x03;

	private static final int SET_DATA = 0x04;

	private static final int RESET = 0;
	private static final int PURGE_RX = 1;
	private static final int PURGE_TX = 2;

	private final UsbDeviceConnection connection;
	private final UsbEndpoint output;
	private final UsbEndpoint input;

	private byte[] buffer;
	private int bufferLength = 0;
	private byte[] bufferBulk;

	private int timeout;

	public UsbTransfer(UsbDeviceConnection connection, UsbEndpoint input, UsbEndpoint output) {
		this.connection = connection;

		this.input = input;
		this.output = output;

		this.buffer = new byte[Math.min(output.getMaxPacketSize(), input.getMaxPacketSize())];
		this.bufferBulk = new byte[buffer.length];

		// clear
		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, CLEAR, RESET, 0, null, 0, timeout);
		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, CLEAR, PURGE_TX, 0, null, 0, timeout);
		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, CLEAR, PURGE_RX, 0, null, 0, timeout);
		// flow control none
		this.connection.controlTransfer(SET_DATA_REQUEST_TYPE, SET_FLOW, 0, 0, null, 0, timeout);
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
	public void produce(byte[] b) {
		connection.bulkTransfer(output, b, b.length, timeout);
	}

	@Override
	public Consumer consumer() {
		final int length = connection.bulkTransfer(input, bufferBulk, bufferBulk.length - bufferLength, timeout);
		if (length != -1) {
			System.arraycopy(bufferBulk, 0, buffer, bufferLength, length);
			bufferLength += length;
		}

		return new Consumer() {
			@Override
			protected byte[] getBuffer() {
				return buffer;
			}

			@Override
			protected int getBufferLength() {
				return bufferLength;
			}

			@Override
			protected void setBufferLength(int bufferLength) {
				UsbTransfer.this.bufferLength = bufferLength;
			}
		};
	}

	public static int divisor(int baudrate) {
		return 3000000 / baudrate;
	}

	public static int data(int dataBits, int parity, int stopBits, boolean tx) {
		return (tx ? 1 << 14 : 0) | (stopBits << 11) | (parity << 8) | dataBits;
	}
}
