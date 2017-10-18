package io.agora.openvcall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 */
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.agora.openvcall.location.Bluetooth.IBeacon;
import io.agora.openvcall.location.Bluetooth.IBeaconCallback;
import io.agora.openvcall.location.Bluetooth.HexBytesConversor;
public class IBeaconsCollector {
    // CONSTANTS
    private static final long SCAN_PERIOD = 8000;
    //private final String UUID_FILTER = "A0DF207C-142F-4A39-A457-6FC44D524C04";
    private final String UUID_FILTER = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825";

    // VARIABLES
    private BluetoothAdapter btAdapter;
    private IBeaconCallback cb;
    private final BluetoothLeScanner bts;
    private Handler mHandler;
    private List<IBeacon> collectedIBeacons = new ArrayList<>();
    private boolean filledList = false;

    // CONSTRUCTOR
    public IBeaconsCollector(IBeaconCallback cb, BluetoothAdapter btAdapter) {
        this.btAdapter = btAdapter;
        this.bts = btAdapter.getBluetoothLeScanner();
        mHandler = new Handler();
        this.cb = cb;
    }

    // CALLBACK MESSAGE
    public void findIBeacons(){

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bts.stopScan(leScanCallback);
                cb.scanFinished(collectedIBeacons);
            }
        }, SCAN_PERIOD);

        bts.startScan(leScanCallback);
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            IBeacon ibeacon = parseScanInfo(result);
            if(ibeacon.getUuid().equals(UUID_FILTER))
                collectedIBeacons.add(ibeacon);
        }
    };


    // HELPERS
    // Method for parsing each iBeacon detected by the scan callback
    private IBeacon parseScanInfo(ScanResult result){
        // Convert scan result to bytes stream
        byte[] scanRecord = result.getScanRecord().getBytes();

        // Obtaining Major value
        int major = (scanRecord[25] & 0xff) * 0x100 + (scanRecord[26] & 0xff);
        // Obtaining Minor value
        int minor = (scanRecord[27] & 0xff) * 0x100 + (scanRecord[28] & 0xff);

        //Convert UUID to hex String
        byte[] uuidBytes = new byte[16];
        System.arraycopy(scanRecord, 9, uuidBytes, 0, 16);
        String hexString = HexBytesConversor.bytesToHex(uuidBytes);

        // Translating Bytes to HEX for UUID
        String uuid =  hexString.substring(0,8) + "-" +
                hexString.substring(8,12) + "-" +
                hexString.substring(12,16) + "-" +
                hexString.substring(16,20) + "-" +
                hexString.substring(20,32);

        return new IBeacon(result.getRssi(), major, minor, uuid);
    }

    /* GETTERS AND SETTERS */

    public List<IBeacon> getCollectedIBeacons() {
        return collectedIBeacons;
    }

    public void setCollectedIBeacons(List<IBeacon> collectedIBeacons) {
        this.collectedIBeacons = collectedIBeacons;
    }
}
