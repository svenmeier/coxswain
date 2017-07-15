package svenmeier.coxswain.heart.bluetooth.constants;

import android.util.Log;

import java.util.List;

import svenmeier.coxswain.Coxswain;

public class GattHeartRateMeasurement {

    public enum HeartResolution {
        UINT8, UINT16;

        private static final byte MASK = 0x01;

        public static HeartResolution parse(byte flags) {
            return ((flags & MASK) != MASK) ? UINT16 : UINT8;
        }
    }

    public enum ContactStatus {
        UNSUPPORTED,
        NOT_DETECTED,
        DETECTED;

        private static final byte SUPPORTED_MASK = 0x02;
        private static final byte DETECTED_MASK = 0x04;

        public static ContactStatus parse(byte flags) {
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

        public static EnergyExpended parse(byte flags) {
            return ((flags & MASK) != MASK) ? AVAILABLE : NOT_PRESENT;
        }
    }

    public enum RR {
        NOT_PRESENT,
        AVAILABLE;

        private static final byte MASK = 0x0A;

        public static RR parse(byte flags) {
            return ((flags & MASK) != MASK) ? AVAILABLE : NOT_PRESENT;
        }
    }

    private final HeartResolution resolution;
    private final ContactStatus contactStatus;
    private final EnergyExpended energyExpended;
    private final RR rr;

    private final Integer heartBpm;
    private final Integer energyJoule;

    public GattHeartRateMeasurement(List<Byte> data) {
        Log.d(Coxswain.TAG, "Parsing data " + data);
        if ((data != null) && (data.size() > 0)) {
            resolution = HeartResolution.parse(data.get(0));
            contactStatus = ContactStatus.parse(data.get(0));
            energyExpended = EnergyExpended.parse(data.get(0));
            rr = RR.parse(data.get(0));

            if ((data.size() > 1) && (resolution == HeartResolution.UINT8)) {
                heartBpm = (int) data.get(1) & 0xFF;
            } else if ((data.size() > 1) && (resolution == HeartResolution.UINT16)) {
                heartBpm =
                        ((int) data.get(1) & 0xFF) << 8 +
                                ((int) data.get(2) & 0xFF);
            } else {
                heartBpm = null;
            }

            if ((data.size() > 2) && (resolution == HeartResolution.UINT8) && (energyExpended == EnergyExpended.AVAILABLE)) {
                energyJoule = (int) data.get(2) & 0xFF << 8 +
                        (int) data.get(3) & 0xFF;
            } else if ((data.size() > 2) && (resolution == HeartResolution.UINT16) && (energyExpended == EnergyExpended.AVAILABLE)) {
                energyJoule =
                        ((int) data.get(3) & 0xFF) << 8 +
                                ((int) data.get(4) & 0xFF);
            } else {
                energyJoule = null;
            }
        } else {
            resolution = null;
            contactStatus = null;
            energyExpended = null;
            rr = null;
            heartBpm = null;
            energyJoule = null;
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
