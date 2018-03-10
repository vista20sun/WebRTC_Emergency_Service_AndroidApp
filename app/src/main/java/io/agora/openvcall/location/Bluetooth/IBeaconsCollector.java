package io.agora.openvcall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 * edited by Yuyang on 26/02/2018, replace this Collector use Android_Beacon_Library to improve the effective
 */
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class IBeaconsCollector  implements BeaconConsumer {
    // CONSTANTS
    public static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private final static String targetUUID = "fda50693-a4e2-4fb1-afcf-c6eb07647825",gateUUID="A0DF207C-142F-4A39-A457-6FC44D524C04";    //uuid for iBeacons and GateWay Devices
    private BeaconManager beaconManager;
    private Context context;
    private List<IBeacon> beaconList;                                                                                           //use hash map to support average calculation
    private Handler handler;
    private final static String debugTag="BeaconScanner:";
    private IBeaconCallback callback;
    private static final long SCAN_PERIOD = 8000;                                                                                       //searching time
    private boolean background;
    private int backgroundCounter,backgroundTimes;


    public IBeaconsCollector(Context context){
        this.context = context;
        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
        beaconList = new LinkedList<>();
        handler=new Handler();
        callback = (IBeaconCallback)context;
        background =false;
    }
    public void onBeaconServiceConnect() {                                                                                              //callback function, called when bind this object to a searching service
        beaconManager.removeAllRangeNotifiers();                                                                                        //remove all exists searching task before start new searching

        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if(beacons.size()>0){
                    for(Beacon x:beacons){                                                                                              //use iterator to access all beacons that be searched
                        Log.d(debugTag,String.format("[%d]uuid:%s\tmajor:%s\\manner:%s\\distance:%.2f<rssi:%d>\n",beacons.size(),x.getId1(),x.getId2(),x.getId3(),x.getDistance(),x.getRssi()));
                        beaconList.add(new IBeacon(x.getRssi(),x.getId2().toInt(),x.getId3().toInt(),x.getId1().toString()));


                    }
                }else
                    Log.d(debugTag,"-----no detected-----");
                backgroundReturn(2);
            }
        });

        try{
            beaconManager.startRangingBeaconsInRegion(new Region(targetUUID, Identifier.parse(targetUUID), null, null));    //start looking for iBeacons
            beaconManager.startRangingBeaconsInRegion(new Region(gateUUID,Identifier.parse(gateUUID),null,null));           //start looking for gateWay
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    public void findIBeacons(){                                                                                                     //start looking for the beacons
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                beaconManager.unbind(IBeaconsCollector.this);                                                            //stop searching by unbind scanner after a time
                callback.scanFinished(beaconList);
            }
        },SCAN_PERIOD);
        beaconManager.bind(IBeaconsCollector.this);                                                                      //start searching by bind this scanner to service
        background = false;
    }

    public void startBackGroundSearching(int time){
        background = true;
        backgroundCounter = 0;
        backgroundTimes=time;
        beaconManager.bind(IBeaconsCollector.this);
    }

    public void stopBackgroundSearching(){
        background = false;
        beaconManager.unbind(IBeaconsCollector.this);
    }

    private synchronized void backgroundReturn(int uuidNums){
        if(!background)
            return;
        backgroundCounter = (backgroundCounter+1)%(uuidNums*backgroundTimes);
        if(backgroundCounter == 0) {
            callback.scanFinished(beaconList);
            beaconList.clear();
        }

    }

    @Override
    public Context getApplicationContext() {
        return context;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);                                                                                   //callback function, just use context of the caller activity to bind and unbind service
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return context.bindService(intent,serviceConnection,i);
    }
}
