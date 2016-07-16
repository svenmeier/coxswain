package svenmeier.coxswain.view;

import svenmeier.coxswain.R;

/**
 */
public enum ValueBinding {

	DURATION(R.string.duration_label, R.string.duration_pattern),
	DURATION_SHORT(R.string.duration_label, R.string.duration_short_pattern),
	DISTANCE(R.string.distance_label, R.string.distance_pattern),
	STROKES(R.string.strokes_label, R.string.strokes_pattern),
	ENERGY(R.string.energy_label, R.string.energy_pattern),
	SPEED(R.string.speed_label, R.string.speed_pattern),
	PULSE(R.string.pulse_label, R.string.pulse_pattern),
	STROKE_RATE(R.string.strokeRate_label, R.string.strokeRate_pattern),
	STROKE_RATIO(R.string.strokeRatio_label, R.string.strokeRatio_pattern),
	TIME(R.string.time_label, R.string.time_pattern),
	DELTA_DURATION(R.string.delta_duration_label, R.string.delta_duration_pattern),
	DELTA_DISTANCE(R.string.delta_distance_label, R.string.delta_distance_pattern),
	NONE(R.string.none_label, R.string.none_pattern);

	public final int label;
	public final int pattern;

	ValueBinding(int label, int pattern) {
		this.label = label;
		this.pattern = pattern;
	}
}
