package svenmeier.coxswain.heart.generic.communication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import svenmeier.coxswain.heart.generic.ConnectionStatus;
import svenmeier.coxswain.heart.generic.ConnectionStatusListener;
import svenmeier.coxswain.heart.generic.BatteryStatusListener;
import svenmeier.coxswain.heart.generic.HeartRateCommunication;
import svenmeier.coxswain.heart.generic.HeartRateListener;

public class HandlerCommunication implements HeartRateCommunication {
    private Handler handler;

    private static final String IMPL = "Impl";
    private static final String CONNECTION_STATUS = "ConnectionStatus";
    private static final String DEVICE = "Device";
    private static final String MESSAGE = "Message";


    private enum MessageTypes {
        UNKNOWN(0),
        BATTERY(1),
        PROGRESS(2),
        HEART(3);

        final int val;

        MessageTypes(final int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public static MessageTypes byVal(final int val) {
            switch (val) {
                case 1: return BATTERY;
                case 2: return PROGRESS;
                case 3: return HEART;
                default: return UNKNOWN;
            }
        }
    }

    @Override
    public Reader makeReader(@Nullable BatteryStatusListener batteryStatusListener, @Nullable ConnectionStatusListener connectionStatusListener, @Nullable HeartRateListener heartRateListener) {
        return new Reader(batteryStatusListener, connectionStatusListener, heartRateListener);
    }

    @Override
    public Writer makeWriter() {
        return new Writer();
    }

    @Override
    public void destroy() {
        handler = null;
    }

    private class Reader implements HeartRateCommunication.Reader {
        private final BatteryStatusListener batteryStatusListener;
        private final ConnectionStatusListener connectionStatusListener;
        private final HeartRateListener heartRateListener;

        public Reader(BatteryStatusListener batteryStatusListener, ConnectionStatusListener connectionStatusListener, HeartRateListener heartRateListener) {
            this.batteryStatusListener = batteryStatusListener;
            this.connectionStatusListener = connectionStatusListener;
            this.heartRateListener = heartRateListener;
        }

        @Override
        public Intent bind(final Context context) {
            handler = new ReaderHandler();
            return null;
        }

        @Override
        public void destroy() {
            handler = null;
        }

        private class ReaderHandler extends Handler {
            @Override
            public void handleMessage(final Message msg) {
                switch (MessageTypes.byVal(msg.arg1)) {
                    case BATTERY:
                        if (batteryStatusListener != null) {
                            batteryStatusListener.onBatteryStatus(msg.arg2);
                        }
                        break;
                    case PROGRESS:
                        if (connectionStatusListener != null) {
                            final Bundle data = msg.getData();
                            connectionStatusListener.onConnectionStatusChange(
                                    (Class) data.getSerializable(IMPL),
                                    ConnectionStatus.valueOf(data.getString(CONNECTION_STATUS)),
                                    data.getString(DEVICE),
                                    data.getString(MESSAGE));
                        }
                        break;
                    case HEART:
                        if (heartRateListener != null) {
                            heartRateListener.onHeartRate(msg.arg2);
                        }
                        break;
                    default:
                }
            }
        }
    }

    private class Writer implements HeartRateCommunication.Writer {
        @Override
        public void bind(Context context) {
        }

        @Override
        public void destroy() {

        }

        @Override
        public void acceptHeartRate(int bpm) {
            final Message message = Message.obtain();
            message.arg1 = MessageTypes.HEART.getVal();
            message.arg2 = bpm;
            handler.sendMessage(message);
        }

        @Override
        public void acceptBatteryStatus(int percent) {
            final Message message = Message.obtain();
            message.arg1 = MessageTypes.BATTERY.getVal();
            message.arg2 = percent;
            handler.sendMessage(message);
        }

        @Override
        public void acceptConnectionStatus(final Class impl, final ConnectionStatus connectionStatus, @Nullable String device, @Nullable String msg) {
            final Bundle payload = new Bundle(3);
            payload.putSerializable(IMPL, impl);
            payload.putString(CONNECTION_STATUS, connectionStatus.name());
            payload.putString(DEVICE, device);
            payload.putString(MESSAGE, msg);

            final Message message = Message.obtain();
            message.arg1 = MessageTypes.PROGRESS.getVal();
            message.setData(payload);

            handler.sendMessage(message);
        }
    }
}
