package svenmeier.coxswain.view;

import android.content.Context;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;

/**
 */
public enum ValueBinding {

	DURATION(R.string.duration_label, R.string.duration_pattern),
	DISTANCE(R.string.distance_label, R.string.distance_pattern),
	STROKES(R.string.strokes_label, R.string.strokes_pattern),
	ENERGY(R.string.energy_label, R.string.energy_pattern),
	SPEED(R.string.speed_label, R.string.speed_pattern),
	PULSE(R.string.pulse_label, R.string.pulse_pattern),
	STROKE_RATE(R.string.strokeRate_label, R.string.strokeRate_pattern),
	STROKE_RATIO(R.string.strokeRatio_label, R.string.strokeRatio_pattern),
	TIME(R.string.time_label, R.string.time_pattern),
	SPLIT(R.string.split_label, R.string.split_pattern),
	AVERAGE_SPLIT(R.string.average_split_label, R.string.average_split_pattern),
	DELTA_DURATION(R.string.delta_duration_label, R.string.delta_duration_pattern),
	DELTA_DISTANCE(R.string.delta_distance_label, R.string.delta_distance_pattern),
	NONE(R.string.none_label, R.string.none_pattern);

	public final int label;
	public final int pattern;

	ValueBinding(int label, int pattern) {
		this.label = label;
		this.pattern = pattern;
	}

	public String format(Context context, int value, boolean arabic) {
		return format(context, value, arabic, false);
	}

	public String format(Context context, int value, boolean arabic, boolean signed) {
		StringBuilder text = new StringBuilder();

		int digits = Math.abs(value);
		String pattern = context.getString(this.pattern);

		for (int c = pattern.length() - 1; c >= 0; c--) {
			char character = pattern.charAt(c);

			if ('0' == character) {
				// decimal
				text.append(toChar(digits % 10, arabic));

				digits /= 10;
			} else if ('6' == character) {
				// minutes or hours
				text.append(toChar(digits % 6, arabic));

				digits /= 6;
			} else if ('F' == character) {
				// hexadecimal
				text.append(Integer.toHexString(digits % 0xF));

				digits /= 0xF;
			} else if ('-' == character){
				if (value < 0) {
					text.append("-");
				} else if (signed) {
					text.append("+");
				}
			} else {
				text.append(character);
			}
		}

		text.reverse();

		return text.toString();
	}

	private char toChar(int digit, boolean arabic) {
		if (arabic) {
			return (char)(0x660 + digit);
		} else {
			return (char)('0' + digit);
		}
	}
}
