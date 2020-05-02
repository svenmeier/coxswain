package svenmeier.coxswain.bluetooth;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * Test for {@link BlueWriter}.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattScannerTest {

	@Test
	public void test() {
		assertEquals(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), BlueWriter.uuid(0x2A37));

		assertEquals(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BlueWriter.uuid(0x2902));
	}
}
