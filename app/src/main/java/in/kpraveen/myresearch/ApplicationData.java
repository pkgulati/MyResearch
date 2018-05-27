package in.kpraveen.myresearch;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ApplicationData extends Application {

    public static RequestQueue requestQueue;
    public static String accessToken = "";
    public static String TAG = "applog";
    public static String username = "";
    public static String deviceToken;
    public static boolean loggedIn;
    public static boolean testMode;
    public static final String baseUrl = "http://iot.kpraveen.in";


    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
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
}
