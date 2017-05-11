package svenmeier.coxswain.garmin;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link TrackTest}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class)
public class TrackTest {

	@Test
	public void startGreenwich() {

		Track path = new Track(null);

		assertEquals(51.4826d, path.getLatitude(), 0.001d);

		path.addDistance(111320);

		assertEquals(50.4826, path.getLatitude(), 0.001d);

		path.addDistance(1113200);

		assertEquals(40.4826, path.getLatitude(), 0.001d);
	}

	@Test
	public void startArticCircle() {

		Location location = new Location("");
		location.setLongitude(0);
		location.setLatitude(-66.57d);

		Track path = new Track(location);

		assertEquals(-66.57d, path.getLatitude(), 0.001d);

		path.addDistance(111320);

		assertEquals(-65.57d, path.getLatitude(), 0.001d);

		path.addDistance(1113200);

		assertEquals(-55.57d, path.getLatitude(), 0.001d);
	}

	@Test
	public void startAntarticCircle() {

		Location location = new Location("");
		location.setLongitude(0);
		location.setLatitude(66.57d);

		Track path = new Track(location);

		assertEquals(66.57d, path.getLatitude(), 0.001d);

		path.addDistance(111320);

		assertEquals(65.57d, path.getLatitude(), 0.001d);

		path.addDistance(1113200);

		assertEquals(55.57d, path.getLatitude(), 0.001d);
	}
}
