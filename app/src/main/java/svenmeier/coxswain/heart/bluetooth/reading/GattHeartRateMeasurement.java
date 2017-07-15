package svenmeier.coxswain.heart.bluetooth.reading;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Interpret values according to GATT HRS spec 1.0
 *
 *  https://www.bluetooth.com/specifications/adopted-specifications#gattspec
 */
public class GattHeartRateMeasurement {
    public enum HeartResolution {
        UINT8, UINT16;

        private static final byte MASK = 0x01;

        public static HeartResolution parse(int flags) {
            return ((flags & MASK) > 0) ? UINT16 : UINT8;
        }
    }

    public enum ContactStatus {
        UNSUPPORTED,
        NO_SKIN_CONTACT,
        SKIN_CONTACT;

        private static final byte SUPPORTED_MASK = 0x02;
        private static final byte DETECTED_MASK = 0x04;

        public static ContactStatus parse(int flags) {
            if ((flags & SUPPORTED_MASK) == SUPPORTED_MASK) {
                return ((flags & DETECTED_MASK) == DETECTED_MASK) ? SKIN_CONTACT : NO_SKIN_CONTACT;
            } else {
                return UNSUPPORTED;
            }
        }
    }

    public enum EnergyExpendedSupport {
        NOT_PRESENT,
        AVAILABLE;

        private static final byte MASK = 0x08;

        public static EnergyExpendedSupport parse(int flags) {
            return ((flags & MASK) > 0) ? AVAILABLE : NOT_PRESENT;
        }

        public boolean isPresent() {
            return this != NOT_PRESENT;
        }
    }

    /**
     *  The RR-interval is the distance between two consecutive peaks in the
     *  ECG-curve
     */
    public enum RrSupport {
        NOT_PRESENT(0),
        MAX7(7),
        MAX8(8);

        private static final byte MASK = 0x0A;
        private final int maxCount;

        RrSupport(int maxCount) {
            this.maxCount = maxCount;
        }

        public static RrSupport parse(int flags, final HeartResolution res, final EnergyExpendedSupport ee) {
            if ((flags & MASK) > 0) {
                if (res == HeartResolution.UINT16 || ee.isPresent()) {
                    return MAX7;
                } else {
                    return MAX8;
                }
            } else {
                return NOT_PRESENT;
            }
        }

        public int getMaxCount() {
            return maxCount;
        }

        public boolean isPresent() {
            return maxCount > 0;
        }
    }

    private final HeartResolution resolution;
    private final ContactStatus contactStatus;
    private final EnergyExpendedSupport energyExpendedSupport;
    private final RrSupport rrSupport;

    private final Integer heartBpm;
    private final Integer energyJoule;
    private final List<Float> rrIntervals;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GattHeartRateMeasurement(BluetoothGattCharacteristic chr) {
        final int flags = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        this.resolution = HeartResolution.parse(flags);
        this.contactStatus = ContactStatus.parse(flags);
        this.energyExpendedSupport = EnergyExpendedSupport.parse(flags);
        this.rrSupport = RrSupport.parse(flags, resolution, energyExpendedSupport);

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

        switch (energyExpendedSupport) {
            case AVAILABLE:
                this.energyJoule = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, bytePos);
                bytePos += 2;
                break;
            default:
                this.energyJoule = null;
        }

        if (rrSupport.isPresent()) {
            this.rrIntervals = new ArrayList<>(rrSupport.getMaxCount());
            for (int i = 0; i < rrSupport.getMaxCount(); ++i) {
                final Integer raw = chr.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, bytePos);
                if (raw == null) {
                    break;
                } else {
                    final Float val = Float.valueOf(raw)
                            / 1024;
                    bytePos += 2;
                    rrIntervals.add(val);
                }
            }
        } else {
            this.rrIntervals = Collections.emptyList();
        }
    }


    public Integer getHeartBpm() {
        return heartBpm;
    }

    public Integer getEnergyJoule() {
        return energyJoule;
    }

    public ContactStatus getContactStatus() {
        return contactStatus;
    }

    /**
     *  The RR-interval is the distance between two consecutive peaks in the
     *  ECG-curve.
     *
     *  @return List of values in seconds, empty list if unsupported
     */
    public List<Float> getRrIntervals() {
        return rrIntervals;
    }

    @Override
    public String toString() {
        return "Heart rate: " + heartBpm + " (" + resolution + "), " +
                "energy: " + energyJoule + "J (" + energyExpendedSupport + "), " +
                "RR: " + rrIntervals + " (" + rrSupport + ")";
    }
}
