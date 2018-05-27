package in.kpraveen.myresearch;


import android.app.AlarmManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

public class UserConfiguration {

    public long alarm1 = 0;
    public long alarm2 = 0;
    public long alarm3 = 0;
    public boolean usePeriodicJob = true;
    public boolean useNonPeriodicJob = true;
    public long periodicJobInterval = 1200000L;
    public long locationListenerPeriod = 40000;
    public boolean sleepInNight = true;
    public boolean liveLocationOnMoving = true;
    public static UserConfiguration instance = new UserConfiguration();
    private static boolean loaded = false;
    private boolean disable = false;
    public float liveSpeedLimit = 1.0f;
    public float minimumDistance = 100;
    public long runningJobInterval = AlarmManager.INTERVAL_HALF_HOUR;
    public long researchJobInterval = 1200000L;

    public static void load(Context context) {
        if (!loaded) {
            SharedPreferences pref = context.getSharedPreferences("Configuration", Context.MODE_PRIVATE);
            String stringValue = pref.getString("data", "");
            if (!stringValue.isEmpty()) {
                Gson gson = new Gson();
                UserConfiguration config = gson.fromJson(stringValue, UserConfiguration.class);
                if (config != null) {
                    instance = config;
                }
            }
            loaded = true;
        }
    };

    public static void fetch(final Context context) {
        String url = ApplicationData.baseUrl + "/api/userconfiguration/me?access_token=";
        url = url + ApplicationData.accessToken;
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(ApplicationData.TAG, "configuration received ");
                SharedPreferences pref = context.getSharedPreferences("Configuration", Context.MODE_PRIVATE);
                String currentValue = pref.getString("data", "{}");
                if (!currentValue.equals(response)) {
                    Log.d(ApplicationData.TAG, "configuration changed");
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("data", response);
                    editor.commit();
                    loaded = false;
                    load(context);
                    Log.d(ApplicationData.TAG, "new value " + instance.usePeriodicJob);
                    if (instance.disable) {
                        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(context.JOB_SCHEDULER_SERVICE);
                        if (jobScheduler != null) {
                            jobScheduler.cancelAll();
                        }
                    } else {
                        LocationJob.scheduleJob(LocationJob.START_JOB_ID, context, 1000);
                    }
                } else {
                    Log.d(ApplicationData.TAG, "no configuration changed");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(ApplicationData.TAG, "configuration fetch Error " + error.toString());
            }
        });
        int MY_SOCKET_TIMEOUT_MS = 10000;
        request.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationData.requestQueue.add(request);
    }

}
