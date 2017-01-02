package svenmeier.coxswain.io;

import android.net.Uri;

/**
 * Created by sven on 27.05.16.
 */
public interface Import<T> {

	void start(Uri uri);
}
