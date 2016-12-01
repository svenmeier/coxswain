package svenmeier.coxswain.util;

import android.location.Location;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import propoid.core.Property;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

/**
 * Converter for {@link svenmeier.coxswain.gym.Program}s.
 */
public class Program2Json {

	private JsonWriter writer;

	public Program2Json(Writer writer) throws IOException {
		this.writer = new JsonWriter(writer);
//		this.writer.setIndent("  ");
	}

	public void document(Program program) throws IOException {
		program(program);

		writer.flush();
	}

	private void program(Program program) throws IOException {
		writer.beginObject();

		writer.name("name").value(program.name.get());

		writer.name("segments");
		writer.beginArray();
		for (Segment segment : program.getSegments()) {
			segment(segment);
		}
		writer.endArray();

		writer.endObject();
	}

	private void segment(Segment segment) throws IOException {
		writer.beginObject();

		writer.name("difficulty").value(segment.difficulty.get().name());

		target("distance", segment.distance);
		target("duration", segment.duration);
		target("strokes", segment.strokes);
		target("energy", segment.energy);

		limit("speed", segment.speed);
		limit("strokeRate", segment.strokeRate);
		limit("pulse", segment.pulse);

		writer.endObject();
	}

	private void target(String name, Property<Integer> property) throws IOException {
		if (property.get() > 0) {
			writer.name(name).value(property.get());
		}
	}

	private void limit(String name, Property<Integer> property) throws IOException {
		if (property.get() > 0) {
			writer.name(name).value(property.get());
		}
	}
}
