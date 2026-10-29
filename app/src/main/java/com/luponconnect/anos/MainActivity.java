package com.luponconnect.anos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    static final String SITE = "https://luponconnect.brgy-anos.workers.dev";
    static final String SITE_HOST = "luponconnect.brgy-anos.workers.dev";

    private WebView web;
    private ProgressBar bar;
    private SwipeRefreshLayout swipe;
    private long pausedAt = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper.createChannel(this);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        web = findViewById(R.id.web);
        bar = findViewById(R.id.bar);
        swipe = findViewById(R.id.swipe);

        // Pull down to refresh (only when the page is at the very top)
        swipe.setColorSchemeResources(R.color.gold, R.color.green);
        swipe.setOnRefreshListener(() -> {
            String u = web.getUrl();
            if (u == null || u.startsWith("file:")) web.loadUrl(SITE);
            else web.reload();
        });
        swipe.setOnChildScrollUpCallback((parent, child) -> web.getScrollY() > 0);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " LuponConnectApp/" + BuildConfig.VERSION_NAME);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternal(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                bar.setVisibility(View.GONE);
                swipe.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                swipe.setRefreshing(false);
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/offline.html");
                }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                bar.setProgress(newProgress);
            }
        });

        web.loadUrl(targetUrl(getIntent()));
    }

    // Links to other sites (Facebook, email, phone, maps...) open in the phone's own apps
    private boolean handleExternal(Uri u) {
        String scheme = u.getScheme() == null ? "" : u.getScheme();
        if (scheme.equals("file")) return false;
        if (scheme.startsWith("http") && SITE_HOST.equals(u.getHost())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (ActivityNotFoundException ignored) {
            // nothing can open it
        }
        return true;
    }

    private String targetUrl(Intent i) {
        String route = i == null ? null : i.getStringExtra("route");
        return SITE + (route == null ? "" : route);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        web.loadUrl(targetUrl(intent));
    }

    // Coming back to the app after a few minutes: load the page again so new news/quiz show up
    @Override
    protected void onPause() {
        super.onPause();
        pausedAt = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null && pausedAt > 0 && System.currentTimeMillis() - pausedAt > 5 * 60 * 1000L) {
            String u = web.getUrl();
            if (u != null && u.startsWith("file:")) web.loadUrl(SITE);
            else if (u != null) web.reload();
        }
        pausedAt = 0;
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
