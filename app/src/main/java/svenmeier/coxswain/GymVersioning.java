package svenmeier.coxswain;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import propoid.db.SQL;
import propoid.db.Setting;
import propoid.db.Versioning;
import propoid.db.version.DefaultVersioning;
import propoid.db.version.Upgrade;

/**
 */
class GymVersioning extends DefaultVersioning {

	GymVersioning() {
		add(new WrongIndices());
	}

	/**
	 * Index names where bogus, thus they were recreated on each start :/.
	 * Let's drop them all.
	 */
	private class WrongIndices implements Upgrade {
		@Override
		public void apply(SQLiteDatabase database) {
			Cursor indices = database.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index'", new String[0]);
			try {
				while (indices.moveToNext()) {
					String name = indices.getString(0);

					SQL drop = new SQL();
					drop.raw("DROP INDEX ");
					drop.escaped(name);
					database.execSQL(drop.toString());
				}
			} finally {
				indices.close();
			}
		}
	}
}
