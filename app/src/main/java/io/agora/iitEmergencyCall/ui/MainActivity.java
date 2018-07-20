package io.agora.iitEmergencyCall.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.agora.iitEmergencyCall.R;
import io.agora.iitEmergencyCall.model.ConstantApp;
import io.agora.iitEmergencyCall.ui.GPSTracker;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import io.agora.iitEmergencyCall.location.Bluetooth.*;



public class MainActivity extends BaseActivity implements IBeaconCallback{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static int REQUEST_LOCATION = 0x114514;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("https://www.911webrtc.com");
        } catch (URISyntaxException e) {}
    }

    // BT attributes
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager btManager;
    private BluetoothAdapter mBluetoothAdapter;

    private List<IBeacon> collectedIBeacons = new ArrayList<>();

    //private TextView logText;
    public static Json json;
    public static String url = "http://nead.bramsoft.com/indexupdate.php?test=true&";//ip of the location server (will respond with XML file)
    HttpTx httptx;
    public static Context c;
    public Button button;
    RequestQueue queue;
    public ProgressDialog dialog;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSocket.connect();
        //logText = (TextView)findViewById(R.id.logText);
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);;
        mBluetoothAdapter = btManager.getAdapter();
        queue = Volley.newRequestQueue(this);
        httptx=new HttpTx();
        button = (Button) findViewById(R.id.button_join);
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Pressed
                    button.setBackgroundResource(R.drawable.pushbutton);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Released
                    button.setBackgroundResource(R.drawable.telbutton2);
                    dialog = ProgressDialog.show(MainActivity.this, "",
                            "Processing your call. Please wait...", true);
                    button.setClickable(false);
                    findChannelWithGPS(false);
                }
                return true;
            }
        });
        Button btn = findViewById(R.id.button_mock);
        btn.setVisibility(getResources().getBoolean(R.bool.mock)?View.VISIBLE:View.GONE);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "",
                        "Processing your call. Please wait...", true);
                findChannelWithGPS(true);
            }
        });
    }

    @Override
    protected void initUIandEvent() {
        /*EditText v_channel = (EditText) findViewById(R.id.channel_name);
        v_channel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmpty = TextUtils.isEmpty(s.toString());
                findViewById(R.id.button_join).setEnabled(!isEmpty);
            }
        });*/

        Spinner encryptionSpinner = (Spinner) findViewById(R.id.encryption_mode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.encryption_mode_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encryptionSpinner.setAdapter(adapter);

        encryptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vSettings().mEncryptionModeIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        encryptionSpinner.setSelection(vSettings().mEncryptionModeIndex);

        String lastChannelName = vSettings().mChannelName;
        /*if (!TextUtils.isEmpty(lastChannelName)) {
            v_channel.setText(lastChannelName);
            v_channel.setSelection(lastChannelName.length());
        }*/

        EditText v_encryption_key = (EditText) findViewById(R.id.encryption_key);
        String lastEncryptionKey = vSettings().mEncryptionKey;
        if (!TextUtils.isEmpty(lastEncryptionKey)) {
            v_encryption_key.setText(lastEncryptionKey);
        }
    }

    @Override
    protected void deInitUIandEvent() {
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                forwardToSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*public void onClickJoin(View view) {
        forwardToRoom();
    }*/

    //add String Channel
    public void forwardToRoom(String channel,String sessionID,boolean mock) {
        //EditText v_channel = (EditText) findViewById(R.id.channel_name);
        //v_channel.setText(channel);
        vSettings().mChannelName = channel;

        EditText v_encryption_key = (EditText) findViewById(R.id.encryption_key);
        //encryption : AES-256-XTS
        String encryption = v_encryption_key.getText().toString();
        vSettings().mEncryptionKey = encryption;

        Intent i = new Intent(MainActivity.this, ChatActivity.class);
        i.putExtra("sessionID",sessionID);
        i.putExtra("mock",mock);
        i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, channel);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY, encryption);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE, getResources().getStringArray(R.array.encryption_mode_values)[vSettings().mEncryptionModeIndex]);

        startActivity(i);
    }
    public void findChannelWithGPS(final boolean mock){
        //forwardToRoom(channel[0],sessionID[0]);
        //collectBeacon();
        final String[] channel = {""};
        final String[] sessionID = {""};
        final double latitude;
        final double longitude;
        GPSTracker gps = new GPSTracker(this);
        if(gps.canGetLocation()){
            latitude=gps.getLatitude(); // returns latitude
            longitude=gps.getLongitude();
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(this);
            //logText.setText(""+latitude);
            //collectBeacon();
            //String url ="https://www.911webrtc.com/api/android/getChannelGPS?latitude="+latitude+"&longitude="+longitude;
            String url = "https://www.911webrtc.com/api/android/getChannelGPS?latitude=41.841389&longitude=-87.622272";
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i("Response",response.toString());
                            try {
                                channel[0]=response.getString("channel");
                                Log.i("Response",channel[0]);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                sessionID[0]=response.getString("sessionID");
                                Log.i("Response",sessionID[0]);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dialog.hide();
                            JSONObject toSend= new JSONObject();
                            try {
                                toSend.put("sessionID", sessionID[0]);
                                toSend.put("latitude", latitude);
                                toSend.put("longitude", longitude);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSocket.emit("caller", toSend );
                            Log.d("IDTEST", "channel: "+channel[0]);
                            Log.d("IDTEST", "sessionID: "+sessionID[0]);
                            forwardToRoom(channel[0],sessionID[0],mock);

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            //forwardToRoom("","",mock);
                            Log.d("[request error]","no response" );
                            error.printStackTrace();
                            dialog.hide();
                            Toast.makeText(MainActivity.this, "all channel is busy", Toast.LENGTH_SHORT).show();
                        }
                    });
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(1000,3,1f));
            // Add the request to the RequestQueue.
            queue.add(jsObjRequest);

        }

    }
    public void forwardToSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }
    public void collectBeacon(){
        // 1. Check if BT is activated
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // 2. Collect info from iBeacons
        IBeaconsCollector bc = new IBeaconsCollector(MainActivity.this);
        bc.findIBeacons();
    }
    @Override
    public void scanFinished(Collection<IBeacon> list)  {
        //logText.append("*FINISHED - iBeacons Scanned: "+ list.size() + "\n");

        // 3. Send HTTP request to Location Server
        if(list.size() > 0){
            // 4. Create JSON object with the iBeacons
            json = new Json();
            for (IBeacon x:list){
                try {
                    json.updateMyJsonIndoor(x.getMajor(),x.getMinor(),x.getRssi());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            RequestQueue queue = Volley.newRequestQueue(this);
            try {
                String urlfinal = "https://api.iitrtclab.com/indoorLocation/getIndoorLocationCivicAddressJSON?test=true&json="+json.readMyJson();
                Log.i("[NG911 HTTP Get val] ", urlfinal);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, urlfinal,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.i("[XML received] ", response);
                                //looking for county balise


                                //logText.setText(response);
                                //findChannelWithBluetooth(response);

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //logLat.setText("That didn't work!");
                    }
                });
                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // TODO Send information to location server. SYNCHRONOUS METHOD
        }else{
            findChannelWithGPS(false);
        }

    }
    /*public void findChannelWithBluetooth(String xml){
        final String[] channel = {""};
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://ec2-54-244-212-226.us-west-2.compute.amazonaws.com:8080/channel/bluetooth?xml="+xml;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            channel[0] =response;
                            //ad channel[0]
                            forwardToRoom(channel[0]);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //logLat.setText("That didn't work!");
                }
            });
            // Add the request to the RequestQueue.
        queue.add(stringRequest);



    }*/

}
