package svenmeier.coxswain.io;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.garmin.TcxImport;

/**
 */
public class ImportIntention {

	private final Activity activity;

	public ImportIntention(Activity activity) {
		this.activity = activity;
	}

	public boolean onIntent(Intent intent) {
		try {
			Uri uri;
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				uri = intent.getData(); // ACTION_VIEW
			} else if (Intent.ACTION_SEND.equals(intent.getAction())) {
				uri = intent.getParcelableExtra(Intent.EXTRA_STREAM); // ACTION_SEND
			} else {
				return false;
			}

			String extension = getExtension(uri);
			if ("tcx".equalsIgnoreCase(extension)) {
				new TcxImport(activity).start(uri);
				return true;
			} else if ("coxswain".equalsIgnoreCase(extension)) {
				new ProgramImport(activity).start(uri);
				return true;
			}
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, ex.getMessage());
		}

		return false;
	}

	private String getExtension(Uri uri) {
		String name = getFileName(uri);

		int dot = name.lastIndexOf('.');
		return name.substring(dot + 1);
	}

	private String getFileName(Uri uri) {
		Cursor cursor = activity.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

		if (cursor == null) {
			return uri.getLastPathSegment();
		} else {
			try {
				int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				cursor.moveToFirst();
				return cursor.getString(nameIndex);
			} finally {
				cursor.close();
			}
		}
	}
}