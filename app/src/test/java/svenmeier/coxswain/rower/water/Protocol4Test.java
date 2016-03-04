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

		Protocol4 protocol = new Protocol4(transfer, new NullWriter());
		protocol.setOutputThrottle(0);

		protocol.transfer(memory);
		transfer.assertOutput("USB\r\n");

		transfer.setupInput("_WR_\r\n");
		protocol.transfer(memory);
		transfer.assertOutput("IV?\r\n");

		transfer.setupInput("IV42020\r\n");
		protocol.transfer(memory);
		assertEquals("42020", protocol.getVersion());
	}
}