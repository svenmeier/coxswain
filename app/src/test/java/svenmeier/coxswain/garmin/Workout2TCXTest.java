package svenmeier.coxswain.garmin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link Workout2TCX}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class)
public class Workout2TCXTest {

	private static final long Mon_Jun_15_2015 = 1434326400000l;

	@Test
	public void empty() throws IOException {

		Workout workout = new Workout();
		workout.start.set(Mon_Jun_15_2015);
		workout.duration.set(2);
		workout.distance.set(6);
		workout.strokes.set(2);
		workout.energy.set(3);

		List<Snapshot> snapshots = new ArrayList<>();

		StringWriter writer = new StringWriter();

		new Workout2TCX(writer, new StationaryCourse(null)).document(workout, snapshots);

		assertContent(getClass().getResourceAsStream("/empty.tcx"), writer.toString());
	}

	@Test
	public void snapshots() throws IOException {
		Program program = new Program();
		program.name.set("Test Program");

		Workout workout = new Workout();
		workout.program.set(program);
		workout.start.set(Mon_Jun_15_2015 + (60 * 1000));
		workout.duration.set(2);
		workout.distance.set(6);
		workout.strokes.set(2);
		workout.energy.set(3);

		List<Snapshot> snapshots = new ArrayList<>();

		Snapshot snapshot = new Snapshot();
		snapshot.speed.set(4_50);
		snapshot.pulse.set(80);
		snapshot.strokeRate.set(25);
		snapshot.distance.set(2);
		snapshot.strokes.set(0);
		snapshot.power.set(0);
		snapshots.add(snapshot);

		snapshot = new Snapshot();
		snapshot.speed.set(4_51);
		snapshot.pulse.set(81);
		snapshot.strokeRate.set(26);
		snapshot.distance.set(4);
		snapshot.strokes.set(1);
		snapshot.power.set(1);
		snapshots.add(snapshot);

		snapshot = new Snapshot();
		snapshot.speed.set(4_52);
		snapshot.pulse.set(82);
		snapshot.strokeRate.set(27);
		snapshot.distance.set(6);
		snapshot.strokes.set(2);
		snapshot.power.set(2);
		snapshots.add(snapshot);

		StringWriter writer = new StringWriter();

		new Workout2TCX(writer, new StationaryCourse(null)).document(workout, snapshots);

		assertContent(getClass().getResourceAsStream("/snapshots.tcx"), writer.toString());
	}

	private void assertContent(InputStream input, String actual) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

		StringBuilder content = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			if (content.length() > 0) {
				content.append("\r\n");
			}
			content.append(line);
		}

		assertEquals(content.toString(), actual);
	}
}