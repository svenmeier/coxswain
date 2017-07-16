package svenmeier.coxswain.heart.bluetooth;

public interface BluetoothHeartConnectionListener {
    void onConnected(String deviceName, String deviceId);
    void onDisconnected(String deviceName, String deviceId);
}
