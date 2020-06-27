package svenmeier.coxswain.rower.wireless;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import java.util.ArrayDeque;

import propoid.util.content.Preference;
import svenmeier.coxswain.R;
import svenmeier.coxswain.bluetooth.BlueWriter;
import svenmeier.coxswain.bluetooth.BluetoothActivity;
import svenmeier.coxswain.bluetooth.Fields;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.util.ByteUtils;
import svenmeier.coxswain.util.PermissionBlock;

public class BluetoothRower extends Rower {

	private static final int CONNECT_TIMEOUT_MILLIS = 10000;

	/**
	 * Timeout after which we re-enable notifications.
	 */
	private static final int NOTIFICATIONS_TIMEOUT = 2000;

	private static byte OP_CODE_REQUEST_CONTROL = 0x00;
	private static byte OP_CODE_RESET = 0x01;

	private final Context context;

	private final Handler handler = new Handler();

	private final Preference<String> devicePreference;
	
	private ArrayDeque<Connection> connections = new ArrayDeque<>();

	private boolean resetting = false;

	private int elapsedTime;

	public BluetoothRower(Context context, Callback callback) {
		super(context, callback);

		this.context = context;

		devicePreference = Preference.getString(context, R.string.preference_bluetooth_rower_device);
	}

	@Override
	public void open() {
		if (connections.isEmpty()) {
			push(new Permissions());
		}
	}

	@Override
	public void reset() {
		resetting = true;

		Connection last = connections.peek();
		if (last instanceof GattConnection) {
			((GattConnection) last).reset();
		}

		super.reset();
	}

