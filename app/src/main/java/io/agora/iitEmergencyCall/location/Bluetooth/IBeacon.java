package io.agora.iitEmergencyCall.location.Bluetooth;

import java.util.LinkedList;

/**
 * Created by enzop on 25/05/2017.
 * Edited by Yuyang on 26/02/2018
 */

public class IBeacon {
    private int major, minor;
    private String uuid;
    private LinkedList<Integer> rssiSet; // add a list to record all rssi value of a specific beacon, support for both calculate avg rssi on phone and WebRTC_server

    /* CONSTRUCTORS */

    public IBeacon() {
    }

    public IBeacon(int rssi, int major, int minor, String uuid) {
        rssiSet = new LinkedList<>();
        rssiSet.add(rssi);
        this.major = major;
        this.minor = minor;
        this.uuid = uuid;
    }

    /* GETTERS AND SETTERS */

    public String getRssi() {
        int sum = 0;
        for (int x:rssiSet)
            sum+=x;
        return String.valueOf(sum/rssiSet.size());
    }

    public void add(int rssi) {
        rssiSet.add(rssi);
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
    public long getKey(){
        return getKey(major,minor);
    }
    public static long getKey(int major,int minor){
        return Long.parseLong(""+major+minor);
    }

    @Override
    public String toString() {
        return "IBeacon{" +
                "rssi=" + getRssi() +
                ", major=" + major +
                ", minor=" + minor +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
