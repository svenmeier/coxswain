package svenmeier.coxswain.rower.wired;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import svenmeier.coxswain.gym.Measurement;
import svenmeier.coxswain.rower.wired.usb.ITransfer;
import svenmeier.coxswain.rower.wired.usb.TestTransfer;

import static org.junit.Assert.assertEquals;

/**
 */
public class Protocol3Test {

	Measurement measurement = new Measurement();

	@Test
	public void test() throws Exception {

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol3 protocol = new Protocol3(transfer, trace);
		assertEquals(1200, transfer.baudrate);
		assertEquals(8, transfer.dataBits);
		assertEquals(TestTransfer.PARITY_NONE, transfer.parity);
		assertEquals(ITransfer.STOP_BIT_1_0, transfer.stopBits);
		assertEquals(false, transfer.tx);
		protocol.setThrottle(0);

		// distance +2.5
		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x19});
		protocol.transfer(measurement);
		assertEquals(2, measurement.getDistance());
		// distance +0.5
		transfer.setupInput(new byte[]{(byte) 0xFE, (byte) 0x05});
		protocol.transfer(measurement);
		assertEquals(3, measurement.getDistance());


		transfer.setupInput(new byte[]{(byte) 0xFC});
		protocol.transfer(measurement);
		assertEquals(1, measurement.getStrokes());


		transfer.setupInput(new byte[]{(byte) 0xFD, (byte) 0x01, (byte) 0x02});
		protocol.transfer(measurement);


		transfer.setupInput(new byte[]{(byte) 0xFB, (byte) 0x01, (byte) 0xFB, (byte) 0x02});
		protocol.transfer(measurement);
		assertEquals(2, measurement.getPulse());

		transfer.setupInput(new byte[]{(byte) 0xFF, (byte) 0x01, (byte) 0x02});
		protocol.transfer(measurement);
		assertEquals(1, measurement.getStrokeRate());
		assertEquals(20, measurement.getSpeed());

		transfer.setupInput(new byte[]{(byte) 0xFF, (byte) 0x01, (byte) 0x02});
		protocol.transfer(measurement);
		assertEquals(1, measurement.getStrokeRate());
		assertEquals(20, measurement.getSpeed());

		transfer.setupInput(new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03});
		protocol.transfer(measurement);

		// incomplete
		transfer.setupInput(new byte[]{(byte) 0xFF});
		protocol.transfer(measurement);

		assertEquals("#protocol 3<FE 19<FE 05<FC<FD 01 02<FB 01<FB 02<FF 01 02<FF 01 02<01#unrecognized<02#unrecognized<03#unrecognized", trace.toString());
	}

	@Test
	public void trace() throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/waterrower.trace")));

		TestTransfer transfer = new TestTransfer();
		TestTrace trace = new TestTrace();

		Protocol3 protocol = new Protocol3(transfer, trace);
		protocol.setThrottle(0);

		while (true) {
			String line = reader.readLine();

			if (line == null) {
				break;
			} else if (line.startsWith("<")) {
				String[] hexes = line.substring(1).split("\\s+");
				byte[] bytes = new byte[hexes.length];
				for (int h = 0; h < hexes.length; h++) {
					bytes[h] = (byte)Integer.parseInt(hexes[h], 16);
				}
				transfer.setupInput(bytes);
				protocol.transfer(measurement);
			}
		}
		assertEquals(363, measurement.getStrokes());
		assertEquals(1510, measurement.getDistance()	);
	}
}