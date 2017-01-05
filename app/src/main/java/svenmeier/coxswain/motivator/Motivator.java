package svenmeier.coxswain.motivator;

import svenmeier.coxswain.Event;

/**
 * Created by sven on 01.10.15.
 */
public interface Motivator {

	void onEvent(Event event);

	void destroy();
}
