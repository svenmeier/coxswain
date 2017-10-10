package svenmeier.coxswain.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

/**
 * Works with calendars.
 */
public class Calendar {

	private Context context;

	public Calendar(Context context) {
		this.context = context;
	}

	/**
	 * Get the id of the default calendar.
	 */
	public int getDefaultId() {
		ContentResolver resolver = context.getContentResolver();

		String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY};
		String selection = CalendarContract.Calendars.IS_PRIMARY + " = 1";

		Cursor cursor = resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null);
		try {
			if (cursor.moveToNext()) {
				return cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID));
			}
		} finally {
			cursor.close();
		}

		throw new RuntimeException("no default calendar");
	}

	/**
	 * Insert an event.
	 *
	 * @param event
	 */
	public void insert(ContentValues event) {
		ContentResolver content = context.getContentResolver();

		content.insert(CalendarContract.Events.CONTENT_URI, event);
	}
}
