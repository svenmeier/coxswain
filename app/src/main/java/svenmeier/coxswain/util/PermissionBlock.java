package svenmeier.coxswain.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import java.util.Arrays;

public class PermissionBlock {

	private final Context context;

	private String[] permissions;

	private BroadcastReceiverImpl receiver;

	public PermissionBlock(Context context) {
		this.context = context;
	}

	public void acquirePermissions(String... permissions) {
		unregister();

		this.permissions = permissions;

		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions();
				return;
			}
		}

		onPermissionsApproved();
	}

	protected final void abortPermissions() {
		unregister();
	}

	protected void onPermissionsApproved() {
	}

	protected void onRejected() {
	}

	private void requestPermissions() {

		IntentFilter filter = PermissionActivity.start(context, permissions);

		receiver = new BroadcastReceiverImpl();
		context.registerReceiver(receiver, filter);
	}

	private void unregister() {
		if (receiver != null) {
			context.unregisterReceiver(receiver);
			receiver = null;
		}
	}

	private class BroadcastReceiverImpl extends BroadcastReceiver {
		@Override
		public final void onReceive(Context context, Intent intent) {

			String[] permissions = intent.getStringArrayExtra(PermissionActivity.PERMISSIONS);
			if (Arrays.equals(PermissionBlock.this.permissions, permissions) == false) {
				return;
			}

			unregister();

			boolean granted = intent.getBooleanExtra(PermissionActivity.GRANTED, false);
			if (granted) {
				onPermissionsApproved();
			} else {
				onRejected();
			}
		}
	}
}