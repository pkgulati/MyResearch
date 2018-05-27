package in.kpraveen.myresearch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getSharedPreferences("Application", Context.MODE_PRIVATE);
        boolean isLoggedIn;

        isLoggedIn = sharedPref.getBoolean("loggedIn", false);
        if (isLoggedIn) {
            String accessToken = sharedPref.getString("accessToken", "");
            ApplicationData.accessToken = accessToken;
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }
}
