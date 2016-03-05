package svenmeier.coxswain.rower.water;

import org.junit.Test;

import svenmeier.coxswain.gym.Snapshot;

import static org.junit.Assert.assertEquals;

/**
 */
public class Protocol3Test {

	@Test
	public void test() throws Exception {
		Snapshot memory = new Snapshot();

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol3 protocol = new Protocol3(transfer, trace);

		transfer.setupInput(new byte[]{(byte)0xFE, (byte)0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(10), memory.distance.get());

		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(20), memory.distance.get());


		transfer.setupInput(new byte[]{(byte) 0xFC, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.strokes.get());

		transfer.setupInput(new byte[]{(byte) 0xFC, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(2), memory.strokes.get());


		transfer.setupInput(new byte[]{(byte) 0xFB, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.pulse.get());

		transfer.setupInput(new byte[]{(byte) 0xFB, (byte) 0x01});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.pulse.get());


		transfer.setupInput(new byte[]{(byte) 0xFF, (byte) 0x01, (byte) 0x02});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.strokeRate.get());
		assertEquals(Integer.valueOf(20), memory.speed.get());

		transfer.setupInput(new byte[]{(byte) 0xFF, (byte) 0x01, (byte) 0x02});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.strokeRate.get());
		assertEquals(Integer.valueOf(20), memory.speed.get());

		assertEquals("#protocol 3<FE 01<FE 01<FC 01<FC 01<FB 01<FB 01<FF 01 02<FF 01 02", trace.toString());
	}

}