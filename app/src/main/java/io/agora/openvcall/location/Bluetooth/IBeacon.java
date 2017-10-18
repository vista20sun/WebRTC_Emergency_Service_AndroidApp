package io.agora.openvcall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 */

public class IBeacon {
    private int rssi, major, minor;
    private String uuid;

    /* CONSTRUCTORS */

    public IBeacon() {
    }

    public IBeacon(int rssi, int major, int minor, String uuid) {
        this.rssi = rssi;
        this.major = major;
        this.minor = minor;
        this.uuid = uuid;
    }

    /* GETTERS AND SETTERS */

    public String getRssi() { return Integer.toString(rssi); }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getMajor() {
        return Integer.toString(major);
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public String getMinor() {
        return Integer.toString(minor);
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "IBeacon{" +
                "rssi=" + rssi +
                ", major=" + major +
                ", minor=" + minor +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
