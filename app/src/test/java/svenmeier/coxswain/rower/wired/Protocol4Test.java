package svenmeier.coxswain.rower.wired;

import org.junit.Test;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.wired.usb.ITransfer;
import svenmeier.coxswain.rower.wired.usb.TestTransfer;

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
		protocol.setThrottle(0);

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

		transfer.setupInput("IDT1E1151515\r\n");
		protocol.transfer(measurement);
		assertEquals(((15 * 60) + 15)*60 +15, measurement.duration);

		transfer.setupInput("IDT08A0003E8\r\n");
		protocol.transfer(measurement);
		assertEquals(1, measurement.energy);

		// incomplete
		transfer.setupInput("IDT08A0003");
		protocol.transfer(measurement);

		assertEquals("#protocol 4>USB<_WR_#handshake complete>IV?<IV42020#version 42020>IRD140<IDT1E1151515>IRD057<IDT08A0003E8>IRD14A>IRS1A9", trace.toString());
	}
}