package svenmeier.coxswain.bluetooth;

import android.os.Build;
import android.support.annotation.RequiresApi;

import org.junit.Test;

import java.util.UUID;

import svenmeier.coxswain.bluetooth.BluetoothHeart;

import static junit.framework.Assert.assertEquals;

/**
 * Test for {@link GattScanner}.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattScannerTest {

	@Test
	public void test() {
		assertEquals(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), GattScanner.uuid(0x2A37));

		assertEquals(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), GattScanner.uuid(0x2902));
	}
}
