package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ITransfer;
import svenmeier.coxswain.rower.water.usb.UsbTransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class UsbTransferTest {

	@Test
	public void baudrate() throws Exception {
		assertEquals(0x1A, UsbTransfer.divisor(115200));
		assertEquals(0x9c4, UsbTransfer.divisor(1200));
	}

	@Test
	public void data() throws Exception {
		assertEquals(0b0_0_000_000_00001000, UsbTransfer.data(8, ITransfer.PARITY_NONE, ITransfer.STOP_BIT_1_0, false));
		assertEquals(0b0_1_010_001_00000100, UsbTransfer.data(4, ITransfer.PARITY_ODD, ITransfer.STOP_BIT_2_0, true));
	}
}