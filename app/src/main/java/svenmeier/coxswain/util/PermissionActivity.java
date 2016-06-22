package svenmeier.coxswain.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

/**
 */
public class PermissionActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] permissions = getIntent().getStringArrayExtra("permissions");

		ActivityCompat.requestPermissions(this, permissions, 1);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		for (int grantResult : grantResults) {
			if (grantResult != PackageManager.PERMISSION_GRANTED) {
				finish();
				return;
			}
		}

		// TODO report back to called
		finish();
	}

	public static void start(Context context, String[] permissions) {
		Intent intent = new Intent(context, PermissionActivity.class);

		// required for activity started from non-activity
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		intent.putExtra("permissions", permissions);

		context.startActivity(intent);
	}
}
