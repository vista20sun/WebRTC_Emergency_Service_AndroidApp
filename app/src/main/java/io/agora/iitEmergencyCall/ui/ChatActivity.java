package io.agora.iitEmergencyCall.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


import io.agora.iitEmergencyCall.R;
import io.agora.iitEmergencyCall.location.Bluetooth.IBeacon;
import io.agora.iitEmergencyCall.location.Bluetooth.IBeaconCallback;
import io.agora.iitEmergencyCall.location.Bluetooth.IBeaconsCollector;
import io.agora.iitEmergencyCall.location.Bluetooth.Json;
import io.agora.iitEmergencyCall.model.AGEventHandler;
import io.agora.iitEmergencyCall.model.ConstantApp;
import io.agora.iitEmergencyCall.model.Message;
import io.agora.iitEmergencyCall.model.User;
import io.agora.propeller.Constant;
import io.agora.propeller.UserStatusData;
import io.agora.propeller.VideoInfoData;
import io.agora.propeller.preprocessing.VideoPreProcessing;
import io.agora.propeller.ui.RtlLinearLayoutManager;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

import static io.agora.iitEmergencyCall.ui.MainActivity.json;

public class ChatActivity extends BaseActivity implements AGEventHandler, IBeaconCallback {

    private final static Logger log = LoggerFactory.getLogger(ChatActivity.class);

    final  static String request ="http://rtcfacilities.herokuapp.com/svg/%s-%02d.svg";//"svg/%s-%02d.svg";

    private GridVideoViewContainer mGridVideoViewContainer;

    private RelativeLayout mSmallVideoViewDock;

    private WebView webView;

    // should only be modified under UI thread
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid

    private volatile boolean mVideoMuted = false;

    private volatile boolean mAudioMuted = false;

    private int buildingNumber; //used to be a string identifier like "SB" or "AM", but replace with the Building ID;
    private int floor;
    private boolean mockMode;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager btManager;


    private BluetoothAdapter mBluetoothAdapter;


