package svenmeier.coxswain.io;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.provider.CalendarContract;
import android.text.TextUtils;

import propoid.db.mapping.LocationMapper;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;
import svenmeier.coxswain.rower.Distance;
import svenmeier.coxswain.rower.Energy;

/**
 */
public class CalendarExport extends Export<Workout> {

	public CalendarExport(Context context) {
		super(context);
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

		intent.putExtra(CalendarContract.Events.TITLE, context.getString(R.string.app_name) + ": " + workout.programName("-"));

		String description = TextUtils.join("\n", new String[]{
				String.format(context.getString(R.string.duration_minutes), workout.duration.get() / 60),
				Distance.m(context, workout.distance.get()).formatted(),
				Energy.kcal(context, workout.energy.get()).formatted(),
				String.format(context.getString(R.string.strokes_count), workout.strokes.get())
				// new Reference(workout).toString() // propoid URI is not recognized as link as 'http://' is :/
		});
		intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

		context.startActivity(intent);
	}
}
