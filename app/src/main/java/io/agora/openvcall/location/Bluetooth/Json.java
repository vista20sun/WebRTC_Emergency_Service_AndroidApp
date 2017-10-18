package io.agora.openvcall.location.Bluetooth;

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
    String Uuid;
    String Major;
    String Minor;
    String Rssi;
    JSONObject jsonOb;
    JSONObject jsonObOutdoor;
    JSONArray beaconsJson;
    JSONObject main;

    public Json(String Uuid, String Major, String Minor, String Rssi){
        this.Uuid=Uuid;
        this.Major=Major;
        this.Minor=Minor;
        this.Rssi=Rssi;
        jsonOb = new JSONObject();
        jsonObOutdoor = new JSONObject();
        beaconsJson = new JSONArray();
        main=new JSONObject();
    }

    public Json() {
        jsonOb = new JSONObject();
        jsonObOutdoor = new JSONObject();
        beaconsJson = new JSONArray();
        main=new JSONObject();
    }

    public void createMyJsonIndoor() throws JSONException {
        jsonOb.put("Major", Integer.parseInt(Major));
        jsonOb.put("Minor", Integer.parseInt(Minor));
        jsonOb.put("Rssi", Integer.parseInt(Rssi));
        beaconsJson.put(jsonOb);
        //main.put("location",beaconsJson);
    }
    public void updateMyJsonIndoor( String Major, String Minor, String Rssi) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("Major", Integer.parseInt(Major));
        json.put("Minor", Integer.parseInt(Minor));
        json.put("Rssi", Integer.parseInt(Rssi));
        beaconsJson.put(json);
        //main.put("location",beaconsJson);//
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
        Log.i("readMyJson JsonCLASS",beaconsJson.toString());


        return beaconsJson.toString();
        //return main.toString();
    }

}
