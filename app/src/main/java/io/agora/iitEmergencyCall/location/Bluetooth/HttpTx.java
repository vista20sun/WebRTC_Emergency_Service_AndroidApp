package io.agora.iitEmergencyCall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 */
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;

import io.agora.iitEmergencyCall.ui.MainActivity;

public class HttpTx {
    String result;
    public static String url = "http://nead.bramsoft.com/indexupdate.php";
    private RequestQueue queue;

    public void HttpGetRequest(String url, final Context context, String json,final VolleyCallback callback) throws JSONException {

        //get to the server
        if (queue==null)
            queue = Volley.newRequestQueue(context.getApplicationContext());
        String urlfinal = url + "json=" + json;
        Log.i("[NG911 HTTP Get val] ", urlfinal);
        StringRequest myReq = new StringRequest(Request.Method.GET, urlfinal,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        result = response;
                        //Log.d("Response", response);

                        //FileOperations op = new FileOperations(context.getApplicationContext());
                        //op.write("LocServerResponse", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Response Error HTTP GET", error + "");

                        // in case of an error, create an empty response with no location on it
                        // and send it on the INVITE
                        String response = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><presence></presence>";

                        // FileOperations op = new FileOperations(context.getApplicationContext());
                        // op.write("LocServerResponse", response);

                        if (error instanceof NoConnectionError) {
                            Log.d("NoConnectionError", "NoConnectionError.......");
                            //turn wifi on if it is off


                        }
                    }
                }
        );
        //Set maximum timeout to support NG911 Location Provider response
        myReq.setRetryPolicy(new DefaultRetryPolicy(
                10000,  //maximum timeout set to 10s
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(myReq);
    }

    public void HttpGetRequest(String json) {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.c);
        String urlfinal = url + "?json=" + json;
        Log.i("[NG911 HTTP Get val] ", urlfinal);
        StringRequest myReq = new StringRequest(Request.Method.GET, urlfinal,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        result = response;
                        // Log.d("Response", response);

                        //FileOperations op = new FileOperations(context.getApplicationContext());
                        //op.write("LocServerResponse", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Response Error HTTP GET", error + "");

                        // in case of an error, create an empty response with no location on it
                        // and send it on the INVITE
                        String response = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><presence></presence>";

                        // FileOperations op = new FileOperations(context.getApplicationContext());
                        // op.write("LocServerResponse", response);


                    }
                }
        );
        //Set maximum timeout to support NG911 Location Provider response
        myReq.setRetryPolicy(new DefaultRetryPolicy(
                10000,  //maximum timeout set to 10s
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(myReq);
    }
}
