package svenmeier.coxswain.rower.water;

import svenmeier.coxswain.rower.water.usb.ITransfer;

import static org.junit.Assert.assertEquals;

/**
 */
class TestTransfer implements ITransfer {

	public byte[] buffer = new byte[256];
	public int length = 0;

	@Override
	public void setBaudRate(int baudRate) {
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
		assertEquals(this.length, string.length());

		for (int b = 0; b < string.length(); b++) {
			assertEquals(this.buffer[b], (byte) string.charAt(b));
		}
	}

	public void assertOutput(byte[] buffer) {
		assertEquals(this.length, buffer.length);

		for (int b = 0; b < buffer.length; b++) {
			assertEquals(this.buffer[b], buffer[b]);
		}
	}

	@Override
	public byte[] buffer() {
		return buffer;
	}

	@Override
	public int bulkInput() {
		return this.length;
	}

	@Override
	public void bulkOutput(int length) {
		this.length = length;
	}
}
