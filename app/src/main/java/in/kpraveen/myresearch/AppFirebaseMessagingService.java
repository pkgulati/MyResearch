package in.kpraveen.myresearch;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private boolean channelCreated = false;

    public AppFirebaseMessagingService() {
        // Since android Oreo notification channel is needed.

    }

    final Handler mHandler = new Handler();

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AppFirebaseMessagingService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean checkPermission() {
        int permission = PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PermissionChecker.PERMISSION_GRANTED) {
            permission = PermissionChecker.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return (permission == PermissionChecker.PERMISSION_GRANTED ? true : false);
    }

    public float distance(double lat_a, double lng_a, double lat_b, double lng_b) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b - lat_a);
        double lngDiff = Math.toRadians(lng_b - lng_a);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return new Float(distance * meterConversion).floatValue();
    }

    ;

    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            //Log.d(TeamApplication.TAG, "Message data payload: " + remoteMessage.getData());

            JSONObject req = new JSONObject(remoteMessage.getData());

            try {
                String type = req.getString("type");
                if (type.equals("InformationUpdateRequest")) {
                    Log.d(ApplicationData.TAG, "InformationUpdateRequest " + remoteMessage.getMessageId());
                    String activityId = req.getString("activityId");

                }
                else if (type.equals("configure")) {
                    UserConfiguration.fetch(this);
                } else if (type.equals("notification")) {
                    Log.d(ApplicationData.TAG, "notification");
                } else if (type.equals("setMotion")) {
                } else if (type.equals("cancelJobs")) {
                    JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
                    if (jobScheduler != null) {
                        jobScheduler.cancelAll();
                    }
                } else if (type.equals("killApp")) {
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                    System.exit(0);
                }
                else {
                    Log.d(ApplicationData.TAG, "Unknonw message type " + type);
                    //toast("Unknonw message type " + type);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

    }

    private void sendNotification(String title, String messageBody) {

        String channelId = "MyResearchChannel";
        if (!channelCreated) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(channelId,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            channelCreated = true;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        // replaces current notification
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(notificationPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        notificationManager.notify(25, notification);

    }

}
