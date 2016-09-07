package svenmeier.coxswain.calendar;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.provider.CalendarContract;
import android.text.TextUtils;

import propoid.db.Reference;
import propoid.db.mapping.LocationMapper;
import svenmeier.coxswain.Export;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;

/**
 */
public class CalendarExport implements Export {

	private final Activity activity;

	public CalendarExport(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void start(Workout workout) {
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.setData(CalendarContract.Events.CONTENT_URI);

		intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, workout.start.get());
		intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, workout.start.get() + workout.duration.get() * 1000);

		if (workout.location.get() != null) {
			intent.putExtra(CalendarContract.Events.EVENT_LOCATION, LocationMapper.toString(workout.location.get(), Location.FORMAT_DEGREES, ','));
		}

		intent.putExtra(CalendarContract.Events.TITLE, activity.getString(R.string.app_name) + ": " + workout.name("-"));

		String description = TextUtils.join("\n", new String[]{
				String.format(activity.getString(R.string.duration_minutes), workout.duration.get() / 60),
				String.format(activity.getString(R.string.distance_meters), workout.distance.get()),
				String.format(activity.getString(R.string.strokes_count), workout.strokes.get()),
				String.format(activity.getString(R.string.energy_calories), workout.energy.get())
				// new Reference(workout).toString() // propoid URI is not recognized as link as 'http://' is :/
		});
		intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

		activity.startActivity(intent);
	}
}
