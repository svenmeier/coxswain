package svenmeier.coxswain.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import propoid.ui.list.GenericRecyclerAdapter;
import propoid.util.content.Preference;
import svenmeier.coxswain.R;

public class BluetoothActivity extends AppCompatActivity {

	private static final long TIMEOUT = 60000;

	private RecyclerView devicesView;

	private DevicesAdapter devicesAdapter;

	private Scanning scanning = new Scanning();

	private List<Scanned> scanned = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setFinishOnTouchOutside(false);
		
		setContentView(R.layout.layout_bluetooth);

		devicesView = findViewById(R.id.bluetooth_devices);
		devicesView.setLayoutManager(new LinearLayoutManager(this));
		devicesView.setHasFixedSize(true);

		devicesView.setAdapter(devicesAdapter = new DevicesAdapter());

		scanning.start();
	}

	@Override
	protected void onDestroy() {
		scanning.stop();

		super.onDestroy();
	}

	private class DevicesAdapter extends GenericRecyclerAdapter<Scanned> {

		public DevicesAdapter() {
			super(R.layout.layout_bluetooth_devices_item, scanned);
		}

		@Override
		protected GenericHolder createHolder(View v) {
			return new ScannedHolder(v);
		}
	}

	private class ScannedHolder extends GenericRecyclerAdapter.GenericHolder<Scanned> implements View.OnClickListener {

		private final TextView nameView;
		private final TextView addressView;

		public ScannedHolder(View v) {
			super(v);

			nameView = v.findViewById(R.id.scanned_name);
			addressView = v.findViewById(R.id.scanned_address);

			v.setOnClickListener(this);
		}

		@Override
		protected void onBind() {
			nameView.setText(item.name);
			addressView.setText(item.address);
		}

		@Override
		public void onClick(View view) {
			onSelect(item.address);
		}
	}

	private void onSelect(String address) {
		Preference.getString(this, R.string.preference_bluetooth_preferred).set(address);

		finish();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Scanning implements BluetoothAdapter.LeScanCallback {

		private BluetoothAdapter adapter;

		public void start() {
			BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();
			adapter.startLeScan(this);
		}

		private void stop() {
			adapter.stopLeScan(this);

			adapter = null;
		}

		@Override
		public synchronized void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
			if (adapter == null) {
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Scanned newest = new Scanned(device.getName(), device.getAddress());

					int index = scanned.indexOf(newest);
					if (index == -1) {
						scanned.add(newest);

						devicesAdapter.notifyItemInserted(scanned.size() - 1);
						devicesView.requestLayout();
					} else {
						scanned.set(index, newest);
					}

					for (index = 0; index < scanned.size(); index++) {
						Scanned old = scanned.get(index);

						if (newest.when - old.when > TIMEOUT) {
							scanned.remove(index);
							devicesAdapter.notifyItemRemoved(index);
						}
					}
				}
			});
		}
	}

	private class Scanned {

		public final String address;
		public final String name;
		public long when;

		private Scanned(String name, String address) {
			this.name = (name == null ? "N/A" : name);
			this.address = address;

			when = System.currentTimeMillis();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null) {
				return ((Scanned)obj).address.equals(this.address);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return address.hashCode();
		}
	}

	public static void start(Context context) {
		Intent intent = new Intent(context, BluetoothActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}