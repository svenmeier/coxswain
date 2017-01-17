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
package svenmeier.coxswain.rower.water;

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
import android.util.Log;

import propoid.util.content.Preference;
import svenmeier.coxswain.BuildConfig;
import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.R;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.water.usb.ITransfer;
import svenmeier.coxswain.rower.water.usb.UsbTransfer;

/**
 * Waterrower rower.
 */
public class WaterRower extends Rower {

    private final Context context;

    private final UsbDevice device;

    private UsbDeviceConnection connection;

    private ITransfer transfer;

    private IProtocol protocol;

    private boolean detached;

    private BroadcastReceiver receiver;

    private ITrace trace;

    public WaterRower(Context context, UsbDevice device) {
        this.context = context;
        this.device = device;
    }

    @Override
    public boolean open() {
        if (isOpen()) {
            return true;
        }

        initTrace();

        trace.comment(String.format("coxswain %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        if (initConnection() == false) {
            closeTrace();

            return false;
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device.equals(WaterRower.this.device)) {
                        detached = true;
                    }
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        if (Preference.getBoolean(context, R.string.preference_hardware_legacy).get()) {
            protocol = new Protocol3(transfer, trace);
        } else {
            Protocol4 protocol4 = new Protocol4(transfer, trace);
            protocol4.energyCalculator.setWeight(Preference.getInt(context, R.string.preference_weight).fallback(90).get());
            protocol = protocol4;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Waterrower";
    }

    @Override
    public boolean isOpen() {
        return this.connection != null;
    }

    @Override
    public void close() {
        if (isOpen() == false) {
            return;
        }

        context.unregisterReceiver(receiver);
        receiver = null;

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
    public boolean row() {
        if (isOpen() == false || detached) {
            return false;
        }

        protocol.transfer(this);

        return true;
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