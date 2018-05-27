package in.kpraveen.myresearch;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


// This will be run by JobScheduler
// it will check two approximate location at gap of 3 mins to decide whether
// user has left home / office / at some base location

public class LocationJob extends JobService {

    public static int START_JOB_ID = 201;
    public static int END_JOB_ID = 250;

    private CountDownTimer mTimer;

    private int FOREGROUND_NOTIFICATION = 165;

    private LocationListener networkListener;
    private LocationListener gpsListener;

    public Location lastNetworkLocation = null;
    public Location lastGPSLocation = null;
    private LocationManager locationManager;
    private float batteryPercentage = 0;
    private ArrayList<Location> list = new ArrayList<Location>();
    private JobParameters mParams;
    private CountDownTimer mLocationTimer;
    private CountDownTimer mNextJobTimer;
    private int numNetworkErrors = 0;
    private long nextJobExecutionTime = 0;
    private long jobExpiryTime = 0;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    private boolean trackMotion = false;
    private int nextJobId = 0;
    private int numGPSResults = 0;

    static void scheduleJob(int jobId, Context context, long latency) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(jobId, componentName)
                    .setMinimumLatency(latency)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(ApplicationData.TAG, "Scheduled " + jobId + " latency " + latency);
            }
        }
    }



    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    // GPS received receivedGPS
                    // gpsreceived
                    locationReceived(location);
                    numGPSResults++;
                    lastGPSLocation = location;
                    postData();
                    if (location.getAccuracy() < 20 || numGPSResults >= 2) {
                        Log.d(ApplicationData.TAG, "GPS enough received");
                        if (mLocationTimer != null) {
                            mLocationTimer.cancel();
                        }
                        stopGPS();
                        stopNetwork();
                        list.clear();
                        startNextJobTimer();
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(ApplicationData.TAG, "GPS change");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(ApplicationData.TAG, "GPS is enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(ApplicationData.TAG, "GPS is disabled");
            }
        };

        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    // networkreceived
                    locationReceived(location);
                    lastNetworkLocation = location;
                    if (lastGPSLocation != null) {
                        stopNetwork();
                    } else {
                        // may be network location difference will determine speed
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(ApplicationData.TAG, "netowrok location changed");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(ApplicationData.TAG, "netowrok location is enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(ApplicationData.TAG, "netwrok location is disabled");
            }
        };

        lastGPSLocation = null;
        lastNetworkLocation = null;
    }

    private void locationReceived(Location location) {
        long age = (System.currentTimeMillis() - location.getTime()) / 1000;
        Log.d(ApplicationData.TAG, "location provider " + location.getProvider() + " time " + location.getTime() + ", age " + age + " accuracy " + location.getAccuracy());
        list.add(location);
    }

    private void startGPS() {
        try {
            Log.d(ApplicationData.TAG, "start GPS");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, gpsListener);
        } catch (SecurityException e) {
            Log.e(ApplicationData.TAG, "Check permission for GPS");
        }
    }

    private void stopGPS() {
        try {
            Log.d(ApplicationData.TAG, "stop GPS");
            locationManager.removeUpdates(gpsListener);
        } catch (SecurityException e) {
            Log.d(ApplicationData.TAG, "Check permission for GPS");
        }
    }

    private void startNetwork() {
        try {
            Log.d(ApplicationData.TAG, "start Network");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 10, networkListener);
        } catch (SecurityException e) {
        }
    }

    private void stopNetwork() {
        try {
            Log.d(ApplicationData.TAG, "stop network");
            locationManager.removeUpdates(networkListener);
        } catch (SecurityException e) {
        }
    }

    public void startLocationRequests() {
        lastNetworkLocation = null;
        lastGPSLocation = null;
        numGPSResults = 0;
        startGPS();
        startNetwork();
        startLocationTimer();
    }

    public void postData() {
        JSONObject activity = new JSONObject();
        try {
            activity.put("type", "LocationJobResult");
            activity.put("batteryPercentage", batteryPercentage);
            activity.put("gpsEnabled", gpsEnabled);
            activity.put("jobId", mParams.getJobId());
            activity.put("networkEnabled", networkEnabled);
            JSONArray locations = new JSONArray();
            long time = System.currentTimeMillis();
            if (lastGPSLocation != null) {
                activity.put("gpsLocation", true);
                activity.put("gpsLatitude", lastGPSLocation.getLatitude());
                activity.put("gpsLongitude", lastGPSLocation.getLongitude());
                activity.put("gpsAccuracy", lastGPSLocation.getAccuracy());
                activity.put("gpsLocationTime", lastGPSLocation.getTime());
                long minutesOld = System.currentTimeMillis() - lastGPSLocation.getTime();
                minutesOld = minutesOld / 60000;
                activity.put("gpsMinutesOld", minutesOld);
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                String formattedTime = df.format(lastGPSLocation.getTime());
                activity.put("gpsTime", formattedTime);
                activity.put("gpsHasSpeed", lastGPSLocation.hasSpeed());
                activity.put("gpsSpeed", lastGPSLocation.getSpeed());
            } else {
                activity.put("gpsLocation", false);
            }
            if (lastNetworkLocation != null) {
                activity.put("networkLocation", true);
                activity.put("networkLatitude", lastNetworkLocation.getLatitude());
                activity.put("networkLongitude", lastNetworkLocation.getLongitude());
                activity.put("networkAccuracy", lastNetworkLocation.getAccuracy());
                activity.put("networkLocationTime", lastNetworkLocation.getTime());
                long minutesOld = System.currentTimeMillis() - lastNetworkLocation.getTime();
                minutesOld = minutesOld / 60000;
                activity.put("netWorkMinutesOld", minutesOld);
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                String formattedTime = df.format(lastNetworkLocation.getTime());
                activity.put("networkTime", formattedTime);
                activity.put("networkHasSpeed", lastNetworkLocation.hasSpeed());
                activity.put("networkSpeed", lastNetworkLocation.getSpeed());
            } else {
                activity.put("networkLocation", false);
            }
            String ssid = "";
            WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    ssid = info.getSSID();
                }
            }
            CellTowerInfo cellTowerInfo = new CellTowerInfo(this);
            cellTowerInfo.getInfo();
            activity.put("wifissid", ssid);
            activity.put("mcc", cellTowerInfo.mcc);
            activity.put("mnc", cellTowerInfo.mnc);
            activity.put("lac", cellTowerInfo.lac);
            activity.put("psc", cellTowerInfo.psc);
            activity.put("cid", cellTowerInfo.cid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ApplicationData.addActivity(activity);
        ApplicationData.synchronizeActivities(this);
    }

    public void startLocationTimer() {
        mLocationTimer = new CountDownTimer(30000, 30000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Log.d(ApplicationData.TAG, "location timer over post data");
                stopGPS();
                stopNetwork();
                postData();
                list.clear();
                startNextJobTimer();
            }
        };
        mLocationTimer.start();
    }

    public void startNextJobTimer() {
        mNextJobTimer = new CountDownTimer(30000, 30000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                Log.d(ApplicationData.TAG, "NextJobTimer");
                String url = ApplicationData.baseUrl + "/api/userinfo/me?access_token=";
                url = url + ApplicationData.accessToken;
                StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            nextJobExecutionTime = obj.getLong("nextJobExecutionTime");
                            Log.d(ApplicationData.TAG, "nextJobExecutionTime  " + nextJobExecutionTime);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        long latency = AlarmManager.INTERVAL_HOUR;
                        latency = nextJobExecutionTime - System.currentTimeMillis();
                        if (latency < 30000) {
                            latency = AlarmManager.INTERVAL_HOUR;
                        }
                        Log.d(ApplicationData.TAG, "latency " + latency);
                        nextJobId = mParams.getJobId() + 1;
                        if (nextJobId > END_JOB_ID) {
                            nextJobId = START_JOB_ID;
                        }
                        scheduleJob(nextJobId, LocationJob.this, latency);
                        Log.d(ApplicationData.TAG, "finish the Job");
                        jobFinished(mParams, false);
                        mParams = null;
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(ApplicationData.TAG, "userinfo fetch Error, finish the job " + error.toString());
                        if (mNextJobTimer != null) {
                            mNextJobTimer.cancel();
                        }
                        if (mLocationTimer != null) {
                            mLocationTimer.cancel();
                        }
                        jobFinished(mParams, true);
                        mParams = null;
                    }
                });
                int MY_SOCKET_TIMEOUT_MS = 10000;
                request.setRetryPolicy(new DefaultRetryPolicy(
                        MY_SOCKET_TIMEOUT_MS,
                        1,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                ApplicationData.requestQueue.add(request);

            }
        };
        mNextJobTimer.start();
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(ApplicationData.TAG, "Start LocationJob " + params.getJobId());
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercentage = 100 * level / (float) scale;

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (batteryPercentage < 10 || mParams != null) {
            JSONObject activity = new JSONObject();
            try {
                activity.put("type", "SkipLocationJob");
                activity.put("batteryPercentage", batteryPercentage);
                activity.put("gpsEnabled", gpsEnabled);
                activity.put("jobId", params.getJobId());
                activity.put("networkEnabled", networkEnabled);
                activity.put("jobAlreadyRunning", (mParams != null));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ApplicationData.addActivity(activity);
            ApplicationData.synchronizeActivities(this);
            return false;
        }

        mParams = params;
        numNetworkErrors = 0;
        startLocationRequests();
        return true;
    };

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(ApplicationData.TAG, "onStopJob");
        mParams = null;
        list.clear();
        if (mLocationTimer != null) {
            mLocationTimer.cancel();
        }
        if (mNextJobTimer != null) {
            mNextJobTimer.cancel();
        }
        return false;
    }
}


