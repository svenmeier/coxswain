package svenmeier.coxswain.rower.wireless;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.util.Log;

import java.util.ArrayDeque;
import java.util.UUID;

import propoid.util.content.Preference;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.bluetooth.BlueWriter;
import svenmeier.coxswain.bluetooth.BluetoothActivity;
import svenmeier.coxswain.bluetooth.Fields;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.util.ByteUtils;
import svenmeier.coxswain.util.PermissionBlock;

public class BluetoothRower extends Rower {

	private static final int CONNECT_TIMEOUT_MILLIS = 10000;

	private static final int RESET_TIMEOUT_MILLIS = 3000;

	/**
	 * Timeout after which we re-enable notifications.
	 */
	private static final int NOTIFICATIONS_TIMEOUT = 2000;

	private static final int BATTERY_LEVEL_NOTIFICATION_THRESHOLD = 25;

	private static final byte OP_CODE_REQUEST_CONTROL = 0x00;
	private static final byte OP_CODE_RESET = 0x01;

	private final Context context;

	private final Handler handler = new Handler();

	private final Preference<String> devicePreference;
	
	private ArrayDeque<Connection> connections = new ArrayDeque<>();

	private boolean resetting = false;

	private int previousElapsedTime;

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

	private boolean isCurrent(Connection connection) {
		return this.connections.peek() == connection;
	}

	@Override
	public void reset() {
		super.reset();

		resetting = true;
		super.setDistance(1);
		super.setDuration(1);
		super.setStrokes(1);
		super.setEnergy(1);

		Connection last = connections.peek();
		if (last instanceof GattConnection) {
			((GattConnection) last).reset();
		}
	}

	private boolean noTargetValue() {
		return !super.anyTargetValue();
	}

