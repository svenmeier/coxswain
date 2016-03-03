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
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import propoid.util.content.Preference;
import svenmeier.coxswain.Application;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.Rower;

/**
 * https://github.com/jamesnesfield/node-waterrower/blob/develop/Waterrower/index.js
 */
public class WaterRower implements Rower {

    public static final String TRACE_FILE = "waterrower.trace";

    private final Context context;

    private final Snapshot memory;

    private final UsbDevice device;

    private UsbDeviceConnection connection;
    private UsbEndpoint input;
    private UsbEndpoint output;

    private IProtocol protocol;

    private boolean detached;

    private BroadcastReceiver receiver;

    private Writer trace;

    public WaterRower(Context context, Snapshot memory, UsbDevice device) {
        this.context = context;
        this.memory = memory;

        this.device = device;
    }

    @Override
    public boolean open() {
        if (isOpen()) {
            return true;
        }

        initTrace();

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

        if (true) {
            protocol = new Protocol4(connection, input, output, trace);
        } else {
            protocol = new Protocol3(connection, input, trace);
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

        this.input = null;
        this.output = null;

        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
    }

    @Override
    public void reset() {
        protocol.reset();
    }

    @Override
    public boolean row() {
        if (isOpen() == false || detached) {
            return false;
        }

        protocol.transfer(memory);

        return true;
    }

    private void initTrace() {
        if (Preference.getBoolean(context, R.string.preference_hardware_trace).get()) {
            try {
                File dir = Environment.getExternalStoragePublicDirectory(Application.TAG);
                dir.mkdirs();
                dir.setReadable(true, false);
                File file = new File(dir, TRACE_FILE);

                trace = new BufferedWriter(new FileWriter(file));

                // update media so file can be found via MTB
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                return;
            } catch (Exception e) {
                Log.e(Application.TAG, "cannot open trace", e);
            }
        }

        trace = new NullWriter();
    }

    private void trace(char prefix, String message) {
        if (trace != null) {
            try {
                trace.write(prefix);
                trace.write(message);
                trace.write('\n');
                trace.flush();
            } catch (IOException ex) {
                Log.e(Application.TAG, "cannot write trace", ex);
                trace = null;
            }
        }
    }

    private void closeTrace() {
        if (trace != null) {
            try {
                trace.close();
            } catch (IOException ignore) {
            }
            trace = null;
        }
    }

    private boolean initConnection() {
        trace('#', String.format("connecting to %s", device.getDeviceName()));

        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.connection = manager.openDevice(device);
        if (this.connection == null) {
            trace('#', String.format("cannot open connection %s", device.getDeviceName()));
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
                    trace('#', String.format("claimed interface %s", interfaceId));
                    input = in;
                    output = out;
                    return true;
                } else {
                    trace('#', String.format("cannot claim interface %s", interfaceId));
                }
            } else {
                trace('#', String.format("no bulk endpoints %s", interfaceId));
            }
        }

        trace('#', "no interface");
        this.connection.close();
        this.connection = null;
        return false;
    }
}