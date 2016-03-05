package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Snapshot;

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

		assertEquals("#protocol 4>USB<_WR_>IV?<IV42020#version 42020>IRD140>IRD057", trace.toString());
	}

	@Test
	public void unsupported() throws Exception {
		Snapshot memory = new Snapshot();

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol4 protocol = new Protocol4(transfer, trace);
		protocol.setOutputThrottle(0);
		assertEquals(Protocol4.VERSION_UNKOWN, protocol.getVersion());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Protocol4.VERSION_UNSUPPORTED, protocol.getVersion());
	}
}