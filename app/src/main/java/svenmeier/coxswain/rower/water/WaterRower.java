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

import java.util.LinkedList;
import java.util.Queue;

import svenmeier.coxswain.Gym;
import svenmeier.coxswain.MainActivity;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.water.usb.Input;
import svenmeier.coxswain.rower.water.usb.Output;

/**
 * https://github.com/jamesnesfield/node-waterrower/blob/develop/Waterrower/index.js
 */
public class WaterRower implements Rower {

    private static final int TIMEOUT = 0; // milliseconds

    private final Context context;

    private final Snapshot memory;

    private final UsbDevice device;

    private UsbDeviceConnection connection;

    private Input input;
    private Output output;

    private BroadcastReceiver receiver;

    private Mapper mapper = new Mapper();

    private Queue<String> requests = new LinkedList<>();

    public WaterRower(Context context, Snapshot memory, UsbDevice device) {
        this.context = context;
        this.memory = memory;

        this.device = device;
    }

    @Override
    public synchronized boolean open() {
        if (isOpen()) {
            return true;
        }

        if (initConnection() == false) {
            return false;
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device == WaterRower.this.device) {
                        onDetached();
                    }
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        requests.add(Mapper.INIT);
        requests.add(Mapper.VERSION);

        return true;
    }

    /**
     * Hook method called when device was detached.
     */
    protected void onDetached() {
    }

    @Override
    public String getName() {
        return "Waterrower";
    }

    @Override
    public synchronized boolean isOpen() {
        return this.connection != null;
    }

    @Override
    public synchronized void close() {
        if (isOpen() == false) {
            return;
        }

        this.requests.clear();

        context.unregisterReceiver(receiver);
        receiver = null;

        this.input = null;
        this.output = null;

        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
    }

    @Override
    public synchronized void reset() {
        requests.add(Mapper.RESET);
    }

    @Override
    public synchronized boolean row() {
        if (isOpen() == false) {
            return false;
        }

        if (requests.isEmpty()) {
            output.write(mapper.cycle().request);
        } else {
            output.write(requests.remove());
        }

        // could be closed while #write() was waiting
        if (isOpen() == false) {
            return false;
        }

        while (true) {
            String read = input.read();
            if (read == null) {
                break;
            }

            mapper.map(read, memory);
        }

        return true;
    }

    private boolean initConnection() {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        this.connection = manager.openDevice(device);
        if (this.connection == null) {
            Log.d(MainActivity.TAG, String.format("no connection", device.getDeviceName()));
            return false;
        }

        // set data request, baud rate, 115200,
        this.connection.controlTransfer(0x40, 0x03, 0x001A, 0, null, 0, 0);

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface anInterface = device.getInterface(i);

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
                    input = new Input(this.connection, in);
                    output = new Output(this.connection, out, this);
                    return true;
                } else {
                    Log.d(MainActivity.TAG, String.format("can not claim interface %s", anInterface.getId()));
                }
            } else {
                Log.d(MainActivity.TAG, String.format("no suitable endpoints %s", anInterface.getId()));
            }
        }

        Log.d(MainActivity.TAG, String.format("no suitable interface", device.getDeviceName()));
        this.connection.close();
        this.connection = null;
        return false;
    }
}