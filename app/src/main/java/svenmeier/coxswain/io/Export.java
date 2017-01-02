package svenmeier.coxswain.io;

import svenmeier.coxswain.gym.Workout;

/**
 * Created by sven on 27.05.16.
 */
public interface Export<T> {

	void start(T t);
}