	private void fireDisconnected() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onDisconnected();
			}
		});
	}
	
	@Override
	public void close() {
		super.close();

		while (connections.isEmpty() == false) {
			pop();
		}
	}

	@Override
	public String getName() {
		return "Bluetooth Rower";
	}

	private void push(Connection connection) {
		this.connections.push(connection);
		
		connection.open();
	}

	private void pop() {
		this.connections.pop().close();
	}

	private interface Connection {

		void open();

		void close();
	}

	private class Permissions extends PermissionBlock implements Connection {

		public Permissions() {
			super(context);
		}

		@Override
		public void open() {
			acquirePermissions(Manifest.permission.ACCESS_FINE_LOCATION);
		}

		@Override
		public void close() {
			abortPermissions();
		}

		@Override
		protected void onPermissionsApproved() {
			push(new LocationServices());
		}
	}

	private class LocationServices extends BroadcastReceiver implements Connection {

		private boolean registered;

		public void open() {
			if (isRequired()) {
				if (isEnabled() == false) {
					toast(context.getString(R.string.bluetooth_rower_no_location));

					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);

					IntentFilter filter = new IntentFilter();
					filter.addAction(LocationManager.MODE_CHANGED_ACTION);
					context.registerReceiver(this, filter);
					registered = true;

					return;
				}
			}

			proceed();
		}

		/**
		 * Location services must be enabled for Apps built for M and running on M or later.
		 */
		private boolean isRequired() {
			boolean builtForM = context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.M;
			boolean runningOnM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

			return builtForM && runningOnM;
		}

		private boolean isEnabled() {
			LocationManager manager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(true);
			criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

			String provider = manager.getBestProvider(criteria, true);

			return provider != null && LocationManager.PASSIVE_PROVIDER.equals(provider) == false;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (connections.peek() == this && isEnabled()) {
				proceed();
			}
		}

		private void proceed() {
			push(new Bluetooth());
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class Bluetooth extends BroadcastReceiver implements Connection {

		private BluetoothAdapter adapter;

		private boolean registered;

		@Override
		public void open() {
			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
			adapter = manager.getAdapter();

			if (adapter == null) {
				toast(context.getString(R.string.bluetooth_rower_no_bluetooth));
				return;
			}

			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			filter.addAction(LocationManager.MODE_CHANGED_ACTION);
			context.registerReceiver(this, filter);
			registered = true;

			if (adapter.isEnabled() == false) {
				adapter.enable();
			} else {
				proceed();
			}
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				if (connections.peek() == this) {
					proceed();
				}
			} else if (state == BluetoothAdapter.STATE_OFF) {
				fireDisconnected();
			}
		}

		private void proceed() {
			push(new SelectionConnection());
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}

			adapter = null;
		}
	}

	private class SelectionConnection extends BroadcastReceiver implements Connection {

		boolean registered = false;

		@Override
		public void open() {
			String address = devicePreference.get();
			if (address != null) {
				proceed(address);
				return;
			}

			String name = context.getString(R.string.bluetooth_rower);
			IntentFilter filter = BluetoothActivity.start(context, name, BlueWriter.SERVICE_FITNESS_MACHINE.toString());
			context.registerReceiver(this, filter);
			registered = true;
		}

		@Override
		public final void onReceive(Context context, Intent intent) {
			if (connections.peek() == this) {
				String address = intent.getStringExtra(BluetoothActivity.DEVICE_ADDRESS);

				if (address == null) {
					fireDisconnected();
				} else {
					boolean remember = intent.getBooleanExtra(BluetoothActivity.DEVICE_REMEMBER, false);
					if (remember) {
						devicePreference.set(address);
					} else {
						devicePreference.set(null);
					}

					proceed(address);
				}
			}
		}

		private void proceed(String address) {
			push(new GattConnection(address));
		}

		@Override
		public void close() {
			if (registered) {
				context.unregisterReceiver(this);
				registered = false;
			}

			BluetoothActivity.cancel(context);
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private class GattConnection extends BlueWriter implements Connection, Runnable {

		private final String address;

		private KeepAlive keepAlive = new KeepAlive();

		private BluetoothAdapter adapter;

		private BluetoothGatt connected;

		private BluetoothGattCharacteristic softwareRevision;
		private BluetoothGattCharacteristic rowerData;
		private BluetoothGattCharacteristic controlPoint;

		GattConnection(String address) {
			this.address = address;
		}

		private void select() {
			devicePreference.set(null);

			handler.removeCallbacks(this);

			pop();
			pop();
			push(new SelectionConnection());
		}

		@Override
		public synchronized void open() {

			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			try {
				BluetoothDevice device = adapter.getRemoteDevice(address);

				trace.onOutput(String.format("connecting %s", address));
				connected = device.connectGatt(context, false, this);

				handler.postDelayed(this, CONNECT_TIMEOUT_MILLIS);
			} catch (IllegalArgumentException invalid) {
				select();
			}
		}

		@Override
		public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (adapter == null) {
				return;
			}

			String address = gatt.getDevice().getAddress();

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				trace.onInput(String.format("rower connected %s", address));

				connected = gatt;
				trace.onInput("discovering services");
				connected.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED ) {
				if (connected != null && connected.getDevice().getAddress().equals(address)) {
					trace.onInput(String.format("rower disconnected %s", address));

					if (adapter.isEnabled()) {
						toast(context.getString(R.string.bluetooth_rower_disconnected, address));

						if (rowerData == null) {
							// only select when no successful connection yet
							select();
							return;
						}
					}

					fireDisconnected();
				}
			}
		}

		public synchronized void close() {
			if (connected != null) {
				connected.disconnect();
				connected.close();
				connected = null;

				rowerData = null;
			}

			adapter = null;
		}

		public void reset() {
			if (connected != null && controlPoint != null) {
				trace.onOutput("control-point resetting");
				write(connected, controlPoint, OP_CODE_RESET);
			}
		}

		@Override
		public synchronized void onServicesDiscovered(final BluetoothGatt gatt, int status) {
			if (connected == null) {
				return;
			}

			trace.onInput("services discovered");

			rowerData = get(gatt, SERVICE_FITNESS_MACHINE, CHARACTERISTIC_ROWER_DATA);
			if (rowerData == null) {
				trace.comment("no rower-data");
			} else {
				trace.onOutput("rower-data enable notification");
				enableNotification(gatt, rowerData);
			}

			softwareRevision = get(gatt, SERVICE_DEVICE_INFORMATION, CHARACTERISTIC_SOFTWARE_REVISION);
			if (softwareRevision == null) {
				trace.comment("no software-revision");
			} else {
				trace.onOutput("reading software-revision");
				read(connected, softwareRevision);
			}

			if (rowerData == null)  {
				toast(context.getString(R.string.bluetooth_rower_not_found, gatt.getDevice().getAddress()));
				select();
			} else {
				handler.post(new Runnable() {
					@Override
					public void run() {
						callback.onConnected();
					}
				});
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (softwareRevision != null && characteristic.getUuid().equals(softwareRevision.getUuid())) {
				String version = softwareRevision.getStringValue(0);
				trace.onInput(String.format("software-revision %s", version));

				String minVersion = "4.2";
				if (version.compareTo(minVersion) < 0) {
					// old firmware rejects re-bonding of a previously bonded device,
					// so do not write to the control point, as this triggers a bond
				} else {
					controlPoint = get(gatt, SERVICE_FITNESS_MACHINE, CHARACTERISTIC_CONTROL_POINT);
					if (controlPoint == null) {
						trace.comment("no control-point");
					} else {
						trace.onOutput(String.format("control-point enabling indication"));
						enableIndication(connected, controlPoint);

						trace.onOutput(String.format("control-point requesting control"));
						write(connected, controlPoint, OP_CODE_REQUEST_CONTROL);

						if (resetting == true) {
							reset();
						}
					}
				}
			}

			super.onCharacteristicRead(gatt, characteristic, status);
		}

		@Override
		public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (rowerData == null) {
				return;
			}

			if (controlPoint != null && characteristic.getUuid().equals(controlPoint.getUuid())) {
				trace.onInput(String.format("control-point changed %s", ByteUtils.toHex(characteristic.getValue())));
			} else if (rowerData != null && characteristic.getUuid().equals(rowerData.getUuid())) {
				trace.onInput(String.format("rower-data changed %s", ByteUtils.toHex(characteristic.getValue())));

				keepAlive.onNotification();

				int duration = getDuration();
				int distance = getDistance();
				int strokes = getStrokes();
				int energy = getEnergy();

				Fields fields = new Fields(characteristic, Fields.UINT16);
				try {
					if (fields.flag(0) == false) { // more data
						setStrokeRate(fields.get(Fields.UINT8) / 2); // stroke rate 0.5
						strokes = fields.get(Fields.UINT16); // stroke count
					}
					if (fields.flag(1)) {
						fields.get(Fields.UINT8); // average stroke rate
					}
					if (fields.flag(2)) {
						distance = (fields.get(Fields.UINT16) +
								(fields.get(Fields.UINT8) << 16)); // total distance
					}
					if (fields.flag(3)) {
						setSpeed(500 * 100 / fields.get(Fields.UINT16)); // instantaneous pace
					}
					if (fields.flag(4)) {
						fields.get(Fields.UINT16); // average pace
					}
					if (fields.flag(5)) {
						setPower(fields.get(Fields.SINT16)); // instantaneous power
					}
					if (fields.flag(6)) {
						fields.get(Fields.SINT16); // average power
					}
					if (fields.flag(7)) {
						fields.get(Fields.SINT16); // resistance level
					}
					if (fields.flag(8)) { // expended energy
						energy = fields.get(Fields.UINT16); // total energy
						fields.get(Fields.UINT16); // energy per hour
						fields.get(Fields.UINT8); // energy per minute
					}
					if (fields.flag(9)) {
						int heartRate = fields.get(Fields.UINT8); // heart rate
						if (heartRate > 0) {
							setPulse(heartRate);
						}
					}
					if (fields.flag(10)) {
						fields.get(Fields.UINT8); // metabolic equivalent 0.1
					}
					if (fields.flag(11)) {
						duration += durationDelta(fields.get(Fields.UINT16)); // elapsed time
					}
					if (fields.flag(12)) {
						fields.get(Fields.UINT16); // remaining time
					}
				} catch (NullPointerException ex) {
					// rarely flags and fields do not match up
					trace.comment("field mismatch");
				}

				if (resetting) {
					if (distance + duration + energy + strokes == 0) {
						trace.comment("resetted");
						resetting = false;
					}
				} else {
					setDistance(distance);
					setDuration(duration);
					setStrokes(strokes);
					setEnergy(energy);
				}
				notifyMeasurement();
			}
		}

		/**
		 * @see BluetoothRower#CONNECT_TIMEOUT_MILLIS
		 */
		@Override
		public void run() {
			if (connected != null && rowerData == null) {
				trace.comment("connection timeout");

				toast(context.getString(R.string.bluetooth_rower_failed, connected.getDevice().getAddress()));

				select();
			}
		}

		/**
		 * Notifications from the S4 comm module time out every other minute,
		 * thus we re-enabled the notification when no new notification has
		 * been received within a timeout.
		 *
		 * @see #NOTIFICATIONS_TIMEOUT
		 */
		private class KeepAlive implements Runnable {

			/**
			 * Notification was received.
			 */
			public void onNotification() {
				handler.removeCallbacks(this);
				handler.postDelayed(this, NOTIFICATIONS_TIMEOUT);
			}

			/**
			 * Notifications timed out.
			 */
			@Override
			public void run() {
				if (rowerData != null) {
					// re-enable notification
					trace.comment("rower-data reenable notification");
					enableNotification(connected, rowerData);
				}
			}
		}
	}

	/**
	 * The ComModule sends erroneous elapsed time values on minute boundaries:
	 * <ul>
	 *     <li>revisions 4.x jumps forward and back a minute but recovers shortly after</li>
	 *     <li>revisions 1.x jumps back a minute and never recovers</li>
	 * </ul>
	 * Note: Since negative deltas are always ignored, a reset of the rower
	 * can not be detected by means of a zero duration - distance and stroke count will have
	 * to suffer for that.
	 */
	private int durationDelta(int elapsedTime) {
		if (elapsedTime == this.elapsedTime) {
			// no change
			return 0;
		}

		int delta = elapsedTime - this.elapsedTime;
		this.elapsedTime = elapsedTime;
		trace.comment(String.format("elapsed time %+d = %d", delta, elapsedTime));

		if (delta < 0) {
			// ignore error
			return 0;
		}

		if (delta > 5) {
			// limit error
			return 1;
		}

		// correct
		return delta;
	}
}