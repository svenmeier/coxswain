package svenmeier.coxswain.rower.wired.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

/**
 */
public abstract class Consumer {

	int index = 0;

	protected abstract byte[] getBuffer();

	protected abstract int getBufferLength();

	protected abstract void setBufferLength(int bufferLength);

	public boolean hasNext() {
		return index < getBufferLength();
	}

	public byte next() {
		if (index == getBufferLength()) {
			throw new IndexOutOfBoundsException();
		}
		byte b = getBuffer()[index];
		index++;
		return b;
	}

	public byte[] consumed() {
		byte[] consumed = new byte[index];
		System.arraycopy(getBuffer(), 0, consumed, 0, index);

		int bufferLength = getBufferLength();
		bufferLength -= index;
		System.arraycopy(getBuffer(), index, getBuffer(), 0, bufferLength);
		setBufferLength(bufferLength);

		index = 0;
		
		return consumed;
	}
}
