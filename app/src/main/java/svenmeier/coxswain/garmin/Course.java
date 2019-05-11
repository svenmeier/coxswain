package svenmeier.coxswain.garmin;

import android.location.Location;

import java.util.List;

public class Course implements ICourse {

	public final List<Trackpoint> trackpoints;
	
	private double longitude;

	private double latitude;

	public Course(List<Trackpoint> trackpoints) {
		this.trackpoints = trackpoints;
	}

	@Override
	public void setDistance(double meters) {

		Trackpoint temp = null;

		for (Trackpoint trackpoint : trackpoints) {
			if (temp != null && trackpoint.distanceMeters > meters) {
				double factor = (meters - temp.distanceMeters) / (trackpoint.distanceMeters - temp.distanceMeters);

				longitude = temp.location.getLongitude() + (trackpoint.location.getLongitude() - temp.location.getLongitude()) * factor;
				latitude  = temp.location.getLatitude()  + (trackpoint.location.getLatitude()  - temp.location.getLatitude()) * factor;
				break;
			}

			temp = trackpoint;
		}
	}

	@Override
	public double getLongitude() {
		return longitude;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	public static class Trackpoint {

		public final Location location;

		public final double distanceMeters;

		public Trackpoint(Location location, double distanceMeters) {
			this.location = location;
			this.distanceMeters = distanceMeters;
		}
	}
}
