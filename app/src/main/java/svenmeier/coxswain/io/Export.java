package svenmeier.coxswain.io;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;

/**
 */
public abstract class Export<T> {

	protected final Context context;

	protected Export(Context context) {
		this.context = context;
	}

	public abstract void start(T t, boolean automatic);

	/**
	 * Start an automatic export for the given {@link Workout}.
	 *
	 * @param context context
	 * @param workout workout
	 */
	public static void start(Context context, Workout workout) {
		Preference<Boolean> auto = Preference.getBoolean(context, R.string.preference_export_auto);
		if (auto.get()) {
			Preference<String> last = Preference.getString(context, R.string.preference_export_last);

			Export<Workout> export;

			String name = last.get();
			try {
				export = (Export) Class.forName(name).getConstructor(Context.class).newInstance(context);
			} catch (Exception ex) {
				Toast.makeText(context, context.getString(R.string.preference_export_auto_reminder), Toast.LENGTH_LONG).show();
				return;
			}

			export.start(workout, true);
		}
	}

	/**
	 * Start a specific export for the given {@link Workout}, enabling it for any successive automatic
	 * export.
	 *
	 * @param context context
	 * @param workout workout
	 */
	public static void start(Context context, Export<Workout> export, Workout workout) {
		Preference<String> last = Preference.getString(context, R.string.preference_export_last);

		last.set(export.getClass().getName());

		export.start(workout, false);
	}

	/**
	 * Set a file on the given intent.
	 */
	public static void setFile(Context context, File file, Intent intent) {
		Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", file);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
	}
}
