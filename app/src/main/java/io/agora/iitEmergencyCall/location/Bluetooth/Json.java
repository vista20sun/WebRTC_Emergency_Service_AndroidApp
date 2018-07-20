package io.agora.iitEmergencyCall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 */
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Json {
    /**
     * @param Uuid
     * @param Major
     * @param Minor
     * @param Rssi
     * @return
     */

    /**
     * remove redundancy variables and methods
     */
    //JSONObject jsonObOutdoor;
    JSONArray beaconsJson;
    //JSONObject main;

    //private boolean inited;
    public Json() {
        //jsonOb = new JSONObject();
        //jsonObOutdoor = new JSONObject();
        beaconsJson = new JSONArray();
        //main=new JSONObject();
        //inited =false;
    }

/*    private void createMyJsonIndoor(int major, int minor,int rssi) throws JSONException {
        jsonOb.put("Major", major);
        jsonOb.put("Minor", minor);
        jsonOb.put("Rssi", rssi);
        beaconsJson.put(jsonOb);
        //inited = true;
        //main.put("location",beaconsJson);
    }*/
    public void updateMyJsonIndoor( String Major, String Minor, String Rssi) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("Major", Integer.parseInt(Major));
        json.put("Minor", Integer.parseInt(Minor));
        json.put("Rssi", Integer.parseInt(Rssi));
        beaconsJson.put(json);
        //main.put("location",beaconsJson);
    }

    public void createMyJsonOutdoor(Context c) throws JSONException {
        //LocationHelper location=new LocationHelper(c);
        //jsonObOutdoor.put("Lat", location.getLocation().getLatitude());
        //jsonObOutdoor.put("Long", location.getLocation().getLongitude());
        //beaconsJson.put(jsonObOutdoor);
        //main.put("location",beaconsJson);
    }

    public String readMyJson()throws JSONException {
        //fjrois
        Log.d("readMyJson JsonCLASS",beaconsJson.toString());
        return beaconsJson.toString();
        //return main.toString();
    }

}
