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

/**
 * Runs in the background about every 15 minutes and looks for:
 * a newer news post, a newer Ating Alamin article, a new public quiz,
 * and the Mga Kampeon ng Buwan (top 3 of last month).
 */
public class CheckWorker extends Worker {

    private static final String[] MONTHS = {"Enero", "Pebrero", "Marso", "Abril", "Mayo", "Hunyo",
            "Hulyo", "Agosto", "Setyembre", "Oktubre", "Nobyembre", "Disyembre"};

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
            // 1) Newest published news post
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

            // 2) Newest published Ating Alamin article
            String alSlug = null, alTitle = null, alTs = null;
            String r2 = http("GET", base + "/rest/v1/ating_alamin?select=slug,title,created_at&published=eq.true&order=created_at.desc&limit=1", key, null);
            if (r2 != null) {
                JSONArray a = new JSONArray(r2);
                if (a.length() > 0) {
                    JSONObject o = a.getJSONObject(0);
                    alSlug = o.optString("slug");
                    alTitle = o.optString("title");
                    alTs = o.optString("created_at");
                }
            }

            // 3) Current public quiz + last month's champions
            String quizKey = null, quizTitle = null, champKey = null, champText = null;
            String q = http("POST", base + "/rest/v1/rpc/quiz_board", key, "{}");
            if (q != null) {
                JSONObject o = new JSONObject(q);
                JSONObject qz = o.optJSONObject("quiz");
                if (qz != null) {
                    quizTitle = qz.optString("title");
                    quizKey = qz.optString("start") + "|" + quizTitle;
                }
                JSONArray last = o.optJSONArray("last");
                if (last != null && last.length() > 0) {
                    champKey = o.optString("last_month");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < last.length() && i < 3; i++) {
                        JSONObject c = last.getJSONObject(i);
                        if (i > 0) sb.append(", ");
                        sb.append(i + 1).append(". ").append(c.optString("nickname"));
                        String pl = c.optString("place");
                        if (!pl.isEmpty()) sb.append(" (").append(pl).append(")");
                    }
                    champText = sb.toString();
                }
            }

            boolean init = sp.getBoolean("init", false);     // news + quiz baseline
            boolean init2 = sp.getBoolean("init2", false);   // Ating Alamin + champions baseline
            String savedPost = sp.getString("post_ts", "");
            String savedAl = sp.getString("alamin_ts", "");
            String savedQuiz = sp.getString("quiz", "");
            String savedChamp = sp.getString("champ", "");

            if (init) {
                if (postId != null && postTs != null && postTs.compareTo(savedPost) > 0) {
                    NotificationHelper.show(ctx, 1, "📢 Bagong Balita", postTitle, "#/news/" + postId);
                }
                if (quizKey != null && !quizKey.equals(savedQuiz)) {
                    NotificationHelper.show(ctx, 2, "🎯 Bagong Quiz!", quizTitle + " — maglaro na!", "#/quiz");
                }
            }
            if (init2) {
                if (alSlug != null && alTs != null && alTs.compareTo(savedAl) > 0) {
                    NotificationHelper.show(ctx, 3, "📚 Bagong Ating Alamin", alTitle, "#/alamin/" + alSlug);
                }
                if (champKey != null && !champKey.equals(savedChamp)) {
                    NotificationHelper.show(ctx, 4, "🏆 Mga Kampeon ng " + monthName(champKey), champText, "#/");
                }
            }

            SharedPreferences.Editor ed = sp.edit();
            if (postTs != null && postTs.compareTo(savedPost) > 0) ed.putString("post_ts", postTs);
            if (alTs != null && alTs.compareTo(savedAl) > 0) ed.putString("alamin_ts", alTs);
            if (quizKey != null) ed.putString("quiz", quizKey);
            if (champKey != null) ed.putString("champ", champKey);
            ed.putBoolean("init", true).putBoolean("init2", true).apply();
        } catch (Exception e) {
            // No internet or unexpected reply: try again at the next scheduled run
        }
        return Result.success();
    }

    private static String monthName(String date) {
        try {
            int m = Integer.parseInt(date.substring(5, 7));
            return MONTHS[m - 1] + " " + date.substring(0, 4);
        } catch (Exception e) {
            return date;
        }
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
