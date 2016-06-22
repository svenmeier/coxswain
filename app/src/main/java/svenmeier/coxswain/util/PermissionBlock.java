package svenmeier.coxswain.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public abstract class PermissionBlock {

	private final Context context;

	public PermissionBlock(Context context) {
		this.context = context;
	}

	public void enter(String... permissions) {
		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(permissions);
				return;
			}
		}

		entered();
	}

	private void requestPermissions(String[] permissions) {
		if (context instanceof Activity) {
			ActivityCompat.requestPermissions((Activity) context, permissions, 1);
		} else {
			PermissionActivity.start(context, permissions);
		}
	}

	protected abstract void entered();
}
