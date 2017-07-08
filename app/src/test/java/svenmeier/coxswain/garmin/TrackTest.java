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

		ITrack path = new ArtificialTrack(null);

		assertEquals(51.477809d, path.getLatitude(), 0.00d);

		path.setDistance(111320 * 1);

		assertEquals(50.48d, path.getLatitude(), 0.01d);

		path.setDistance(1113200 * 2);

		assertEquals(31.48d, path.getLatitude(), 0.01d);
	}

	@Test
	public void startArticCircle() {

		Location location = new Location("");
		location.setLongitude(0);
		location.setLatitude(-66.57d);

		ITrack path = new ArtificialTrack(location);

		assertEquals(-66.57d, path.getLatitude(), 0.01d);

		path.setDistance(111320 * 1);

		assertEquals(-65.57d, path.getLatitude(), 0.01d);

		path.setDistance(1113200 * 2);

		assertEquals(-46.57d, path.getLatitude(), 0.001d);
	}

	@Test
	public void startAntarticCircle() {

		Location location = new Location("");
		location.setLongitude(0);
		location.setLatitude(66.57d);

		ITrack path = new ArtificialTrack(location);

		assertEquals(66.57d, path.getLatitude(), 0.01d);

		path.setDistance(111320 * 1);

		assertEquals(65.57d, path.getLatitude(), 0.01d);

		path.setDistance(1113200 * 2);

		assertEquals(46.57d, path.getLatitude(), 0.01d);
	}
}
