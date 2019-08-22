package svenmeier.coxswain.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import propoid.ui.list.GenericRecyclerAdapter;
import propoid.util.content.Preference;
import svenmeier.coxswain.R;

public class BluetoothActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

	private static final long TIMEOUT = 60000;

	private CheckBox filterCheckBox;

	private RecyclerView devicesView;

	private DevicesAdapter devicesAdapter;

	private Scanning scanning;

	private List<ScannedDevice> scannedDevices = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setFinishOnTouchOutside(false);
		
		setContentView(R.layout.layout_bluetooth);

		filterCheckBox = findViewById(R.id.bluetooth_filter);
		filterCheckBox.setOnCheckedChangeListener(this);

		devicesView = findViewById(R.id.bluetooth_devices);
		devicesView.setLayoutManager(new LinearLayoutManager(this));
		devicesView.setHasFixedSize(true);

		devicesView.setAdapter(devicesAdapter = new DevicesAdapter());

		startScanning();
	}

	@Override
	protected void onDestroy() {
		stopScanning();

		super.onDestroy();
	}

	private void startScanning() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			scanning = new NewScanning();
		} else {
			scanning = new OldScanning();
		}
		scanning.start();
	}

	private void stopScanning() {
		if (scanning != null) {
			scanning.stop();
			scanning = null;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		stopScanning();

		scannedDevices.clear();
		devicesAdapter.notifyDataSetChanged();

		startScanning();
	}

	private void onSelect(String address) {
		// stop immediately, otherwise following connection might fail
		stopScanning();

		Preference.getString(this, R.string.preference_bluetooth_preferred).set(address);

		finish();
	}

	private class DevicesAdapter extends GenericRecyclerAdapter<ScannedDevice> {

		public DevicesAdapter() {
			super(R.layout.layout_bluetooth_devices_item, scannedDevices);
		}

		@Override
		protected GenericHolder createHolder(View v) {
			return new DeviceHolder(v);
		}
	}

	private class DeviceHolder extends GenericRecyclerAdapter.GenericHolder<ScannedDevice> implements View.OnClickListener {

		private final TextView nameView;
		private final TextView addressView;

		public DeviceHolder(View v) {
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

	interface Scanning {
		void start();
		
		void stop();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class OldScanning implements BluetoothAdapter.LeScanCallback, Scanning {

		private BluetoothAdapter adapter;

		@Override
		public void start() {
			BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (filterCheckBox.isChecked()) {
				adapter.startLeScan(new UUID[]{BluetoothHeart.SERVICE_HEART_RATE}, this);
			} else {
				adapter.startLeScan(this);
			}
		}

		@Override
		public void stop() {
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
					onScanned(new ScannedDevice(device.getName(), device.getAddress()));
				}
			});
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private class NewScanning extends ScanCallback implements Scanning {

		private BluetoothLeScanner scanner;

		@Override
		public void start() {
			BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			scanner = manager.getAdapter().getBluetoothLeScanner();

			List<ScanFilter> filters = new ArrayList<>();
			if (filterCheckBox.isChecked()) {
				ScanFilter heartRateOnly = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BluetoothHeart.SERVICE_HEART_RATE.toString())).build();
				filters.add(heartRateOnly);
			}
			scanner.startScan(filters, new ScanSettings.Builder().build(), this);
		}

		@Override
		public void stop() {
			scanner.stopScan(this);
			scanner = null;
		}

		@Override
		public void onScanResult(int callbackType, final ScanResult result) {
			if (scanner == null) {
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					BluetoothDevice device = result.getDevice();

					onScanned(new ScannedDevice(device.getName(), device.getAddress()));
				}
			});
		}
	}

	private void onScanned(ScannedDevice newest) {
		int index = scannedDevices.indexOf(newest);
		if (index == -1) {
			scannedDevices.add(newest);

			devicesAdapter.notifyItemInserted(scannedDevices.size() - 1);
			devicesView.requestLayout();
		} else {
			scannedDevices.set(index, newest);
		}

		for (index = 0; index < scannedDevices.size(); index++) {
			ScannedDevice old = scannedDevices.get(index);

			if (newest.when - old.when > TIMEOUT) {
				scannedDevices.remove(index);
				devicesAdapter.notifyItemRemoved(index);
			}
		}
	}

	private class ScannedDevice {

		public final String address;
		public final String name;
		public long when;

		private ScannedDevice(String name, String address) {
			this.name = (name == null ? "N/A" : name);
			this.address = address;

			when = System.currentTimeMillis();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null) {
				return ((ScannedDevice)obj).address.equals(this.address);
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