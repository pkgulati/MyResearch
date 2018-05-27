package in.kpraveen.myresearch;

import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class AppFirebaseInstanceIDService extends FirebaseInstanceIdService {
    public AppFirebaseInstanceIDService() {
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String deviceToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(ApplicationData.TAG, "Set new deviceToken : " + deviceToken);
        ApplicationData.deviceToken = deviceToken;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Application", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("deviceId", deviceToken);
        editor.commit();
        if (ApplicationData.loggedIn) {
            String URL = ApplicationData.baseUrl + "/api/deviceRegistrations?access_token=" + ApplicationData.accessToken;
            JSONObject data = new JSONObject();
            try {
                data.put("deviceToken", deviceToken);
                data.put("application", "MyResearch");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final String mRequestBody = data.toString();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(ApplicationData.TAG, "posting device success");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(ApplicationData.TAG, "posting device token error " + error.toString());
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

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
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
}

