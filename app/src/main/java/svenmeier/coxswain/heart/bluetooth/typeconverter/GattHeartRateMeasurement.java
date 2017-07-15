package svenmeier.coxswain.heart.bluetooth.typeconverter;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class GattHeartRateMeasurement {
    public enum HeartResolution {
        UINT8, UINT16;

        private static final byte MASK = 0x01;

        public static HeartResolution parse(int flags) {
            return ((flags & MASK) != MASK) ? UINT16 : UINT8;
        }
    }

    public enum ContactStatus {
        UNSUPPORTED,
        NOT_DETECTED,
        DETECTED;

        private static final byte SUPPORTED_MASK = 0x02;
        private static final byte DETECTED_MASK = 0x04;

        public static ContactStatus parse(int flags) {
            if ((flags & SUPPORTED_MASK) == SUPPORTED_MASK) {
                return ((flags & DETECTED_MASK) == DETECTED_MASK) ? DETECTED : NOT_DETECTED;
            } else {
                return UNSUPPORTED;
            }
        }
    }

    public enum EnergyExpended {
        NOT_PRESENT,
        AVAILABLE;

        private static final byte MASK = 0x08;

        public static EnergyExpended parse(int flags) {
            return ((flags & MASK) != MASK) ? AVAILABLE : NOT_PRESENT;
        }
    }

    public enum RR {
        NOT_PRESENT,
        AVAILABLE;

        private static final byte MASK = 0x0A;

        public static RR parse(int flags) {
            return ((flags & MASK) != MASK) ? AVAILABLE : NOT_PRESENT;
        }
    }

    private final HeartResolution resolution;
    private final ContactStatus contactStatus;
    private final EnergyExpended energyExpended;
    private final RR rr;

    private final Integer heartBpm;
    private final Integer energyJoule;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GattHeartRateMeasurement(BluetoothGattCharacteristic chr) {
        final int flags = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        this.resolution = HeartResolution.parse(flags);
        this.contactStatus = ContactStatus.parse(flags);
        this.energyExpended = EnergyExpended.parse(flags);
        this.rr = RR.parse(flags);

        int bytePos = 1;
        switch (resolution) {
            case UINT8:
                this.heartBpm = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, bytePos);
                bytePos++;
                break;
            case UINT16:
                this.heartBpm = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, bytePos);
                bytePos += 2;
                break;
            default:
                this.heartBpm = null;
        }

        switch (energyExpended) {
            case AVAILABLE:
                this.energyJoule = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, bytePos);
                bytePos += 2;
                break;
            default:
                this.energyJoule = null;
        }
    }


    public Integer getHeartBpm() {
        return heartBpm;
    }

    public Integer getEnergyJoule() {
        return energyJoule;
    }

    @Override
    public String toString() {
        return "Heart rate: " + heartBpm + " (" + resolution + "), energy: " + energyJoule + "J, " + rr;
    }
}
