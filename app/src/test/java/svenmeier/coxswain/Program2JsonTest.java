package svenmeier.coxswain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.io.Program2Json;

import static junit.framework.Assert.assertEquals;

/**
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class)
public class Program2JsonTest {

	@Test
	public void test() throws IOException {

		Program program = new Program("Test");

		Segment segment1 = new Segment(Difficulty.HARD);
		segment1.setDuration(60);
		program.addSegment(segment1);

		Writer writer = new StringWriter();

		new Program2Json(writer).document(program);

		String actual = writer.toString().replaceAll("[\\s]", "");

		assertEquals("{\"name\":\"Test\",\"segments\":[{\"difficulty\":\"EASY\",\"distance\":1000},{\"difficulty\":\"HARD\",\"duration\":60}]}", actual);
	}
}
