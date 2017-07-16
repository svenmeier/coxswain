package svenmeier.coxswain.heart.bluetooth.reading;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class GattBodySensorLocation {
	public enum Location {
        UNKNOWN(null),
        OTHER(0),
		CHEST(1),
		WRIST(2),
		FINGER(3),
		HAND(4),
		EAR_LOBE(5),
		FOOT(6);

        final Integer gattVal;

		Location(Integer gattVal) {
			this.gattVal = gattVal;
		}

		public static Location parse(int value) {
			for (Location location : values()) {
				if (location.gattVal == value) {
					return location;
				}
			}
			return Location.UNKNOWN;
		}
	}

	private final Location location;

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public GattBodySensorLocation(BluetoothGattCharacteristic chr) {
		this.location = Location.parse(chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "Body sensor location " + location;
	}
}
