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
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

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

    private Mapper mapper = new Mapper();

    private BroadcastReceiver receiver;

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

        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        connection = manager.openDevice(device);
        if (connection == null) {
            return false;
        }

        if (initEndpoints() == false) {
            return false;
        }

        // set data request, baud rate, 115200,
        connection.controlTransfer(0x40, 0x03, 0x001A, 0, null, 0, 0);

        requests.add(Mapper.INIT);
        requests.add(Mapper.VERSION);

        return true;
    }

    @Override
    public String getName() {
        return "Waterrower";
    }

    @Override
    public synchronized boolean isOpen() {
        return this.receiver != null;
    }

    @Override
    public synchronized void close() {
        if (isOpen() == false) {
            return;
        }

        this.requests.clear();

        context.unregisterReceiver(receiver);
        this.receiver = null;

        this.input = null;
        this.output = null;

        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }

        Log.d(MainActivity.TAG, "closed");
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

    private boolean initEndpoints() {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface anInterface = device.getInterface(i);

            Log.d(MainActivity.TAG, String.format("interface %s", i));

            UsbEndpoint out = null;
            UsbEndpoint in = null;

            for (int e = 0; e < anInterface.getEndpointCount(); e++) {
                UsbEndpoint endpoint = anInterface.getEndpoint(e);

                Log.d(MainActivity.TAG, String.format("endpoint %s: type=%s direction=%s", e, endpoint.getType(), endpoint.getDirection()));

                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        out = endpoint;
                    } else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        in = endpoint;
                    }
                }
            }

            if (out != null && in != null) {
                if (connection.claimInterface(anInterface, true)) {
                    input = new Input(connection, in);
                    output = new Output(connection, out, this);
                    return true;
                } else {
                    Log.d(MainActivity.TAG, "cannot claim");
                }
            }
        }

        Log.d(MainActivity.TAG, "No endpoints found");
        return false;
    }
}