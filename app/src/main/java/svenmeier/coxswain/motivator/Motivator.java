package svenmeier.coxswain.motivator;

import svenmeier.coxswain.Event;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;

/**
 * Created by sven on 01.10.15.
 */
public interface Motivator {

	void onEvent(Event event);

	void destroy();
}
