package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.water.usb.ITransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class Protocol4Test {

	Measurement measurement = new Measurement();

	@Test
	public void test() throws Exception {
		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		assertEquals(115200, transfer.baudrate);
		assertEquals(0, transfer.dataBits);
		assertEquals(TestTransfer.PARITY_NONE, transfer.parity);
		assertEquals(ITransfer.STOP_BIT_1_0, transfer.stopBits);
		assertEquals(false, transfer.tx);
		protocol.setOutputThrottle(0);

		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(measurement);
		transfer.assertOutput("USB\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput("_WR_\r\n");
		protocol.transfer(measurement);
		transfer.assertOutput("IV?\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput("IV42020\r\n");
		protocol.transfer(measurement);
		assertEquals("42020", protocol.getVersion());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(measurement);
		assertEquals("42020", protocol.getVersion());

		assertEquals("#protocol 4>USB<_WR_#handshake complete>IV?<IV42020#version 42020>IRD140>IRD057", trace.toString());
	}

	@Test
	public void unknown() throws Exception {
		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		protocol.setOutputThrottle(0);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(measurement);
		transfer.assertOutput("USB\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(measurement);
		transfer.assertOutput("");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(measurement);
		transfer.assertOutput("");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		assertEquals("#protocol 4>USB", trace.toString());
	}
}