	@Override
	public boolean anyTargetValue() {
		return resetting ? false : super.anyTargetValue();
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
			if (isCurrent(this) && isEnabled()) {
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
				if (isCurrent( this)) {
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
			if (isCurrent( this)) {
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
	private class GattConnection extends BlueWriter implements Connection {

		private final String address;

		private ConnectionTimeout connectionTimeout = new ConnectionTimeout();

		private ResetTimeout resetTimeout = new ResetTimeout();

		private KeepAlive keepAlive = new KeepAlive();

		private BluetoothAdapter adapter;

		private BluetoothGatt connected;

		private BluetoothGattCharacteristic softwareRevision;
		private BluetoothGattCharacteristic manufacturerName;
		private BluetoothGattCharacteristic rowerData;
		private BluetoothGattCharacteristic controlPoint;
		private BluetoothGattCharacteristic batteryLevel;
		/**
		 * CommModules without support for battery level send proprietary notifications
		 * when on low battery level once a minute instead
		 */
		private BluetoothGattCharacteristic lowBattery;

		private String rowerManufacturer;
		private String rowerVersion;

		GattConnection(String address) {
			this.address = address;
		}

		private void select() {
			devicePreference.set(null);

			handler.removeCallbacks(connectionTimeout);

			pop(); // this
			pop(); // previous selection
			push(new SelectionConnection());
		}

		@Override
		public synchronized void open() {

			BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

			adapter = manager.getAdapter();

			try {
				BluetoothDevice device = adapter.getRemoteDevice(address);

				trace.onOutput(String.format("connecting %s", address));
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					// newer API has been reported to improve connectivity
					connected = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE);
				} else {
					connected = device.connectGatt(context, false, this);
				}

				handler.postDelayed(connectionTimeout, CONNECT_TIMEOUT_MILLIS);
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

			handler.removeCallbacks(resetTimeout);
			handler.postDelayed(resetTimeout, RESET_TIMEOUT_MILLIS);
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

			manufacturerName = get(gatt, SERVICE_DEVICE_INFORMATION, CHARACTERISTIC_MANUFACTURER_NAME);
			if (manufacturerName == null) {
				trace.comment("no manufacturer-name");
				rowerManufacturer = "default"; // use default if not be available
			} else {
				trace.onOutput("reading manufacturer-name");
				read(connected, manufacturerName);
			}

			batteryLevel = get(gatt, SERVICE_BATTERY, CHARACTERISTIC_BATTERY_LEVEL);
			if (batteryLevel == null) {
				trace.comment("no battery-level");
			} else {
				trace.onOutput("reading battery-level");
				read(connected, batteryLevel);
			}

			lowBattery = get(gatt, UUID.fromString("00001001-C042-66BA-1335-90118F542C77"), UUID.fromString("8ec92002-f315-4f60-9fb8-838830daea50"));
			if (lowBattery == null) {
				trace.comment("no low-battery");
			} else {
				trace.onOutput("low-battery enable notification");
				enableNotification(gatt, lowBattery);
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
				rowerVersion = softwareRevision.getStringValue(0);
				trace.onInput(String.format("software-revision %s read", rowerVersion));

				checkControlPoint(gatt);
			} else if (manufacturerName != null && characteristic.getUuid().equals(manufacturerName.getUuid())) {
				rowerManufacturer = manufacturerName.getStringValue(0);
				trace.onInput(String.format("manufacturer-name %s read", rowerManufacturer));

				checkControlPoint(gatt);
			} else if (batteryLevel != null && characteristic.getUuid().equals(batteryLevel.getUuid())) {
				int level = batteryLevel.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

				trace.onInput(String.format("battery level %s read", level));

				onBatteryLevel(level);
			}

			super.onCharacteristicRead(gatt, characteristic, status);
		}

		private void checkControlPoint(BluetoothGatt gatt) {
			if (rowerManufacturer != null && rowerVersion  != null) {
				if ("WaterRower".equals(rowerManufacturer) && rowerVersion.compareTo("4.2") < 0) {
					// old firmware rejects re-bonding of a previously bonded device,
					// so do not write to the control point, as this triggers a bond
					trace.comment(String.format("skipping control-point due to version %s", rowerVersion));
				} else {
					controlPoint = get(gatt, SERVICE_FITNESS_MACHINE, CHARACTERISTIC_CONTROL_POINT);
					if (controlPoint == null) {
						trace.comment("no control-point");
					} else {
						trace.onOutput("control-point requesting control");
						write(connected, controlPoint, OP_CODE_REQUEST_CONTROL);
					}

					if (resetting == true) {
						reset();
					}
				}
			}
		}

		private static final int MORE_DATA = 0;
		private static final int AVERAGE_STROKE_RATE = 1;
		private static final int TOTAL_DISTANCE = 2;
		private static final int INSTANTANEOUS_PACE = 3;
		private static final int AVERAGE_PACE = 4;
		private static final int INSTANTANEOUS_POWER = 5;
		private static final int AVERAGE_POWER = 6;
		private static final int RESISTANCE_LEVEL = 7;
		private static final int EXPANDED_ENERGY = 8;
		private static final int HEART_RATE = 9;
		private static final int METABOLIC_EQUIVALENT = 10;
		private static final int ELAPSED_TIME = 11;
		private static final int REMAINING_TIME = 12;

		@Override
		public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			if (rowerData == null) {
				return;
			}

			if (lowBattery != null && characteristic.getUuid().equals(lowBattery.getUuid())) {
				trace.onInput("low-battery changed");

				// level is not known
				onBatteryLevel(10);

				// prevent further notifications
				lowBattery = null;
			} else if (rowerData != null && characteristic.getUuid().equals(rowerData.getUuid())) {
				trace.onInput(String.format("rower-data changed %s", ByteUtils.toHex(characteristic.getValue())));

				keepAlive.onNotification();

				Fields fields = new Fields(characteristic, Fields.UINT16);
				try {
					if (fields.isNotSet(MORE_DATA)) {
						int strokeRate = fields.get(Fields.UINT8) / 2;
						trace.comment(String.format("strokeRate %d", strokeRate));
						setStrokeRate(strokeRate);

						int strokes = fields.get(Fields.UINT16);
						trace.comment(String.format("strokes %d", strokes));
						setStrokes(strokes);
					}
					if (fields.isSet(AVERAGE_STROKE_RATE)) {
						fields.get(Fields.UINT8);
					}
					if (fields.isSet(TOTAL_DISTANCE)) {
						int totalDistance = fields.get(Fields.UINT16) +
								(fields.get(Fields.UINT8) << 16);
						trace.comment(String.format("totalDistance %d", totalDistance));
						setDistance(totalDistance);
					}
					if (fields.isSet(INSTANTANEOUS_PACE)) {
						int instantaneousPace = fields.get(Fields.UINT16);
						trace.comment(String.format("instantaneousPace %d", instantaneousPace));
						if (instantaneousPace == 0) {
							setSpeed(0);
						} else {
							setSpeed(500 * 100 / instantaneousPace);
						}
					}
					if (fields.isSet(AVERAGE_PACE)) {
						fields.skip(Fields.UINT16);
					}
					if (fields.isSet(INSTANTANEOUS_POWER)) {
						int instantaneousPower = fields.get(Fields.SINT16);
						trace.comment(String.format("instantaneousPower %d", instantaneousPower));
						setPower(instantaneousPower);
					}
					if (fields.isSet(AVERAGE_POWER)) {
						fields.skip(Fields.SINT16);
					}
					if (fields.isSet(RESISTANCE_LEVEL)) {
						fields.skip(Fields.SINT16);
					}
					if (fields.isSet(EXPANDED_ENERGY)) {
						int energy = fields.get(Fields.UINT16); // total
						trace.comment(String.format("energy %d", energy));
						setEnergy(energy);
						fields.skip(Fields.UINT16); // per hour
						fields.skip(Fields.UINT8); // per minute
					}
					if (fields.isSet(HEART_RATE)) {
						int heartRate = fields.get(Fields.UINT8);
						trace.comment(String.format("heartRate %d", heartRate));
						if (heartRate > 0) {
							setPulse(heartRate);
						}
					}
					if (fields.isSet(METABOLIC_EQUIVALENT)) {
						fields.skip(Fields.UINT8);
					}
					if (fields.isSet(ELAPSED_TIME)) {
						int elapsedTime = fields.get(Fields.UINT16);
						int duration = elapsedTime;
						if (!resetting) {
							duration = getDuration() + durationDelta(elapsedTime);
						}
						trace.comment(String.format("elapsedTime %d (%d)", elapsedTime, duration));
						setDuration(duration);
					}
					if (fields.isSet(REMAINING_TIME)) {
						fields.get(Fields.UINT16);
					}
				} catch (Exception ex) {
					trace.comment(String.format("field mismatch at ", fields.offset()));
				}

				if (resetting && noTargetValue()) {
					trace.comment("reset");
					resetting = false;
				}

				notifyMeasurement();
			}
		}

		/**
		 * @see BluetoothRower#CONNECT_TIMEOUT_MILLIS
		 */
		private class ConnectionTimeout implements Runnable {
			@Override
			public void run() {
				if (isCurrent( GattConnection.this) && rowerData == null) {
					trace.comment("connection timeout");

					toast(context.getString(R.string.bluetooth_rower_failed, connected.getDevice().getAddress()));

					select();
				}
			}
		}

		private class ResetTimeout implements Runnable {
			@Override
			public void run() {
				if (isCurrent(GattConnection.this) && resetting == true) {
					trace.comment("reset timeout");

					toast(context.getString(R.string.bluetooth_rower_reset_timeout));
				}
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
				if (isCurrent(GattConnection.this) && rowerData != null) {
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
	 */
	private int durationDelta(int elapsedTime) {
		if (elapsedTime == this.previousElapsedTime) {
			// no change
			return 0;
		}

		int delta = elapsedTime - this.previousElapsedTime;
		this.previousElapsedTime = elapsedTime;

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

	private void onBatteryLevel(int level) {
		if (level <= BATTERY_LEVEL_NOTIFICATION_THRESHOLD) {
			NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

			Notification.Builder builder = new Notification.Builder(context)
					.setContentText(context.getString(R.string.bluetooth_rower_battery_level, level));

			Coxswain.initNotification(context, builder, "Hardware");

			notificationManager.notify(R.string.bluetooth_rower_battery_level, builder.build());
		} else {
			Log.i(Coxswain.TAG, "bluetooth rower battery level " + level);
		}
	}
}