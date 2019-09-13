package svenmeier.coxswain.motivator;

import svenmeier.coxswain.Event;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.gym.Measurement;

/**
 * Created by sven on 01.10.15.
 */
public interface Motivator {

	void onEvent(Event event, Measurement measurement, Gym.Progress progress);

	void destroy();
}
