package svenmeier.coxswain.garmin;

import android.location.Location;
import android.util.Pair;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import propoid.util.io.XmlNavigator;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 * Converter for {@code TCX} (Training Center XML).
 */
public class TCX2Workout {

	private final XmlNavigator navigator;

	private SimpleDateFormat dateFormat;

	private Workout workout;

	private List<Snapshot> snapshots;

	private String programName;

	public TCX2Workout(Reader reader) throws IOException {
		navigator = new XmlNavigator(reader);

		// time for trackpoints must be in UTC
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public Workout getWorkout() {
		return workout;
	}

	public List<Snapshot> getSnapshots() {
		return snapshots;
	}

	public String getProgramName() {
		return programName;
	}

	public TCX2Workout workout() throws IOException, ParseException {
		if (navigator.descent("Activity") == false) {
			throw new ParseException("<Activity> missing", navigator.offset());
		}

		this.workout = new Workout();

		if (navigator.descent("Lap") == false) {
			throw new ParseException("<Lap> missing", navigator.offset());
		}

		workout.start.set(dateFormat.parse(navigator.getAttributeValue("StartTime")).getTime());

		workout.duration.set(Integer.parseInt(navigator.getText("TotalTimeSeconds")));
		workout.distance.set(Integer.parseInt(navigator.getText("DistanceMeters")));
		workout.energy.set(Integer.parseInt(navigator.getText("Calories")));

		this.snapshots = snapshots(workout);

		workout.duration.set(snapshots.size());

		if (navigator.descent("Extensions")) {
			if (navigator.descent("LX")) {
				workout.strokes.set(Integer.parseInt(navigator.getText("Steps")));
				navigator.ascent();
			}
			navigator.ascent();
		}

		navigator.ascent();

		training();

		navigator.ascent();

		return this;
	}

	private void training() throws IOException {
		if (navigator.descent("Training")) {
			if (navigator.descent("Plan")) {
				programName = navigator.getText("Name");

				navigator.ascent();
			}

			navigator.ascent();
		}
	}

	private List<Snapshot> snapshots(Workout workout) throws IOException {
		List<Snapshot> snapshots = new ArrayList<>();

		if (navigator.descent("Track")) {
			while (navigator.descent("Trackpoint")) {
				if (snapshots.size() == 0) {
					workout.location.set(location());
				}

				snapshots.add(snapshot());
				navigator.ascent();
			}

			navigator.ascent();
		}

		return snapshots;
	}

	private Location location() throws IOException {
		Location location = new Location("");

		if (navigator.descent("Position")) {
			location.setLatitude(Double.parseDouble(navigator.getText("LatitudeDegrees")));
			location.setLatitude(Double.parseDouble(navigator.getText("LongitudeDegrees")));

			navigator.ascent();
		}

		return location;
	}

	private Snapshot snapshot() throws IOException {
		Snapshot snapshot = new Snapshot();

		snapshot.distance.set(Integer.parseInt(navigator.getText("DistanceMeters")));

		if (navigator.descent("HeartRateBpm")) {
			snapshot.pulse.set(Integer.parseInt(navigator.getText("Value")));
			navigator.ascent();
		}

		snapshot.strokeRate.set(Integer.parseInt(navigator.getText("Cadence")));

		if (navigator.descent("Extensions")) {
			if (navigator.descent("TPX")) {
				snapshot.speed.set(Math.round(Float.parseFloat(navigator.getText("Speed")) * 100));
				snapshot.power.set(Integer.parseInt(navigator.getText("Watts")));
				navigator.ascent();
			}
			navigator.ascent();
		}

		return snapshot;
	}
}
