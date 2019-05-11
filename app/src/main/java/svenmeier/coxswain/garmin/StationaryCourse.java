package svenmeier.coxswain.garmin;

import android.location.Location;

public class StationaryCourse implements ICourse {

	private Location location;

	public StationaryCourse(Location location) {
		if (location == null) {
			// Greenwich
			location = new Location("");
			location.setLatitude(0);
			location.setLongitude(0);
		}

		this.location = location;
	}

	public void setDistance(double meters) {
	}

	public double getLongitude() {
		return location.getLongitude();
	}

	public double getLatitude() {
		return location.getLatitude();
	}
}