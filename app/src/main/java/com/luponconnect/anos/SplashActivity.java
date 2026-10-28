package com.luponconnect.anos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends AppCompatActivity {

    // How long the splash screen stays on (milliseconds)
    private static final long SPLASH_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Gentle fade-in of the logo and the texts
        int[] ids = {R.id.logo, R.id.name, R.id.tag, R.id.foot};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            v.setAlpha(0f);
            v.animate().alpha(1f).setStartDelay(150L * i).setDuration(700).start();
        }

        // Check for new news / quiz every 15 minutes in the background
        Constraints c = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(CheckWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(c).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("lc-check", ExistingPeriodicWorkPolicy.KEEP, req);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_MS);
    }
}
