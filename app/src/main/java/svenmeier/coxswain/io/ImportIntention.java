package svenmeier.coxswain.io;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.garmin.TcxImport;

/**
 */
public class ImportIntention {

	private final Activity activity;

	public ImportIntention(Activity activity) {
		this.activity = activity;
	}


	public boolean onIntent(Intent intent) {
		Uri uri;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			uri = intent.getData();
		} else if (Intent.ACTION_SEND.equals(intent.getAction())) {
			uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		} else {
			return false;
		}

		return importFrom(uri);
	}

	private boolean importFrom(Uri uri) {
		Import<?> importer = null;

		try {
			String name = getFileName(uri);
			int dot = name.lastIndexOf('.');
			String extension = name.substring(dot + 1);

			if ("tcx".equalsIgnoreCase(extension)) {
				importer = new TcxImport(activity);
			} else if ("coxswain".equalsIgnoreCase(extension)) {
				importer = new ProgramImport(activity);
			}
		} catch (Exception ex) {
			Log.e(Coxswain.TAG, ex.getMessage());
		}

		if (importer == null) {
			Toast.makeText(activity, R.string.import_unknown, Toast.LENGTH_LONG).show();
			return false;
		}

		importer.start(uri);
		return true;
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