package svenmeier.coxswain.garmin;

import android.location.Location;

public interface ICourse {

	void setDistance(double meters);

	double getLongitude();

	double getLatitude();
}
