package svenmeier.coxswain;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

/**
 */
public class Coxswain extends Application {

	public static String TAG = "coxswain";

	private Gym gym;

	public static void initNotification(Context context, Notification.Builder builder, String name) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);

		builder.setSmallIcon(R.drawable.notification);
		builder.setContentTitle(context.getString(R.string.app_name));
		builder.setDefaults(Notification.DEFAULT_VIBRATE);
		builder.setPriority(Notification.PRIORITY_DEFAULT);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(name.toLowerCase(),
					name, NotificationManager.IMPORTANCE_DEFAULT);
			channel.enableVibration(true);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

			notificationManager.createNotificationChannel(channel);
			builder.setChannelId(channel.getId());
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			builder.setVisibility(Notification.VISIBILITY_PUBLIC);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		gym = Gym.instance(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			CompactService.setup(this.getApplicationContext());
		}
	}
}
