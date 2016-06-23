package svenmeier.coxswain.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.util.Arrays;

public abstract class PermissionBlock extends BroadcastReceiver {

	private final Context context;

	private String[] permissions;

	private boolean registered;

	public PermissionBlock(Context context) {
		this.context = context;
	}

	protected void acquire(String... permissions) {
		unregister();

		this.permissions = permissions;

		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions();
				return;
			}
		}

		onApproved();
	}

	protected final void cancel() {
		unregister();
	}

	protected abstract void onApproved();

	protected void onRejected() {
	}

	private void requestPermissions() {

		IntentFilter filter = new IntentFilter();
		filter.addAction(PermissionActivity.ACTION);
		context.registerReceiver(this, filter);
		registered = true;

		PermissionActivity.start(context, permissions);
	}

	@Override
	public final void onReceive(Context context, Intent intent) {

		String[] permissions = intent.getStringArrayExtra(PermissionActivity.PERMISSIONS);
		if (Arrays.equals(this.permissions, permissions) == false) {
			return;
		}

		unregister();

		boolean granted = intent.getBooleanExtra(PermissionActivity.GRANTED, false);
		if (granted) {
			onApproved();
		} else {
			onRejected();
		}
	}

	private void unregister() {
		if (registered) {
			context.unregisterReceiver(this);
			registered = false;
		}
	}
}