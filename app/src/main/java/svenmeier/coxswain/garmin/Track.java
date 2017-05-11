package svenmeier.coxswain.garmin;

import android.location.Location;

public class Track {

	private static final double M_PER_LATITUDE = 111320d;

	private Location location;

	private int meters;

	public Track(Location location) {
		if (location == null) {
			// Greenwich
			location = new Location("");
			location.setLatitude(51.477809);
			location.setLongitude(-0.000800);
		}

		this.location = location;
	}

	public double getLongitude() {
		return location.getLongitude();
	}

	public double getLatitude() {
		return location.getLatitude() + (meters / M_PER_LATITUDE);
	}

	public void addDistance(int meters) {
		if (location.getLatitude() > 0) {
			this.meters -= meters;
		} else {
			this.meters += meters;
		}
	}

	public void setDistance(int meters) {
		if (location.getLatitude() > 0) {
			this.meters = -meters;
		} else {
			this.meters = +meters;
		}
	}
}
