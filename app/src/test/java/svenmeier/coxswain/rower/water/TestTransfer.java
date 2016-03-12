package svenmeier.coxswain.rower.water;

import svenmeier.coxswain.rower.water.usb.ITransfer;

import static org.junit.Assert.assertEquals;

/**
 */
class TestTransfer implements ITransfer {

	public int baudrate;
	public int length = 0;
	public int dataBits;
	public int parity;
	public int stopBits;
	public boolean tx;

	public byte[] buffer = new byte[256];

	@Override
	public void setBaudrate(int baudrate) {
		this.baudrate = baudrate;
	}

	@Override
	public void setData(int dataBits, int parity, int stopBits, boolean tx) {
		this.dataBits = dataBits;
		this.parity = parity;
		this.stopBits = stopBits;
		this.tx = tx;
	}

	@Override
	public void setTimeout(int timeout) {
	}

	public void setupInput(String buffer) {
		for (int b = 0; b < buffer.length(); b++) {
			this.buffer[b] = (byte) buffer.charAt(b);
		}

		this.length = buffer.length();
	}

	public void setupInput(byte[] buffer) {
		System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);

		this.length = buffer.length;
	}

	public void assertOutput(String string) {
		assertEquals(string.length(), this.length);

		for (int b = 0; b < string.length(); b++) {
			assertEquals((byte) string.charAt(b), this.buffer[b]);
		}

		this.length = 0;
	}

	public void assertOutput(byte[] buffer) {
		assertEquals(buffer.length, this.length);

		for (int b = 0; b < buffer.length; b++) {
			assertEquals(buffer[b], this.buffer[b]);
		}

		this.length = 0;
	}

	@Override
	public byte[] buffer() {
		return buffer;
	}

	@Override
	public int bulkInput() {
		int length = this.length;

		this.length = 0;

		return length;
	}

	@Override
	public void bulkOutput(int length) {
		this.length = length;
	}
}
