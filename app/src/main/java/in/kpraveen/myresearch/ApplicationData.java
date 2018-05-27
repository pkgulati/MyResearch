package in.kpraveen.myresearch;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ApplicationData extends Application {

    public static RequestQueue requestQueue;
    public static String accessToken = "";
    public static String TAG = "mylog";
    public static String username = "";
    public static String deviceToken = "";
    public static boolean loggedIn = false;
    public static boolean testMode = false;
    public static final String baseUrl = "http://iot.kpraveen.in";
    public static String userId = "";
    private static JSONArray activities = new JSONArray();

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
        SharedPreferences sharedPref = getSharedPreferences("Application", Context.MODE_PRIVATE);
        ApplicationData.accessToken = sharedPref.getString("accessToken", "");
        ApplicationData.deviceToken = sharedPref.getString("deviceId", "");
        ApplicationData.username = sharedPref.getString("username", "");
        ApplicationData.loggedIn = sharedPref.getBoolean("loggedIn", false);
        Log.d(ApplicationData.TAG, "TeamApplication onCreate, loggedin " + ApplicationData.loggedIn + " " + ApplicationData.username);
        SharedPreferences datapref = getSharedPreferences("JData", Context.MODE_PRIVATE);
        ApplicationData.testMode = datapref.getBoolean("testMode", false);
        String contacts = datapref.getString("contacts", "");
    }

    public static void addActivity(JSONObject obj) {
        try {
            String id = UUID.randomUUID().toString();
            obj.put("id", id);
            long time = System.currentTimeMillis();
            obj.put("time", time);
            obj.put("name", ApplicationData.username);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            String formattedTime = df.format(c.getTime());
            obj.put("justtime", formattedTime);
            activities.put(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void synchronizeActivities(Context context) {

        SharedPreferences pref = context.getSharedPreferences("Login", 0); // 0 - for private mode
        String accessToken = pref.getString("accessToken", "");
        String URL = ApplicationData.baseUrl + "/api/activities/synchronize?access_token=" + accessToken;
        JSONObject obj = new JSONObject();

        final String mRequestBody = activities.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        if (obj.isNull("error")) {
                            JSONObject data = obj.getJSONObject("data");
                            String id = data.getString("id");
                            activities = new JSONArray();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(ApplicationData.TAG, "location posting error " + error.toString());
                String message = null;
                if (error instanceof NetworkError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof ServerError) {
                    message = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof ParseError) {
                    message = "Parsing error! Please try again after some time!!";
                } else if (error instanceof NoConnectionError) {
                    message = "Cannot connect to Internet...Please check your connection!";
                } else if (error instanceof TimeoutError) {
                    message = "Connection TimeOut! Please check your internet connection.";
                } else if (error != null && error.networkResponse != null) {
                    //get status code here
                    final String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    try {
                        String body = new String(error.networkResponse.data, "UTF-8");
                        try {
                            JSONObject response = new JSONObject(body);
                            JSONObject response2 = (JSONObject) (response.get("error"));
                            message = response2.getString("message");
                        } catch (Throwable e) {
                        }
                    } catch (UnsupportedEncodingException e) {
                        // exception
                    }
                }
                if (message == null) {
                    message = "Error occured in posting location";
                }
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("app", "Unsupported Encoding ");
                    return null;
                }
            }
        };
        int MY_SOCKET_TIMEOUT_MS = 20000;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationData.requestQueue.add(stringRequest);
    }


}
