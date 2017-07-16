package svenmeier.coxswain.heart.bluetooth.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import svenmeier.coxswain.Coxswain;
import svenmeier.coxswain.heart.bluetooth.BluetoothHeartConnectionListener;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartCharacteristics;
import svenmeier.coxswain.heart.bluetooth.constants.BluetoothHeartDescriptors;
import svenmeier.coxswain.heart.bluetooth.reading.GattBatteryStatus;
import svenmeier.coxswain.heart.bluetooth.reading.GattBodySensorLocation;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToBattery;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToBodySensorLocation;
import svenmeier.coxswain.heart.bluetooth.typeconverter.CharacteristicToNotificationSupport;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.util.Destroyable;

import static android.bluetooth.BluetoothGatt.GATT_CONNECTION_CONGESTED;
import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
import static android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
import static android.bluetooth.BluetoothGatt.GATT_INVALID_OFFSET;
import static android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED;
import static android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED;

/**
 *  Generic communication with a bluetooth device.
 *
 *  Ensures only one request is sent to the device at a time (Android does not allow for more).
 *  It's tied to the constants defined in heart.bluetooth.constants, so not super-generic.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class AbstractBluetoothHeartDevice implements BluetoothHeartDevice {
    private final Conversation conversation;
    private final Map<UUID, List<BluetoothNotificationListener>> notificationListeners;

    public AbstractBluetoothHeartDevice(final @NonNull Context context, final @NonNull BluetoothDevice delegate, final @Nullable BluetoothHeartConnectionListener connectionListener) {
        this(new Conversation(context, delegate, new ConcurrentHashMap<UUID, List<BluetoothNotificationListener>>(2), connectionListener));
    }

    public AbstractBluetoothHeartDevice(final @NonNull Conversation conversation) {
        Preconditions.checkNotNull(conversation);

        this.conversation = conversation;
        this.notificationListeners = conversation.notificationListeners;
    }

    protected CompletableFuture<BluetoothGattCharacteristic> query(final BluetoothHeartCharacteristics characteristic) {
        final CompletableFuture<BluetoothGattCharacteristic> response = new CompletableFuture<>();
        final ConversationItem item = new ConversationItem(response, characteristic, new byte[]{},
            ConversationItemType.READ);
        conversation.accept(item);
        return response;
    }

    protected CompletableFuture<Boolean> queryNotificationSupport(final BluetoothHeartCharacteristics characteristic) {
        final CompletableFuture<BluetoothGattCharacteristic> response = new CompletableFuture<>();
        final ConversationItem item = new ConversationItem(response, characteristic, new byte[]{},
            ConversationItemType.QUERY_NOTIFICATION_SUPPORT);
        conversation.accept(item);
        return response
                .handle(CharacteristicToNotificationSupport.INSTANCE);
    }

    protected CompletableFuture<BluetoothGattCharacteristic> write(final BluetoothHeartCharacteristics characteristic, final byte[] value) {
        final CompletableFuture<BluetoothGattCharacteristic> response = new CompletableFuture<>();
        final ConversationItem item = new ConversationItem(response, characteristic, value,
                ConversationItemType.WRITE);
        conversation.accept(item);
        return response;
    }

    protected CompletableFuture<BluetoothGattCharacteristic> enableNotifications(final BluetoothHeartCharacteristics characteristic, final BluetoothNotificationListener listener) {
        final CompletableFuture<BluetoothGattCharacteristic> response = new CompletableFuture<>();
        notificationListeners.putIfAbsent(characteristic.getUuid(), new ArrayList<BluetoothNotificationListener>(1));
        notificationListeners.get(characteristic.getUuid()).add(listener);
        final ConversationItem item = new ConversationItem(response, characteristic, new byte[]{1},
                ConversationItemType.SET_NOTIFY);
        conversation.accept(item);
        return response;
    }

    protected CompletableFuture<BluetoothGattCharacteristic> disableNotifications(final BluetoothHeartCharacteristics characteristic) {
        final CompletableFuture<BluetoothGattCharacteristic> response = new CompletableFuture<>();
        final ConversationItem item = new ConversationItem(response, characteristic, new byte[]{0},
                ConversationItemType.SET_NOTIFY);
        conversation.accept(item);
        return response;
    }

    public Conversation getConversation() {
        return conversation;
    }

    /**
     *  Encapsulates all parameters required for a request to the device.
     */
    private static class ConversationItem {
        final CompletableFuture<BluetoothGattCharacteristic> future;
        final BluetoothHeartCharacteristics characteristic;
        final byte[] value;
        final ConversationItemType type;

        public ConversationItem(CompletableFuture<BluetoothGattCharacteristic> future, BluetoothHeartCharacteristics characteristic, byte[] value, ConversationItemType type) {
            this.future = future;
            this.characteristic = characteristic;
            this.value = value;
            this.type = type;
        }
    }


    @Override
    public void destroy() {
        for (UUID uuid : notificationListeners.keySet()) {
            final Optional<BluetoothHeartCharacteristics> chr = BluetoothHeartCharacteristics.byUuid(uuid);
            if (chr.isPresent()) {
                disableNotifications(chr.get());
            }
        }

        conversation.destroy();
        notificationListeners.clear();
    }

    /**
     *  Ensures, that only one characteristic is queried at a time
     */
    public static class Conversation extends BluetoothGattCallback implements Destroyable {
        final Queue<ConversationItem> requests;
        final BluetoothGatt gatt;
        private final Map<UUID, List<BluetoothNotificationListener>> notificationListeners;
        private final @Nullable BluetoothHeartConnectionListener connectionListener;
        volatile boolean isConnected;

        public Conversation(final Context context, final BluetoothDevice device, final Map<UUID, List<BluetoothNotificationListener>> notificationListeners, final @Nullable BluetoothHeartConnectionListener connectionListener) {
            Preconditions.checkNotNull(context, "Need a context");
            Preconditions.checkNotNull(device, "Need a delegate-device");
            Preconditions.checkNotNull(notificationListeners);

            this.gatt = device.connectGatt(context, true, this);
            this.requests = new ArrayBlockingQueue<>(10);
            this.notificationListeners = notificationListeners;
            this.connectionListener = connectionListener;
            isConnected = false;
        }

        /**
         *  Enqueues one request to be sent to the device
         */
        public void accept(ConversationItem item) {
            ensureServicesDiscovered();
            if (requests.isEmpty()) {
                requests.add(item);
                handleCurrentRequest();
            } else {
                requests.add(item);
            }
        }

        private void ensureServicesDiscovered() {
            if (! gatt.getServices().isEmpty()) {
                return;
            } else {
                requests.add(new ConversationItem(null, null, null, ConversationItemType.SERVICE_DISCOVERY));
                handleCurrentRequest();
            }
        }

        private void handleCurrentRequest() {
            if (! isConnected) {
                connect();
                return; // The connection-state listener will invoke this method again
            }
            final ConversationItem currentRequest = requests.peek();
            if (currentRequest != null) {
                switch (currentRequest.type) {
                    case SERVICE_DISCOVERY:
                        if (!gatt.getServices().isEmpty()) {
                            Log.i(Coxswain.TAG, "Services already discovered, skipping");
                            requests.poll();
                            handleCurrentRequest();
                        } else if (gatt.discoverServices()) {
                            Log.i(Coxswain.TAG, "Service discovery on " + gatt.getDevice().getName() + " started...");
                        } else {
                            Log.w(Coxswain.TAG, "Error starting service discovery");
                        }
                        break;
                    case QUERY_NOTIFICATION_SUPPORT:
                        requests.poll();    // Remove head
                        currentRequest.future.complete(currentRequest.characteristic.lookup(gatt));
                        break;
                    case READ:
                        Log.d(Coxswain.TAG, "Reading characteristic " + currentRequest.characteristic);
                        readNow(currentRequest.characteristic);
                        break;
                    case WRITE:
                        Log.d(Coxswain.TAG, "Writing characteristic " + currentRequest.characteristic);
                        writeNow(currentRequest.characteristic, currentRequest.value);
                        break;
                    case SET_NOTIFY:
                        Log.d(Coxswain.TAG, "Setting notifications on characteristic " + currentRequest.characteristic);
                        enableNotificationNow(currentRequest.characteristic, currentRequest.value[0] > 0);
                        break;
                }
            }
        }



        private void writeNow(final BluetoothHeartCharacteristics characteristic, byte[] value) {
            final BluetoothGattCharacteristic chr = characteristic.lookup(gatt);
            chr.setValue(value);
            gatt.writeCharacteristic(chr);
        }

        private void readNow(final BluetoothHeartCharacteristics characteristic) {
            final BluetoothGattCharacteristic chr = characteristic.lookup(gatt);
            if (chr == null) {
                failCurrentRequest("Characteristic " + characteristic + " not resolvable!");
            } else if ((chr.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                failCurrentRequest("Characteristic " + characteristic + " (" + chr.getUuid() + ") is not readable!");
            } else {
                int attempts = 0;
                while (! gatt.readCharacteristic(chr)) {
                    attempts++;
                    if (attempts < 3) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            failCurrentRequest(e.getMessage());
                            return;
                        }
                    } else {
                        failCurrentRequest("Request wasn't accepted after 3 attempts!");
                    }
                }
            }
        }

        private void enableNotificationNow(final BluetoothHeartCharacteristics characteristic, boolean enable) {
            final BluetoothGattCharacteristic chr = characteristic.lookup(gatt);
            gatt.setCharacteristicNotification(chr, enable);
            final BluetoothGattDescriptor desc = chr.getDescriptor(BluetoothHeartDescriptors.CLIENT_CHARACTERISTIC_CONFIGURATION.getUuid());
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }


        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            handleIncomingData(characteristic, characteristic.getValue(), status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            handleIncomingData(characteristic, characteristic.getValue(), status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            handleIncomingData(descriptor.getCharacteristic(), descriptor.getValue(), status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(Coxswain.TAG, "Service discovery returned " + status);
            handleIncomingData(null, null, status);
        }

        /**
         *  Called when notifications are enabled
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final ConversationItem currentRequest = requests.peek();
            if ((currentRequest == null) && (currentRequest.type == ConversationItemType.SET_NOTIFY)
                    && (currentRequest.characteristic.getUuid().equals(characteristic.getUuid()))) {
                requests.poll();
            }
            if (notificationListeners.containsKey(characteristic.getUuid())) {
                for (BluetoothNotificationListener listener: notificationListeners.get(characteristic.getUuid())) {
                    listener.onNotification(characteristic);
                }
            }
        }

        private void handleIncomingData(final BluetoothGattCharacteristic characteristic, final byte[] value, final int status) {
            if (
                    status == GATT_SUCCESS ||
                    status == GATT_READ_NOT_PERMITTED ||
                    status == GATT_WRITE_NOT_PERMITTED ||
                    status == GATT_INSUFFICIENT_AUTHENTICATION ||
                    status == GATT_REQUEST_NOT_SUPPORTED ||
                    status == GATT_INSUFFICIENT_ENCRYPTION ||
                    status == GATT_INVALID_OFFSET ||
                    status == GATT_INVALID_ATTRIBUTE_LENGTH) {

                final ConversationItem currentRequest = requests.peek();
                if (currentRequest == null) {
                    Log.w(Coxswain.TAG, "Request vanished");
                } else if (currentRequest.type == ConversationItemType.SERVICE_DISCOVERY) {
                    Log.i(Coxswain.TAG, "Services discovered");
                    requests.poll();    // Remove head
                    handleCurrentRequest();
                } else if (currentRequest.characteristic.getUuid().equals(characteristic.getUuid())) {
                    requests.poll();    // Remove head
                    Log.d(Coxswain.TAG, "Replying to " + currentRequest.characteristic + ": " + value);
                    final BluetoothGattCharacteristic ret = new BluetoothGattCharacteristic(characteristic.getUuid(), characteristic.getProperties(), characteristic.getPermissions());
                    ret.setValue(value);
                    currentRequest.future.complete(ret);
                    handleCurrentRequest();
                } else {
                    // TODO: Tough decision - should we drop the current request?
                    // Perhaps the correct response is still coming... this will then also
                    // cause the next request to be invalid.
                    // On the other hand: If we don't drop, we might get stuck
                    failCurrentRequest("Response does not match the request! Expected: "
                        + currentRequest.characteristic + "(" + currentRequest.characteristic.getParcelUuid() + ")" +
                       " but got "
                        + BluetoothHeartCharacteristics.byUuid(characteristic.getUuid())
                        + "(" + characteristic.getUuid() + ")"
                    );
                }
            } else if (
                    status == GATT_CONNECTION_CONGESTED ||
                    status == GATT_FAILURE) {
               Log.i(Coxswain.TAG, "Communication issue, retrying command");
               handleCurrentRequest();
            } else {
                Log.e(Coxswain.TAG, "Unknown error, skipping bluetooth command");
                requests.poll();
                handleCurrentRequest();
            }
        }

        private void failCurrentRequest(final String message) {
            Log.w(Coxswain.TAG, message);
            final ConversationItem currentRequest = requests.poll();    // Remove head
            if (currentRequest != null) {
                currentRequest.future.completeExceptionally(new IllegalStateException(message));
            }
            handleCurrentRequest();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }


        // ----------------------------------------------------------------------------------------
        //
        //   Connection-stuff follows...
        //

        /**
         *  Connects if required
         */
        private void connect() {
            if (! isConnected) {
                Log.i(Coxswain.TAG, "Ensure connected: triggering connection...");
                int attempts = 0;
                while (! gatt.connect()) {
                    if (attempts++ > 4) {
                        Log.w(Coxswain.TAG, "Connection to " + gatt.getDevice().getName() +
                                " was unsuccessful after " + attempts + " attempts. Giving up!");
                        return;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(Coxswain.TAG, "Connected to GATT server.");
                isConnected = true;
                if (connectionListener != null) {
                    connectionListener.onConnected(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                }
                handleCurrentRequest();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("", "Disconnected from GATT server.");
                isConnected = false;
                if (connectionListener != null) {
                    connectionListener.onDisconnected(gatt.getDevice().getName(), gatt.getDevice().getAddress());
                }
            }
        }

        @Override
        public void destroy() {
            gatt.disconnect();
            gatt.close();    // TODO: Should we close?!
            notificationListeners.clear();
            requests.clear();
        }
    }

    private enum ConversationItemType {
        READ, WRITE, SET_NOTIFY, QUERY_NOTIFICATION_SUPPORT, SERVICE_DISCOVERY
    }
}
