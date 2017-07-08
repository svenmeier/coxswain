package svenmeier.coxswain.garmin;

import android.location.Location;

public interface ITrack {

	double getLongitude();

	double getLatitude();

	void setDistance(int meters);
}