    private RequestQueue queue;
    private volatile int mAudioRouting = -1; // Default
    Handler h = new Handler();
    int delay = 10000; //15 seconds
    Runnable runnable;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("https://www.911webrtc.com");
        } catch (URISyntaxException e) {}
    }
    public String sessionID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        sessionID=getIntent().getExtras().getString("sessionID");
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);;
        mBluetoothAdapter = btManager.getAdapter();
        webView = null;
        queue=Volley.newRequestQueue(this);
    }
    @Override
    protected void onStart() {
//start handler as activity become visible
        collectBeacon();

        /*h.postDelayed(new Runnable() {
            public void run() {

                //GPS information
                GPSTracker gps = new GPSTracker(getApplicationContext());
                if(gps.canGetLocation()) {
                    double latitude = gps.getLatitude(); // returns latitude
                    double longitude = gps.getLongitude();

                    try {
                        toSend.put("sessionID", sessionID);
                        toSend.put("latitude", latitude);
                        toSend.put("longitude", longitude);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                collectBeacon();



                runnable=this;

                h.postDelayed(runnable, delay);

            }
        }, delay);*/

        super.onStart();
    }
    protected void onPause() {
        h.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }
    public void collectBeacon(){
        // 1. Check if BT is activated
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // 2. Collect info from iBeacons
        IBeaconsCollector bc = new IBeaconsCollector(ChatActivity.this);
        bc.startBackGroundSearching(5);
    }
    private static String getJSONURL(Json json) throws JSONException {
        String str = json.readMyJson();
        str = str.substring(1,str.length()-1);
        str = str.replaceAll("\\{","json[]={");
        str = str.replaceAll("\\}\\,","}&");
        return String.format("https://api.iitrtclab.com/map/indoorlocation?%s&algorithim=1",str.toLowerCase());
    }
    public void scanFinished(Collection<IBeacon> list) {
        //logText.append("*FINISHED - iBeacons Scanned: "+ list.size() + "\n");

        // 3. Send HTTP request to Location Server
        final JSONObject toSend = new JSONObject();
        GPSTracker gps = new GPSTracker(getApplicationContext());
        if(gps.canGetLocation()) {
            double latitude = gps.getLatitude(); // returns latitude
            double longitude = gps.getLongitude();

            try {
                toSend.put("sessionID", sessionID);
                toSend.put("latitude", latitude);
                toSend.put("longitude", longitude);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        //list.add(new IBeacon(-91,1000,540,"uuid"));
        //list.add(new IBeacon(-92,1000,518,"uuid"));
        //list.add(new IBeacon(-55,1000,518,"uuid"));
        if(list.size() > 0 && !mockMode){
            // 4. Create JSON object with the iBeacons
            json = new Json();
            for (IBeacon x:list){
                try{
                    json.updateMyJsonIndoor(x.getMajor(),x.getMinor(),x.getRssi());
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            try {
                //String urlfinal = "https://api.iitrtclab.com/indoorLocation/getIndoorLocationCivicAddressJSON?test=true&json="+json.readMyJson();
                //String urlfinal ="https://api.iitrtclab.com/map/indoorlocation?%s&algorithim=1" ;
                String urlfinal = getJSONURL(json);
                Log.d("[BOSSA] - SEND", urlfinal);
                // Request a string response from the provided URL.
                JsonObjectRequest jsObjRequest = new JsonObjectRequest
                        (Request.Method.GET, urlfinal, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                Log.i("Response",response.toString());
                                int Building=buildingNumber;
                                int floor_temp=floor;
                                try{
                                    Building = Integer.parseInt(response.getString("building"));
                                    toSend.put("building_id",Building);
                                } catch (JSONException e) {
                                    Building = -1;
                                    e.printStackTrace();
                                }
                                try{
                                    toSend.put("Room",response.getString("room"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try{
                                    toSend.put("Floor",response.getString("floor"));
                                    floor_temp = Integer.parseInt(response.getString("floor"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try{
                                    toSend.put("x0",response.getString("x"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try{
                                    toSend.put("y0",response.getString("y"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mSocket.emit("caller", toSend );

                                try {
                                    if(buildingNumber==-1||buildingNumber!=Building||floor_temp!=floor){
                                        buildingNumber=Building;
                                        floor=floor_temp;
                                        updateMap(Float.parseFloat(toSend.getString("x0")),
                                                Float.parseFloat(toSend.getString("y0")),
                                                String.format(request,buildingNumber,floor));
                                    }
                                    else
                                        updateMap(Float.parseFloat(toSend.getString("x0")),Float.parseFloat(toSend.getString("y0")),null);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Log.d("[BOSSA] - RECEIVE", toSend.toString());

                            }

                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO Auto-generated method stub
                                Log.d("[NG911 HTTP Get val] ", error.toString());

                            }
                        });
                // Add the request to the RequestQueue.
                jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(1500,1,1.0f));
                queue.add(jsObjRequest);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            // TODO Send information to location server. SYNCHRONOUS METHOD
        }else if(mockMode){
            mork(toSend);
            mSocket.emit("caller", toSend );
            try {
                updateMap(Float.parseFloat(toSend.getString("x0")),
                        Float.parseFloat(toSend.getString("y0")),
                        String.format(request,"SB",Integer.parseInt(toSend.getString("Floor"))));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("[NG911 HTTP Get val] ", toSend.toString());
        }else{
            buildingNumber = -1;
        }

    }
    private String info[]={
            "106 28.0195126 50.88391361 1",
            "107 28.0195126 42.80893591 1",
            "108 28.0195126 28.22523788 1",
            "111 22.79965764 19.79185287 1",
            "113 11.94049434 19.79185287 2",
            "113 6.65061426 25.86174879 2",
            "113 5.57867978 32.47799542 2",
            "102 5.9975816 42.60105594 2",
            "103 2.94553596 51.64015332 2"
    };
    private int timeMock = 0;
    private void mork(JSONObject toSend){
        String Building = "31";//SB
        int id = (++timeMock)%info.length;
        String infos[]=info[id].split(" ");
        try {
            toSend.put("building_id",Building);
            toSend.put("Floor",infos[3]);
            toSend.put("Room",infos[0]);
            toSend.put("x0",infos[1]);
            toSend.put("y0",infos[2]);
            toSend.put("mock",true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void onShowMap(View view){
        showMapDialog();
    }

    private void showMapDialog(){
        if(buildingNumber==-1&&!mockMode){
            Toast.makeText(this, "cannot get indoor location", Toast.LENGTH_SHORT).show();
            return;
        }
        final AlertDialog.Builder mapDialog = new AlertDialog.Builder(ChatActivity.this);
        final View dialogView = LayoutInflater.from(ChatActivity.this).inflate(R.layout.map_dialog_layout,null);
        webView = (WebView)dialogView.findViewById(R.id.map_view);
        mapDialog.setTitle("IndoorLocation");
        mapDialog.setCancelable(false);
        mapDialog.setView(dialogView);
        mapDialog.setNegativeButton("close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                webView = null;
            }
        });
        AlertDialog dia = mapDialog.show();
        Window window = dia.getWindow();
        window.getDecorView().setPadding(0,0,0,0);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width=WindowManager.LayoutParams.MATCH_PARENT;
        lp.height=WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        //webView.loadUrl("https://www.google.com/maps");

        webView.loadUrl("file:///android_asset/map/index.html");
        webView.evaluateJavascript(String.format("changeSVG('%s');",String.format(request,buildingNumber,floor)), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("map-drawing", value);
            }
        });
    }

    private void updateMap(final float x,final  float y,final String map){
        h.post(new Runnable() {
            @Override
            public void run() {
                if(webView==null)
                    return;
                if(map != null){
                    webView.evaluateJavascript(String.format("changeSVG('%s',%f,%f);",map,x,y), new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.d("map-drawing", value);
                        }
                    });
                }else {
                    webView.evaluateJavascript(String.format("removeAllCircles();setBeacon(%f,%f)", x, y), new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.d("map", value);
                        }
                    });
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);

        Intent i = getIntent();

        String channelName = i.getStringExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME);

        final String encryptionKey = getIntent().getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY);

        final String encryptionMode = getIntent().getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE);

        mockMode = i.getBooleanExtra("mock",false);
        doConfigEngine(encryptionKey, encryptionMode);

        mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container);
        mGridVideoViewContainer.setItemEventHandler(new VideoViewEventListener() {
            @Override
            public void onItemDoubleClick(View v, Object item) {
                log.debug("onItemDoubleClick " + v + " " + item + " " + mLayoutType);

                if (mUidsList.size() < 2) {
                    return;
                }

                UserStatusData user = (UserStatusData) item;
                int uid = (user.mUid == 0) ? config().mUid : user.mUid;

                if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
                    switchToSmallVideoView(uid);
                } else {
                    switchToDefaultVideoView();
                }
            }
        });

        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        surfaceV.setZOrderOnTop(false);
        surfaceV.setZOrderMediaOverlay(false);

        mUidsList.put(0, surfaceV); // get first surface view

        mGridVideoViewContainer.initViewContainer(getApplicationContext(), 0, mUidsList); // first is now full view
        worker().preview(true, surfaceV, 0);

        worker().joinChannel(channelName, config().mUid);

        //TextView textChannelName = (TextView) findViewById(R.id.channel_name);
        //textChannelName.setText(channelName);

        optional();

        LinearLayout bottomContainer = (LinearLayout) findViewById(R.id.bottom_container);
        FrameLayout.MarginLayoutParams fmp = (FrameLayout.MarginLayoutParams) bottomContainer.getLayoutParams();
        fmp.bottomMargin = virtualKeyHeight() + 16;



        //initMessageList();
    }

    public void onClickHideIME(View view) {
        log.debug("onClickHideIME " + view);

        closeIME(findViewById(R.id.msg_content));

        findViewById(R.id.msg_input_container).setVisibility(View.GONE);
        findViewById(R.id.bottom_action_end_call).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_action_container).setVisibility(View.VISIBLE);
    }

    private InChannelMessageListAdapter mMsgAdapter;

    private ArrayList<Message> mMsgList;

    private void initMessageList() {
        mMsgList = new ArrayList<>();
        RecyclerView msgListView = (RecyclerView) findViewById(R.id.msg_list);

        mMsgAdapter = new InChannelMessageListAdapter(this, mMsgList);
        mMsgAdapter.setHasStableIds(true);
        msgListView.setAdapter(mMsgAdapter);
        msgListView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        msgListView.addItemDecoration(new MessageListDecoration());
    }

    private void notifyMessageChanged(Message msg) {
        mMsgList.add(msg);

        int MAX_MESSAGE_COUNT = 16;

        if (mMsgList.size() > MAX_MESSAGE_COUNT) {
            int toRemove = mMsgList.size() - MAX_MESSAGE_COUNT;
            for (int i = 0; i < toRemove; i++) {
                mMsgList.remove(i);
            }
        }

        mMsgAdapter.notifyDataSetChanged();
    }

    private int mDataStreamId;

    private void sendChannelMsg(String msgStr) {
        RtcEngine rtcEngine = rtcEngine();
        if (mDataStreamId <= 0) {
            mDataStreamId = rtcEngine.createDataStream(true, true); // boolean reliable, boolean ordered
        }

        if (mDataStreamId < 0) {
            String errorMsg = "Create data stream error happened " + mDataStreamId;
            log.warn(errorMsg);
            showLongToast(errorMsg);
            return;
        }

        byte[] encodedMsg;
        try {
            encodedMsg = msgStr.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedMsg = msgStr.getBytes();
        }

        rtcEngine.sendStreamMessage(mDataStreamId, encodedMsg);
    }

    private void optional() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void optionalDestroy() {
    }

    private int getVideoProfileIndex() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int profileIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        if (profileIndex > ConstantApp.VIDEO_PROFILES.length - 1) {
            profileIndex = ConstantApp.DEFAULT_PROFILE_IDX;

            // save the new value
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, profileIndex);
            editor.apply();
        }
        return profileIndex;
    }

    private void doConfigEngine(String encryptionKey, String encryptionMode) {
        int vProfile = ConstantApp.VIDEO_PROFILES[getVideoProfileIndex()];

        worker().configEngine(vProfile, encryptionKey, encryptionMode);
    }

    public void onBtn0Clicked(View view) {
        log.info("onBtn0Clicked " + view + " " + mVideoMuted + " " + mAudioMuted);
        showMessageEditContainer();
    }

    private void showMessageEditContainer() {
        findViewById(R.id.bottom_action_container).setVisibility(View.GONE);
        findViewById(R.id.bottom_action_end_call).setVisibility(View.GONE);
        findViewById(R.id.msg_input_container).setVisibility(View.VISIBLE);

        EditText edit = (EditText) findViewById(R.id.msg_content);

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String msgStr = v.getText().toString();
                    if (TextUtils.isEmpty(msgStr)) {
                        return false;
                    }
                    sendChannelMsg(msgStr);

                    v.setText("");

                    Message msg = new Message(Message.MSG_TYPE_TEXT,
                            new User(config().mUid, String.valueOf(config().mUid)), msgStr);
                    notifyMessageChanged(msg);

                    return true;
                }
                return false;
            }
        });

        openIME(edit);
    }

    public void onCustomizedFunctionClicked(View view) {
        log.info("onCustomizedFunctionClicked " + view + " " + mVideoMuted + " " + mAudioMuted + " " + mAudioRouting);
        if (mVideoMuted) {
            onSwitchSpeakerClicked();
        } else {
            onSwitchCameraClicked();
        }
    }

    private void onSwitchCameraClicked() {
        RtcEngine rtcEngine = rtcEngine();
        rtcEngine.switchCamera();
    }

    private void onSwitchSpeakerClicked() {
        RtcEngine rtcEngine = rtcEngine();
        rtcEngine.setEnableSpeakerphone(mAudioRouting != 3);
    }

    @Override
    protected void deInitUIandEvent() {
        optionalDestroy();

        doLeaveChannel();
        event().removeEventHandler(this);

        mUidsList.clear();
    }

    private void doLeaveChannel() {
        worker().leaveChannel(config().mChannel);
        worker().preview(false, null, 0);
    }

    public void onEndCallClicked(View view) {
        log.info("onEndCallClicked " + view);

        finish();
    }

    private VideoPreProcessing mVideoPreProcessing;

    public void onBtnNClicked(View view) {
        if (mVideoPreProcessing == null) {
            mVideoPreProcessing = new VideoPreProcessing();
        }

        ImageView iv = (ImageView) view;
        Object showing = view.getTag();
        if (showing != null && (Boolean) showing) {
            mVideoPreProcessing.enablePreProcessing(false);
            iv.setTag(null);
            iv.clearColorFilter();
        } else {
            mVideoPreProcessing.enablePreProcessing(true);
            iv.setTag(true);
            iv.setColorFilter(ContextCompat.getColor(this,R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
        }
    }

    public void onVoiceChatClicked(View view) {
        log.info("onVoiceChatClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted + " audio_status: " + mAudioMuted);
        if (mUidsList.size() == 0) {
            return;
        }

        SurfaceView surfaceV = getLocalView();
        ViewParent parent;
        if (surfaceV == null || (parent = surfaceV.getParent()) == null) {
            log.warn("onVoiceChatClicked " + view + " " + surfaceV);
            return;
        }

        RtcEngine rtcEngine = rtcEngine();
        mVideoMuted = !mVideoMuted;

        if (mVideoMuted) {
            rtcEngine.disableVideo();
        } else {
            rtcEngine.enableVideo();
        }

        ImageView iv = (ImageView) view;

        iv.setImageResource(mVideoMuted ? R.drawable.btn_video : R.drawable.btn_voice);

        hideLocalView(mVideoMuted);

        if (mVideoMuted) {
            resetToVideoDisabledUI();
        } else {
            resetToVideoEnabledUI();
        }
    }

    private SurfaceView getLocalView() {
        for (HashMap.Entry<Integer, SurfaceView> entry : mUidsList.entrySet()) {
            if (entry.getKey() == 0 || entry.getKey() == config().mUid) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void hideLocalView(boolean hide) {
        int uid = config().mUid;
        doHideTargetView(uid, hide);
    }

    private void doHideTargetView(int targetUid, boolean hide) {
        HashMap<Integer, Integer> status = new HashMap<>();
        status.put(targetUid, hide ? UserStatusData.VIDEO_MUTED : UserStatusData.DEFAULT_STATUS);
        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
        } else if (mLayoutType == LAYOUT_TYPE_SMALL) {
            UserStatusData bigBgUser = mGridVideoViewContainer.getItem(0);
            if (bigBgUser.mUid == targetUid) { // big background is target view
                mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
            } else { // find target view in small video view list
                log.warn("SmallVideoViewAdapter call notifyUiChanged " + mUidsList + " " + (bigBgUser.mUid & 0xFFFFFFFFL) + " target: " + (targetUid & 0xFFFFFFFFL) + "==" + targetUid + " " + status);
                mSmallVideoViewAdapter.notifyUiChanged(mUidsList, bigBgUser.mUid, status, null);
            }
        }
    }

    private void resetToVideoEnabledUI() {
        ImageView iv = (ImageView) findViewById(R.id.customized_function_id);
        iv.setImageResource(R.drawable.btn_switch_camera);
        iv.clearColorFilter();

        notifyHeadsetPlugged(mAudioRouting);
    }

    private void resetToVideoDisabledUI() {
        ImageView iv = (ImageView) findViewById(R.id.customized_function_id);
        iv.setImageResource(R.drawable.btn_speaker);
        iv.clearColorFilter();

        notifyHeadsetPlugged(mAudioRouting);
    }

    public void onVoiceMuteClicked(View view) {
        log.info("onVoiceMuteClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted + " audio_status: " + mAudioMuted);
        if (mUidsList.size() == 0) {
            return;
        }

        RtcEngine rtcEngine = rtcEngine();
        rtcEngine.muteLocalAudioStream(mAudioMuted = !mAudioMuted);

        ImageView iv = (ImageView) view;

        if (mAudioMuted) {
            iv.setColorFilter(ContextCompat.getColor(this,R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
        } else {
            iv.clearColorFilter();
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                if (mUidsList.containsKey(uid)) {
                    return;
                }

                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                mUidsList.put(uid, surfaceV);

                boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 2;

                surfaceV.setZOrderOnTop(!useDefaultLayout);
                surfaceV.setZOrderMediaOverlay(!useDefaultLayout);

                rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                if (useDefaultLayout) {
                    log.debug("doRenderRemoteUi LAYOUT_TYPE_DEFAULT " + (uid & 0xFFFFFFFFL));
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();
                    log.debug("doRenderRemoteUi LAYOUT_TYPE_SMALL " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    @Override
    public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
        log.debug("onJoinChannelSuccess " + channel + " " + (uid & 0xFFFFFFFFL) + " " + elapsed);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SurfaceView local = mUidsList.remove(0);

                if (local == null) {
                    return;
                }

                mUidsList.put(uid, local);
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        doRemoveRemoteUi(uid);
    }

    @Override
    public void onExtraCallback(final int type, final Object... data) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                doHandleExtraCallback(type, data);
            }
        });
    }

    private void doHandleExtraCallback(int type, Object... data) {
        int peerUid;
        boolean muted;

        switch (type) {
            case AGEventHandler.EVENT_TYPE_ON_USER_AUDIO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];

                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> status = new HashMap<>();
                    status.put(peerUid, muted ? UserStatusData.AUDIO_MUTED : UserStatusData.DEFAULT_STATUS);
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, status, null);
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED:
                peerUid = (Integer) data[0];
                muted = (boolean) data[1];

                doHideTargetView(peerUid, muted);

                break;

            case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS:
                IRtcEngineEventHandler.RemoteVideoStats stats = (IRtcEngineEventHandler.RemoteVideoStats) data[0];

                if (Constant.SHOW_VIDEO_INFO) {
                    if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                        mGridVideoViewContainer.addVideoInfo(stats.uid, new VideoInfoData(stats.width, stats.height, stats.delay, stats.receivedFrameRate, stats.receivedBitrate));
                        int uid = config().mUid;
                        int profileIndex = getVideoProfileIndex();
                        String resolution = getResources().getStringArray(R.array.string_array_resolutions)[profileIndex];
                        String fps = getResources().getStringArray(R.array.string_array_frame_rate)[profileIndex];
                        String bitrate = getResources().getStringArray(R.array.string_array_bit_rate)[profileIndex];

                        String[] rwh = resolution.split("x");
                        int width = Integer.valueOf(rwh[0]);
                        int height = Integer.valueOf(rwh[1]);

                        mGridVideoViewContainer.addVideoInfo(uid, new VideoInfoData(width > height ? width : height,
                                width > height ? height : width,
                                0, Integer.valueOf(fps), Integer.valueOf(bitrate)));
                    }
                } else {
                    mGridVideoViewContainer.cleanVideoInfo();
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS:
                IRtcEngineEventHandler.AudioVolumeInfo[] infos = (IRtcEngineEventHandler.AudioVolumeInfo[]) data[0];

                if (infos.length == 1 && infos[0].uid == 0) { // local guy, ignore it
                    break;
                }

                if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
                    HashMap<Integer, Integer> volume = new HashMap<>();

                    for (IRtcEngineEventHandler.AudioVolumeInfo each : infos) {
                        peerUid = each.uid;
                        int peerVolume = each.volume;

                        if (peerUid == 0) {
                            continue;
                        }
                        volume.put(peerUid, peerVolume);
                    }
                    mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, null, volume);
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_APP_ERROR:
                int subType = (int) data[0];

                if (subType == ConstantApp.AppError.NO_NETWORK_CONNECTION) {
                    showLongToast(getString(R.string.msg_no_network_connection));
                }

                break;

            case AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG:

                peerUid = (Integer) data[0];
                final byte[] content = (byte[]) data[1];
                notifyMessageChanged(new Message(new User(peerUid, String.valueOf(peerUid)), new String(content)));

                break;

            case AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR: {
                int error = (int) data[0];
                String description = (String) data[1];

                notifyMessageChanged(new Message(new User(0, null), error + " " + description));

                break;
            }

            case AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED:
                notifyHeadsetPlugged((int) data[0]);

                break;

        }
    }

    private void requestRemoteStreamType(final int currentHostCount) {
        log.debug("requestRemoteStreamType " + currentHostCount);
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }

                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }

                log.debug("doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL) + " " + mLayoutType);

                if (mLayoutType == LAYOUT_TYPE_DEFAULT || uid == bigBgUid) {
                    switchToDefaultVideoView();
                } else {
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    private SmallVideoViewAdapter mSmallVideoViewAdapter;

    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null) {
            mSmallVideoViewDock.setVisibility(View.GONE);
        }
        mGridVideoViewContainer.initViewContainer(getApplicationContext(), config().mUid, mUidsList);

        mLayoutType = LAYOUT_TYPE_DEFAULT;
    }

    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        mGridVideoViewContainer.initViewContainer(getApplicationContext(), bigBgUid, slice);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;

        requestRemoteStreamType(mUidsList.size());
    }

    public int mLayoutType = LAYOUT_TYPE_DEFAULT;

    public static final int LAYOUT_TYPE_DEFAULT = 0;

    public static final int LAYOUT_TYPE_SMALL = 1;

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = mUidsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, config().mUid, exceptUid, mUidsList, new VideoViewEventListener() {
                @Override
                public void onItemDoubleClick(View v, Object item) {
                    switchToDefaultVideoView();
                }
            });
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        log.debug("bindToSmallVideoView " + twoWayVideoCall + " " + (exceptUid & 0xFFFFFFFFL));

        if (twoWayVideoCall) {
            recycler.setLayoutManager(new RtlLinearLayoutManager(this, RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(config().mUid);
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    public void notifyHeadsetPlugged(final int routing) {
        log.info("notifyHeadsetPlugged " + routing + " " + mVideoMuted);

        mAudioRouting = routing;

        if (!mVideoMuted) {
            return;
        }

        ImageView iv = (ImageView) findViewById(R.id.customized_function_id);
        if (mAudioRouting == 3) { // Speakerphone
            iv.setColorFilter(ContextCompat.getColor(this,R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
        } else {
            iv.clearColorFilter();
        }
    }

    public String getBuildingName(int id){
        switch (id){
            case  31:   return "SB";
            case 4:     return "AM";
            default:    return null;
        }
    }
}
