package com.luponconnect.anos;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Runs in the background about every 15 minutes: looks for a newer news post or a new quiz. */
public class CheckWorker extends Worker {

    public CheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        String base = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_ANON_KEY;
        if (base == null || base.isEmpty() || key == null || key.isEmpty()) return Result.success();

        SharedPreferences sp = ctx.getSharedPreferences("lc_notify", Context.MODE_PRIVATE);
        try {
            // Newest published news post
            String postId = null, postTitle = null, postTs = null;
            String r = http("GET", base + "/rest/v1/posts?select=id,title,created_at&published=eq.true&order=created_at.desc&limit=1", key, null);
            if (r != null) {
                JSONArray a = new JSONArray(r);
                if (a.length() > 0) {
                    JSONObject o = a.getJSONObject(0);
                    postId = o.optString("id");
                    postTitle = o.optString("title");
                    postTs = o.optString("created_at");
                }
            }
            // Current public quiz
            String quizKey = null, quizTitle = null;
            String q = http("POST", base + "/rest/v1/rpc/quiz_board", key, "{}");
            if (q != null) {
                JSONObject o = new JSONObject(q);
                JSONObject qz = o.optJSONObject("quiz");
                if (qz != null) {
                    quizTitle = qz.optString("title");
                    quizKey = qz.optString("start") + "|" + quizTitle;
                }
            }

            boolean init = sp.getBoolean("init", false);
            String savedTs = sp.getString("post_ts", "");
            String savedQuiz = sp.getString("quiz", "");
            if (init) {
                if (postId != null && postTs != null && postTs.compareTo(savedTs) > 0) {
                    NotificationHelper.show(ctx, 1, "📢 Bagong Balita", postTitle, "#/news/" + postId);
                }
                if (quizKey != null && !quizKey.equals(savedQuiz)) {
                    NotificationHelper.show(ctx, 2, "🎯 Bagong Quiz!", quizTitle + " — maglaro na!", "#/quiz");
                }
            }
            SharedPreferences.Editor ed = sp.edit();
            if (postTs != null && postTs.compareTo(savedTs) > 0) ed.putString("post_ts", postTs);
            if (quizKey != null) ed.putString("quiz", quizKey);
            ed.putBoolean("init", true).apply();
        } catch (Exception e) {
            // No internet or unexpected reply: try again at the next scheduled run
        }
        return Result.success();
    }

    private String http(String method, String url, String key, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("apikey", key);
        if (key.startsWith("eyJ")) c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("Accept", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        if (c.getResponseCode() != 200) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
