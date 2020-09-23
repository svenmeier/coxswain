package svenmeier.coxswain;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import propoid.db.Locator;
import propoid.db.locator.FileLocator;
import propoid.util.content.Preference;

/**
 */
class GymLocator implements Locator {

	private static final String NAME = "gym";

	private final Context context;

	private SQLiteDatabase database;

	/**
	 * Locate the database from the given context.
	 *
	 * @param context context
	 */
	public GymLocator(Context context) {
		this.context = context;
	}

	public SQLiteDatabase open() {
		if (database != null) {
			throw new IllegalStateException("already open");
		}

		if (Preference.getBoolean(context, R.string.preference_data_external).get()) {
			try {
				database = open(external());
			} catch (Exception ex) {
				Toast.makeText(context, R.string.gym_repository_extern_failed, Toast.LENGTH_LONG).show();
			}
		}

		if (database == null) {
			database = open(internal());
		}

		return database;
	}

	private File internal() {
		return context.getDatabasePath(NAME);
	}

	private File external() {
		return new File(Coxswain.getExternalFilesDir(context), NAME);
	}

	private SQLiteDatabase open(File file) {
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}

		return SQLiteDatabase.openOrCreateDatabase(file, null);
	}

	@Override
	public void close() {
		database.close();
		database = null;
	}
}