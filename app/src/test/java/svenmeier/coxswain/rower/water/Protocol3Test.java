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

		// distance +1.5
		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x0F});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(1), memory.distance.get());
		// distance +0.5
		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x05});
		protocol.transfer(memory);
		assertEquals(Integer.valueOf(2), memory.distance.get());


		transfer.setupInput(new byte[]{(byte) 0xFC, (byte) 0xFC});
		protocol.transfer(memory);
		assertEquals(Boolean.FALSE, memory.drive.get());
		assertEquals(Integer.valueOf(2), memory.strokes.get());


		transfer.setupInput(new byte[]{(byte) 0xFD, (byte) 0x01, (byte) 0x02});
		protocol.transfer(memory);
		assertEquals(Boolean.TRUE, memory.drive.get());


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

		transfer.setupInput(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03});
		protocol.transfer(memory);

		assertEquals("#protocol 3<FE 0F<FE 05<FC<FC<FD 01 02<FB 01<FB 01<FF 01 02<FF 01 02<01<02<03", trace.toString());
	}
}