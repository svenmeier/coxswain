package svenmeier.coxswain.io;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.TimeZone;

import propoid.db.mapping.LocationMapper;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.rower.Distance;
import svenmeier.coxswain.rower.Energy;
import svenmeier.coxswain.util.PermissionBlock;

/**
 */
public class CalendarExport extends Export<Workout> {

	private Handler handler = new Handler();

	public CalendarExport(Context context) {
		super(context);
	}

	@Override
	public void start(Workout workout) {
		new Writing(workout);
	}

	private class Writing extends PermissionBlock implements Runnable {

		private final Workout workout;

		public Writing(Workout workout) {
			super(context);

			this.workout = workout;

			acquirePermissions(Manifest.permission.WRITE_CALENDAR);
		}

		/**
		 * Falls back to intent.
		 */
		@Override
		protected void onRejected() {
			Intent intent = new Intent(Intent.ACTION_INSERT);
			intent.setData(CalendarContract.Events.CONTENT_URI);

			intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, workout.start.get());
			intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, workout.start.get() + workout.duration.get() * 1000);

			if (workout.location.get() != null) {
				intent.putExtra(CalendarContract.Events.EVENT_LOCATION, LocationMapper.toString(workout.location.get(), Location.FORMAT_DEGREES, ','));
			}

			intent.putExtra(CalendarContract.Events.TITLE, getTitle());

			String description = getDescription();
			intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

			context.startActivity(intent);
		}

		@Override
		protected void onPermissionsApproved() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			Calendar calendars = new Calendar(context);

			try {
				int calendar = calendars.getDefaultId();

				ContentValues event = new ContentValues();
				event.put(CalendarContract.Events.DTSTART, workout.start.get());
				event.put(CalendarContract.Events.DTEND, workout.start.get() + (workout.duration.get() * 1000));
				event.put(CalendarContract.Events.TITLE, getTitle());
				event.put(CalendarContract.Events.DESCRIPTION, getDescription());
				event.put(CalendarContract.Events.CALENDAR_ID, calendar);
				event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
				if (workout.location.get() != null) {
					event.put(CalendarContract.Events.EVENT_LOCATION, LocationMapper.toString(workout.location.get(), Location.FORMAT_DEGREES, ','));
				}
				calendars.insert(event);

				toast(context.getString(R.string.calendar_export_finished));
			} catch (Exception ex) {
				Log.e(Coxswain.TAG, "export failed", ex);
				toast(context.getString(R.string.calendar_export_failed));
			}
		}

		public String getTitle() {
			return context.getString(R.string.app_name) + ": " + workout.programName("-");
		}

		public String getDescription() {
			return TextUtils.join("\n", new String[]{
					String.format(context.getString(R.string.duration_minutes), workout.duration.get() / 60),
					Distance.m(context, workout.distance.get()).formatted(),
					Energy.kcal(context, workout.energy.get()).formatted(),
					String.format(context.getString(R.string.strokes_count), workout.strokes.get())
					// new Reference(workout).toString() // propoid URI is not recognized as link as 'http://' is :/
			});
		}
	}

	private void toast(final String text) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, text, Toast.LENGTH_LONG).show();
			}
		});
	}

}