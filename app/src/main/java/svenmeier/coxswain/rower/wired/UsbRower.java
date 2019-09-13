/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.rower.wired;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

import propoid.util.content.Preference;
import svenmeier.coxswain.BuildConfig;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.wired.usb.ITransfer;
import svenmeier.coxswain.rower.wired.usb.UsbTransfer;

/**
 * Waterrower rower.
 */
public class UsbRower extends Rower implements Runnable {

    private final Context context;

    private final UsbDevice device;

    private final Handler handler = new Handler();

    private UsbDeviceConnection connection;

    private ITransfer transfer;

    private IProtocol protocol;

    private BroadcastReceiver receiver;

    private ITrace trace;

    private boolean adjustSpeed;

    public UsbRower(Context context, UsbDevice device, Callback callback) {
        super(callback);
        
        this.context = context;
        this.device = device;

        adjustSpeed = Preference.getBoolean(context, R.string.preference_adjust_speed).get();
    }

    @Override
    public void open() {
        if (connection != null) {
            return;
        }

        initTrace();

        trace.comment(String.format("coxswain %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        if (initConnection()) {
			callback.onConnected();
        } else {
			callback.onDisconnected();
            return;
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (connection == null) {
                    return;
                }

                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device.equals(UsbRower.this.device)) {
                        trace.comment(String.format("disconnected from %s", device.getDeviceName()));
                        
						callback.onDisconnected();
                    }
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        if (Preference.getBoolean(context, R.string.preference_hardware_legacy).get()) {
            protocol = new Protocol3(transfer, trace);
        } else {
            Protocol4 protocol4 = new Protocol4(transfer, trace);
            if (Preference.getBoolean(context, R.string.preference_adjust_energy).get()) {
                protocol4.adjustEnergy(new EnergyCalculator(Preference.getInt(context, R.string.preference_weight).fallback(90).get()));
            }
            protocol = protocol4;
        }

        new Thread(this).start();
    }

    @Override
    public String getName() {
        return "Waterrower";
    }

    @Override
    public void close() {
    	if (receiver != null) {
			context.unregisterReceiver(receiver);
			receiver = null;
		}

        closeTrace();

        this.transfer = null;

        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
    }

    @Override
    public void reset() {
        super.reset();

        protocol.reset();
    }

    @Override
    public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        while (connection != null) {
            protocol.transfer(this);

            if (adjustSpeed) {
                // magic formula see:
                // http://www.concept2.com/indoor-rowers/training/calculators/watts-calculator
                float mps = 0.709492f * (float) Math.pow(this.power, 1d / 3d);

                this.speed = Math.round(mps * 100);
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
					callback.onMeasurement();
                }
            });
        }
    }

    private void initTrace() {
        if (Preference.getBoolean(context, R.string.preference_hardware_trace).get()) {
            try {
                trace = new FileTrace(context);

                return;
            } catch (Exception e) {
                Log.e(Coxswain.TAG, "cannot open trace", e);
            }
        }

        trace = new NullTrace();
    }

    private void closeTrace() {
        if (trace != null) {
            trace.close();
            trace = null;
        }
    }

    private boolean initConnection() {
        trace.comment(String.format("connecting to %s", device.getDeviceName()));

        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.connection = manager.openDevice(device);
        if (this.connection == null) {
            trace.comment(String.format("cannot open connection %s", device.getDeviceName()));
            return false;
        }

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface anInterface = device.getInterface(i);
            int interfaceId = anInterface.getId();

            UsbEndpoint out = null;
            UsbEndpoint in = null;

            for (int e = 0; e < anInterface.getEndpointCount(); e++) {
                UsbEndpoint endpoint = anInterface.getEndpoint(e);

                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        out = endpoint;
                    } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        in = endpoint;
                    }
                }
            }

            if (out != null && in != null) {
                if (this.connection.claimInterface(anInterface, true)) {
                    trace.comment(String.format("claimed interface %s", interfaceId));
                    transfer = new UsbTransfer(connection, in, out);
                    return true;
                } else {
                    trace.comment(String.format("cannot claim interface %s", interfaceId));
                }
            } else {
                trace.comment(String.format("no bulk endpoints %s", interfaceId));
            }
        }

        trace.comment("no interface");
        this.connection.close();
        this.connection = null;
        return false;
    }
}