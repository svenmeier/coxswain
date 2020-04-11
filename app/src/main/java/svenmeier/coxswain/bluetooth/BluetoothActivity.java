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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import propoid.ui.list.GenericRecyclerAdapter;
import svenmeier.coxswain.R;

public class BluetoothActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

	private static final String ACTION_SELECTED = "svenmeier.coxswain.bluetooth.SELECTED";

	private static final String ACTION_CANCEL = "svenmeier.coxswain.bluetooth.CANCEL";

	private static final String NAME = "name";

	private static final String SERVICE_FILTER = "filter";

	public static final String DEVICE_ADDRESS = "address";

	public static final String DEVICE_REMEMBER = "remember";

	private static final long TIMEOUT = 60000;

	private String serviceFilter;

	private CheckBox filterCheckBox;

	private CheckBox rememberCheckBox;

	private RecyclerView devicesView;

	private DevicesAdapter devicesAdapter;

	private IntentReceiver watcher = new IntentReceiver();

	private Scanning scanning;

	private List<ScannedDevice> scannedDevices = new ArrayList<>();

	private String selected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setFinishOnTouchOutside(false);
		
		setContentView(R.layout.layout_bluetooth);

		String name = getIntent().getStringExtra(NAME);
		setTitle(getString(R.string.bluetooth_choose, name));

		filterCheckBox = findViewById(R.id.bluetooth_filter);
		filterCheckBox.setText(getString(R.string.bluetooth_choose_filtered, name));
		filterCheckBox.setOnCheckedChangeListener(this);

		rememberCheckBox = findViewById(R.id.bluetooth_remember);
		rememberCheckBox.setChecked(false);

		devicesView = findViewById(R.id.bluetooth_devices);
		devicesView.setLayoutManager(new LinearLayoutManager(this));
		devicesView.setHasFixedSize(true);

		devicesView.setAdapter(devicesAdapter = new DevicesAdapter());

		serviceFilter = getIntent().getStringExtra(SERVICE_FILTER);

		watcher.register();

		startScanning();
	}

	@Override
	protected void onDestroy() {
		stopScanning();

		watcher.unregister();

		if (selected == null) {
			select(null);
		}

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

	private void select(String address) {
		selected = address;

		Intent intent = new Intent();
		intent.setAction(ACTION_SELECTED);
		intent.putExtra(SERVICE_FILTER, serviceFilter);
		intent.putExtra(DEVICE_ADDRESS, address);
		intent.putExtra(DEVICE_REMEMBER, rememberCheckBox.isChecked());
		sendBroadcast(intent);
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
			// stop immediately, otherwise following connection might fail
			stopScanning();

			select(item.address);

			finish();
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

			if (filterCheckBox.isChecked() && serviceFilter != null) {
				adapter.startLeScan(new UUID[]{UUID.fromString(serviceFilter)}, this);
			} else {
				adapter.startLeScan(this);
			}
		}

		@Override
		public void stop() {
			try {
				adapter.stopLeScan(this);
			} catch (Exception bluetoothAlreadyOff) {
			}
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
			if (filterCheckBox.isChecked() && serviceFilter != null) {
				ScanFilter heartRateOnly = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceFilter)).build();
				filters.add(heartRateOnly);
			}
			scanner.startScan(filters, new ScanSettings.Builder().build(), this);
		}

		@Override
		public void stop() {
			try {
				scanner.stopScan(this);
			} catch (Exception bluetoothAlreadyOff) {
			}
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

	private class IntentReceiver extends BroadcastReceiver {

		public void register() {
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			filter.addAction(LocationManager.MODE_CHANGED_ACTION);
			filter.addAction(ACTION_CANCEL);
			registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
				if (state == BluetoothAdapter.STATE_OFF) {
					finish();
				}
			} else if (ACTION_CANCEL.equals(intent.getAction())) {
				finish();
			}
		}

		public void unregister() {
			unregisterReceiver(this);
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

	public static IntentFilter start(Context context, String name, String serviceFilter) {
		Intent intent = new Intent(context, BluetoothActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(NAME, name);
		intent.putExtra(SERVICE_FILTER, serviceFilter);
		context.startActivity(intent);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothActivity.ACTION_SELECTED);
		return filter;
	}

	public static void cancel(Context context) {
		Intent intent = new Intent();
		intent.setAction(ACTION_CANCEL);
		context.sendBroadcast(intent);

	}
}