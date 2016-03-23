package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.water.usb.ITransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class Protocol4Test {

	@Test
	public void test() throws Exception {
		Snapshot memory = new Snapshot();

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

		protocol.transfer(memory);
		transfer.assertOutput("USB\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput("_WR_\r\n");
		protocol.transfer(memory);
		transfer.assertOutput("IV?\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput("IV42020\r\n");
		protocol.transfer(memory);
		assertEquals("42020", protocol.getVersion());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals("42020", protocol.getVersion());

		assertEquals("#protocol 4>USB<_WR_#handshake complete>IV?<IV42020#version 42020>IRD140>IRD057", trace.toString());
	}

	@Test
	public void unknown() throws Exception {
		Snapshot memory = new Snapshot();

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		protocol.setOutputThrottle(0);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(memory);
		transfer.assertOutput("USB\r\n");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(memory);
		transfer.assertOutput("");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(memory);
		transfer.assertOutput("");
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		assertEquals("#protocol 4>USB", trace.toString());
	}

	@Test
	public void unsupportedFirst() throws Exception {
		Snapshot memory = new Snapshot();

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		protocol.setOutputThrottle(0);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Protocol4.VERSION_UNSUPPORTED, protocol.getVersion());

		assertEquals("#protocol 4#unsupported", trace.toString());
	}

	@Test
	public void unsupportedAfterOutput() throws Exception {
		Snapshot memory = new Snapshot();

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		protocol.setOutputThrottle(0);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		protocol.transfer(memory);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Protocol4.VERSION_UNSUPPORTED, protocol.getVersion());

		assertEquals("#protocol 4>USB#unsupported", trace.toString());
	}
}