package svenmeier.coxswain.ant;

import android.content.Context;
import android.widget.Toast;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.math.BigDecimal;
import java.util.EnumSet;

import svenmeier.coxswain.Heart;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Measurement;

/**
 * Created by sven on 13.12.16.
 */
public class AntHeart extends Heart {

	private static final int FIRST_AVAILABLE_DEVICE = 0;

	private static final int NO_PROXIMITY_SEARCH = 0;

	private int heartRate = -1;

	private Connection connection;

	public AntHeart(Context context, Measurement measurement) {
		super(context, measurement);

		this.connection = new AntConnection();
		this.connection.open();
	}

	@Override
	public void pulse() {
		if (heartRate == -1) {
			return;
		}

		measurement.pulse = heartRate;
	}

	@Override
	public void destroy() {
		if (connection != null) {
			connection.close();
		}
	}

	private void toast(String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	private interface Connection {

		void open();

		void close();
	}

	private class AntConnection implements Connection, AntPluginPcc.IDeviceStateChangeReceiver, AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>, AntPlusHeartRatePcc.IHeartRateDataReceiver {

		private PccReleaseHandle<AntPlusHeartRatePcc> handle;

		private AntPlusHeartRatePcc pcc;

		@Override
		public void open() {
			handle = AntPlusHeartRatePcc.requestAccess(context, FIRST_AVAILABLE_DEVICE, NO_PROXIMITY_SEARCH, this, this);

			toast(context.getString(R.string.ant_heart_searching));
		}

		@Override
		public void close() {
			if (pcc != null) {
				pcc.releaseAccess();
				pcc = null;
			}

			if (handle != null) {
				handle.close();
				handle = null;
			}
		}

		@Override
		public void onDeviceStateChange(DeviceState deviceState) {
			if (deviceState == DeviceState.DEAD || deviceState == DeviceState.CLOSED) {
				heartRate = 0;
			}
		}

		@Override
		public void onResultReceived(AntPlusHeartRatePcc pcc, RequestAccessResult requestAccessResult, DeviceState deviceState) {
			if (requestAccessResult == RequestAccessResult.SUCCESS || requestAccessResult == RequestAccessResult.ALREADY_SUBSCRIBED) {
				this.pcc = pcc;

				pcc.subscribeHeartRateDataEvent(this);

				toast(context.getString(R.string.ant_heart_reading));
			} else if (requestAccessResult == RequestAccessResult.DEPENDENCY_NOT_INSTALLED) {
				toast(context.getString(R.string.ant_heart_dependency_not_installed));
			} else {
				toast(context.getString(R.string.ant_heart_not_found));
			}
		}

		@Override
		public void onNewHeartRateData(long timestamp, EnumSet<EventFlag> flags, int heartRate, long heartBeatCount, BigDecimal heartBeatEventTime, AntPlusHeartRatePcc.DataState dataState) {
			AntHeart.this.heartRate = heartRate;
		}
	}
}