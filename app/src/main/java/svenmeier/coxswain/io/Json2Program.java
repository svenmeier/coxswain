package svenmeier.coxswain.io;

import android.util.JsonReader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import propoid.core.Property;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;

/**
 * Converter for {@link Program}s.
 */
public class Json2Program {

	private JsonReader reader;

	public Json2Program(Reader reader) throws IOException {
		this.reader = new JsonReader(reader);
	}

	public Program program() throws IOException {
		Program program = new Program();

		reader.beginObject();

		require("name");
		program.name.set(reader.nextString());

		program.segments.set(new ArrayList<Segment>());
		require("segments");
		reader.beginArray();
		while (reader.hasNext()) {
			program.segments.get().add(segment());
		}

		reader.endArray();
		reader.endObject();

		return program;
	}

	private void require(String name) throws IOException {
		if (name.equals(reader.nextName()) == false) {
			throw new IOException("'" + name + "' expected");
		}
	}

	private Segment segment() throws IOException {
		Segment segment = new Segment();

		reader.beginObject();

		require("difficulty");

		segment.difficulty.set(Difficulty.valueOf(reader.nextString()));

		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "distance":
					segment.setDistance(reader.nextInt());
					break;
				case "duration":
					segment.setDuration(reader.nextInt());
					break;
				case "strokes":
					segment.setStrokes(reader.nextInt());
					break;
				case "energy":
					segment.setEnergy(reader.nextInt());
					break;
				case "speed":
					segment.setSpeed(reader.nextInt());
					break;
				case "strokeRate":
					segment.setStrokeRate(reader.nextInt());
					break;
				case "pulse":
					segment.setPulse(reader.nextInt());
					break;
				case "power":
					segment.setPower(reader.nextInt());
					break;
			}
		}

		reader.endObject();

		return segment;
	}
}
