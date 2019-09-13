package svenmeier.coxswain.rower.wired;

import org.junit.Test;

import svenmeier.coxswain.rower.wired.usb.ITransfer;
import svenmeier.coxswain.rower.wired.usb.UsbTransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class UsbTransferTest {

	@Test
	public void baudrate() throws Exception {
		assertEquals(0x09c4, UsbTransfer.divisor(1200));
		assertEquals(0x001A, UsbTransfer.divisor(115200));
	}

	@Test
	public void data() throws Exception {
		assertEquals(0b0_0_000_000_00001000, UsbTransfer.data(8, ITransfer.PARITY_NONE, ITransfer.STOP_BIT_1_0, false));
		assertEquals(0b0_0_001_001_00001000, UsbTransfer.data(8, ITransfer.PARITY_ODD, ITransfer.STOP_BIT_1_5, false));
		assertEquals(0b0_1_010_010_00000100, UsbTransfer.data(4, ITransfer.PARITY_EVEN, ITransfer.STOP_BIT_2_0, true));
	}
}