package svenmeier.coxswain.garmin;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 * Converter for {@code TCX} (Training Center XML).
 */
public class Workout2TCX {

	private XmlSerializer serializer;

	private SimpleDateFormat dateFormat;

	private Track path;

	public Workout2TCX(Writer writer) throws IOException {
		serializer = Xml.newSerializer();
		serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		serializer.setOutput(writer);

		// time for trackpoints must be in UTC
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public void document(Workout workout, List<Snapshot> snaoshots) throws IOException {

		serializer.startDocument("UTF-8", true);

		trainingCenterDatabase(workout, snaoshots);

		serializer.endDocument();
	}

	private void trainingCenterDatabase(Workout workout, List<Snapshot> snapshots) throws IOException {

		serializer.startTag(null, "TrainingCenterDatabase");
		serializer.attribute(null, "xmlns", "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2");

		activities(workout, snapshots);

		serializer.endTag(null, serializer.getName());
	}

	private void activities(Workout workout, List<Snapshot> snapshots) throws IOException {
		serializer.startTag(null, "Activities");

		activity(workout, snapshots);

		serializer.endTag(null, serializer.getName());
	}

	private void activity(Workout workout, List<Snapshot> snapshots) throws IOException {
		serializer.startTag(null, "Activity");
		serializer.attribute(null, "Sport", "Other");

		tag(null, "Id", dateFormat.format(workout.start.get()));

		lap(workout, snapshots);

		tag(null, "Notes", "Created by Coxswain");

		training(workout);

		serializer.endTag(null, serializer.getName());
	}

	private void training(Workout workout) throws IOException {
		serializer.startTag(null, "Training");
		serializer.attribute(null, "VirtualPartner", "true");

		plan(workout);

		serializer.endTag(null, serializer.getName());
	}

	private void plan(Workout workout) throws IOException {
		serializer.startTag(null, "Plan");

		serializer.attribute(null, "Type", "Workout");
		serializer.attribute(null, "IntervalWorkout", "true");

		tag(null, "Name", workout.programName("-"));

		serializer.endTag(null, serializer.getName());
	}

	private void lap(Workout workout, List<Snapshot> snapshots) throws IOException {
		serializer.startTag(null, "Lap");

		serializer.attribute(null, "StartTime", dateFormat.format(workout.start.get()));

		tag(null, "TotalTimeSeconds", workout.duration.get().toString());
		tag(null, "DistanceMeters", workout.distance.get().toString());
		tag(null, "Calories", workout.energy.get().toString());
		tag(null, "Intensity", "Active");
		tag(null, "TriggerMethod", "Manual");

		if (snapshots.isEmpty() == false) {
			track(workout, snapshots);
		}

		extension("LX", "Steps", Integer.toString(workout.strokes.get()));

		serializer.endTag(null, serializer.getName());
	}

	private void track(Workout workout, List<Snapshot> snapshots) throws IOException {
		serializer.startTag(null, "Track");

		path = new Track(workout.location.get());

		for (int index = 0; index < snapshots.size(); index++) {
			trackpoint(workout, snapshots.get(index), index);
		}

		serializer.endTag(null, serializer.getName());
	}

	private void trackpoint(Workout workout, Snapshot snapshot, int index) throws IOException {
		serializer.startTag(null, "Trackpoint");

		tag(null, "Time", dateFormat.format(workout.start.get() + index * 1000));

		position(snapshot);

		tag(null, "DistanceMeters", snapshot.distance.get().toString());

		heartRateBpm(snapshot.pulse.get());

		tag(null, "Cadence", Integer.toString(snapshot.strokeRate.get()));

		extension("TPX", "Speed", Float.toString(snapshot.speed.get() / 100f));

		serializer.endTag(null, serializer.getName());
	}

	private void position(Snapshot snapshot) throws IOException {
		serializer.startTag(null, "Position");

		path.setDistance(snapshot.distance.get());

		tag(null, "LatitudeDegrees", Double.toString(path.getLatitude()));
		tag(null, "LongitudeDegrees", Double.toString(path.getLongitude()));

		serializer.endTag(null, serializer.getName());
	}

	private void extension(String extension, String name, String value) throws IOException {
		serializer.startTag(null, "Extensions");

		serializer.startTag(null, extension);
		serializer.attribute(null, "xmlns", "http://www.garmin.com/xmlschemas/ActivityExtension/v2");

		tag(null, name, value);

		serializer.endTag(null, serializer.getName());

		serializer.endTag(null, serializer.getName());
	}

	private void heartRateBpm(int pulse) throws IOException {
		serializer.startTag(null, "HeartRateBpm");

		tag(null, "Value", Integer.toString(pulse));

		serializer.endTag(null, serializer.getName());
	}

	private void tag(String namespace, String name, String value) throws IOException {
		serializer.startTag(namespace, name);
		if (value != null) {
			serializer.text(value);
		}
		serializer.endTag(null, serializer.getName());
	}
}
