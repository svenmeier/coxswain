package svenmeier.coxswain.rower.water;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 */
public class NullWriter extends Writer {

	@Override
	public void close() throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void write(char[] buf, int offset, int count) throws IOException {
	}
}
