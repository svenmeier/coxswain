package svenmeier.coxswain.util;

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
					segment.distance.set(reader.nextInt());
					break;
				case "duration":
					segment.duration.set(reader.nextInt());
					break;
				case "strokes":
					segment.strokes.set(reader.nextInt());
					break;
				case "energy":
					segment.energy.set(reader.nextInt());
					break;
				case "speed":
					segment.speed.set(reader.nextInt());
					break;
				case "strokeRate":
					segment.strokeRate.set(reader.nextInt());
					break;
				case "pulse":
					segment.pulse.set(reader.nextInt());
					break;
			}
		}

		reader.endObject();

		return segment;
	}
}
