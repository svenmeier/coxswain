package svenmeier.coxswain.view;

import svenmeier.coxswain.R;

/**
 */
public enum ValueBinding {

	DURATION(R.string.value_duration, R.string.pattern_duration),
	DURATION_SHORT(R.string.value_duration, R.string.pattern_duration_short),
	DISTANCE(R.string.value_distance, R.string.pattern_distance),
	STROKES(R.string.value_strokes, R.string.pattern_strokes),
	ENERGY(R.string.value_energy, R.string.pattern_energy),
	SPEED(R.string.value_speed, R.string.pattern_speed),
	PULSE(R.string.value_pulse, R.string.pattern_pulse),
	STROKE_RATE(R.string.value_strokeRate, R.string.pattern_strokeRate),
	STROKE_RATIO(R.string.value_strokeRatio, R.string.pattern_strokeRatio),
	NONE(R.string.value_none, R.string.pattern_none);

	public final int label;
	public final int pattern;

	ValueBinding(int label, int pattern) {
		this.label = label;
		this.pattern = pattern;
	}
}
