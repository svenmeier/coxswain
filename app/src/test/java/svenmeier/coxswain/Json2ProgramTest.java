package svenmeier.coxswain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.util.Json2Program;
import svenmeier.coxswain.util.Program2Json;

import static junit.framework.Assert.assertEquals;

/**
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = svenmeier.coxswain.BuildConfig.class, sdk = 18)
public class Json2ProgramTest {

	@Test
	public void test() throws IOException {

		Reader reader = new StringReader("{\"name\":\"Test\",\"segments\":[{\"difficulty\":\"EASY\",\"distance\":1000},{\"difficulty\":\"HARD\",\"duration\":60}]}");

		Program program = new Json2Program(reader).program();

		assertEquals("Test", program.name.get());
		assertEquals(2, program.segments.get().size());

		Segment segment0 = program.segments.get().get(0);
		assertEquals(Difficulty.EASY, segment0.difficulty.get());
		assertEquals(Integer.valueOf(1000), segment0.distance.get());

		Segment segment1 = program.segments.get().get(1);
		assertEquals(Difficulty.HARD, segment1.difficulty.get());
		assertEquals(Integer.valueOf(60), segment1.duration.get());
	}
}
