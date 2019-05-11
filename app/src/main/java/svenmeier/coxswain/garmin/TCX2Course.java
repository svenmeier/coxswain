package svenmeier.coxswain.garmin;

import android.location.Location;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import propoid.util.io.XmlNavigator;

/**
 * Converter for {@code TCX} (Training Center XML).
 */
public class TCX2Course {

	private final XmlNavigator navigator;

	private Course course;

	public TCX2Course(Reader reader) throws IOException {
		navigator = new XmlNavigator(reader);
	}

	public Course getCourse() {
		return course;
	}

	public TCX2Course course() throws IOException, ParseException {
		if (navigator.descent("Course") == false) {
			throw new ParseException("<Course> missing", navigator.offset());
		}

		if (navigator.descent("Track") == false) {
			throw new ParseException("<Track> missing", navigator.offset());
		}

		this.course = new Course(trackpoints());

		navigator.ascent();

		navigator.ascent();

		return this;
	}

	private List<Course.Trackpoint> trackpoints() throws IOException {
		List<Course.Trackpoint> trackpoints = new ArrayList<>();

		while (navigator.descent("Trackpoint")) {
			Course.Trackpoint trackpoint = new Course.Trackpoint(location(), distanceMeters());

			trackpoints.add(trackpoint);

			navigator.ascent();
		}

		return trackpoints;
	}

	private double distanceMeters() throws IOException {
		navigator.descentRequired("DistanceMeters");

		double distanceMeters = Double.valueOf(navigator.getText());

		navigator.ascent();

		return distanceMeters;
	}

	private Location location() throws IOException {
		Location location = new Location("");

		if (navigator.descent("Position")) {
			location.setLatitude(Double.parseDouble(navigator.getText("LatitudeDegrees")));
			location.setLongitude(Double.parseDouble(navigator.getText("LongitudeDegrees")));

			navigator.ascent();
		}

		return location;
	}
}