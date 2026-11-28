package com.anka.oyunclub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnkaNotifyJob extends JobService {
    static final boolean DEBUG = false;

    private static final String DB = "https://anka-apk-default-rtdb.firebaseio.com";
    private static final String CHANNEL = "anka_bildirim";
    private static final String REPO = "burakkayaa202-cpu/anka-app";
    private static final String FEED =
        "https://ankaoyunclub.blogspot.com/feeds/posts/default?alt=json&max-results=1";
    private static final AtomicBoolean running = new AtomicBoolean(false);

    private int lastCode = 0;

    @Override
    public boolean onStartJob(final JobParameters params) {
        AnkaAlarmReceiver.schedule(this); // bir sonraki alarmı kur
        if (!running.compareAndSet(false, true)) return false;
        new Thread(() -> {
            try {
                try { check(); } catch (Throwable ignored) { }
                try { checkPost(); } catch (Throwable ignored) { }
                try { checkUpdateNotification(); } catch (Throwable ignored) { }
            } finally {
                running.set(false);
                try { jobFinished(params, false); } catch (Throwable ignored) { }
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) { return true; }

    private String appVersion() {
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return v == null ? "0" : v;
        } catch (Throwable ignored) { }
        return "0";
    }

    private String read(String url, String accept) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "AnkaApp");
        if (accept != null) c.setRequestProperty("Accept", accept);
        lastCode = c.getResponseCode();
        if (lastCode != 200) return null;
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8").trim();
    }

    private JSONObject get(String url) {
        lastCode = 0;
        try {
            String s = read(url, null);
            if (s == null || s.isEmpty() || s.equals("null")) return null;
            return new JSONObject(s);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------- YENİ YAYIN ----------
    private void checkPost() throws Exception {
        String s = read(FEED, null);
        if (s == null) return;
        JSONArray entries = new JSONObject(s).getJSONObject("feed").optJSONArray("entry");
        if (entries == null || entries.length() == 0) return;
        JSONObject e = entries.getJSONObject(0);
        String id = e.getJSONObject("id").getString("$t");
        String title = e.getJSONObject("title").optString("$t", "Yeni yayın");

        SharedPreferences sp = getSharedPreferences("anka", MODE_PRIVATE);
        String last = sp.getString("last_post", "");
        if (last.isEmpty()) { // ilk çalışma: bildirim atma, sadece kaydet
            sp.edit().putString("last_post", id).apply();
            return;
        }
        if (!last.equals(id)) {
            sp.edit().putString("last_post", id).apply();
            if (!MainActivity.foreground) {
                show(this, 5555, "📰 Yeni yayın", title);
            }
        }
    }

    // ---------- YENİ SÜRÜM ----------
    private void checkUpdateNotification() throws Exception {
        String s = read("https://api.github.com/repos/" + REPO + "/releases/latest",
            "application/vnd.github+json");
        if (s == null) return;
        String tag = new JSONObject(s).optString("tag_name", "");
        if (tag.isEmpty() || !isNewer(tag, appVersion())) return;
        SharedPreferences sp = getSharedPreferences("anka", MODE_PRIVATE);
        if (!sp.getString("last_notified_version", "").equals(tag)) {
            sp.edit().putString("last_notified_version", tag).apply();
            show(this, 9999, "🚀 Yeni Sürüm Çıktı: " + tag,
                "Anka APK'nın yeni sürümü hazır, hemen indir!");
        }
    }

    private boolean isNewer(String remote, String local) {
        try {
            String[] a = remote.replaceAll("[^0-9.]", "").split("\\.");
            String[] b = local.replaceAll("[^0-9.]", "").split("\\.");
            int len = Math.max(a.length, b.length);
            for (int i = 0; i < len; i++) {
                int x = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
                int y = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
                if (x != y) return x > y;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ---------- MESAJ / YORUM YANITI / ARKADAŞLIK ----------
    private void check() {
        SharedPreferences sp = getSharedPreferences("anka", MODE_PRIVATE);
        String user = sp.getString("user", "");
        if (user.isEmpty()) return;

        String base = DB + "/notifications/" + Uri.encode(user) + ".json?orderBy=%22%24key%22";
        String prefKey = "last_" + user;
        String last = sp.getString(prefKey, "");

        if (last.isEmpty()) {
            JSONObject first = get(base + "&limitToLast=1");
            if (lastCode != 200) return;
            String newest = "-";
            if (first != null) {
                Iterator<String> it = first.keys();
                if (it.hasNext()) newest = it.next();
            }
            sp.edit().putString(prefKey, newest).apply();
            return;
        }

        JSONObject res = get(base + "&startAt=%22" + Uri.encode(last) + "%22");
        if (lastCode != 200 || res == null) return;

        List<String> keys = new ArrayList<>();
        Iterator<String> it = res.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (k != null && k.compareTo(last) > 0) keys.add(k);
        }
        if (keys.isEmpty()) return;
        Collections.sort(keys);
        sp.edit().putString(prefKey, keys.get(keys.size() - 1)).apply();

        if (MainActivity.foreground) return; // uygulama açıkken site kendisi gösteriyor

        if (keys.size() > 5) {
            show(this, 1, "🔔 Anka", keys.size() + " yeni bildirimin var");
            return;
        }
        for (String k : keys) {
            JSONObject o = res.optJSONObject(k);
            if (o == null) continue;
            String type = o.optString("type", "");
            String from = o.optString("from", "Biri");
            String text = o.optString("text", "");
            String title, body;
            if (type.equals("msg")) {
                title = "💬 " + from; body = text;
            } else if (type.equals("friend")) {
                title = "👥 Arkadaşlık isteği"; body = from + " sana arkadaşlık isteği gönderdi";
            } else if (type.equals("accepted")) {
                title = "✅ Arkadaş oldunuz"; body = from + " isteğini kabul etti";
            } else if (type.equals("reply")) {
                title = "↩️ " + from; body = "Yorumunu yanıtladı: " + text;
            } else {
                title = "🔔 Anka"; body = text;
            }
            show(this, k.hashCode(), title, body);
        }
    }

    static void show(Context ctx, int id, String title, String text) {
        try {
            NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Anka Bildirimleri", NotificationManager.IMPORTANCE_HIGH));
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, CHANNEL) : new Notification.Builder(ctx);
            b.setSmallIcon(R.drawable.ic_stat_anka)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true);
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH);
            nm.notify(id, b.build());
        } catch (Throwable ignored) { }
    }
}
