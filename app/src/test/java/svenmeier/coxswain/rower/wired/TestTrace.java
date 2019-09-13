package svenmeier.coxswain.rower.wired;

import static org.junit.Assert.assertEquals;

/**
 */
class TestTrace implements ITrace {

	public StringBuilder result = new StringBuilder();

	@Override
	public void comment(CharSequence string) {
		result.append('#');
		result.append(string);
	}

	@Override
	public void onOutput(CharSequence string) {
		result.append('>');
		result.append(string);
	}

	@Override
	public void onInput(CharSequence string) {
		result.append('<');
		result.append(string);
	}

	@Override
	public void close() {

	}

	@Override
	public String toString() {
		return result.toString();
	}
}
