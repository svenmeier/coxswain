package svenmeier.coxswain.rower.wired.usb;

import svenmeier.coxswain.rower.wired.usb.ITransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class TestTransfer implements ITransfer {

	public int baudrate;
	public int bufferLength = 0;
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

		this.bufferLength = buffer.length();
	}

	public void setupInput(byte[] buffer) {
		System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);

		this.bufferLength = buffer.length;
	}

	public void assertOutput(String string) {
		assertEquals(string.length(), this.bufferLength);

		for (int b = 0; b < string.length(); b++) {
			assertEquals((byte) string.charAt(b), this.buffer[b]);
		}

		this.bufferLength = 0;
	}

	public void assertOutput(byte[] buffer) {
		assertEquals(buffer.length, this.bufferLength);

		for (int b = 0; b < buffer.length; b++) {
			assertEquals(buffer[b], this.buffer[b]);
		}

		this.bufferLength = 0;
	}

	@Override
	public void produce(byte[] b) {
		System.arraycopy(b, 0, buffer, 0, b.length);
		bufferLength = b.length;
	}

	@Override
	public Consumer consumer() {
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
				TestTransfer.this.bufferLength = bufferLength;
			}
		};
	}
}
