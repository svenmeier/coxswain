import org.junit.Test;

import java.util.UUID;

import svenmeier.coxswain.bluetooth.BluetoothHeart;

import static junit.framework.Assert.assertEquals;

/**
 * Created by sven on 14.12.16.
 */
public class BluetoothHeartTest {

	@Test
	public void test() {
		assertEquals(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), BluetoothHeart.uuid(0x2A37));

		assertEquals(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothHeart.uuid(0x2902));
	}
}
