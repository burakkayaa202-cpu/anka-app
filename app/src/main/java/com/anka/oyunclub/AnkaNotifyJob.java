Package com.anka.oyunclub;

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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AnkaNotifyJob extends JobService {
    private static final String DB = "https://anka-apk-default-rtdb.firebaseio.com";
    private static final String CHANNEL = "anka_bildirim";

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(() -> {
            try { 
                check(); 
            } catch (Throwable ignored) { 
                // Thread dışına çıkabilecek tüm hatalar (Error ve Exception) yakalandı[span_2](start_span)[span_2](end_span)
            }
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private JSONObject get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        if (c.getResponseCode() != 200) return null;
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        String s = out.toString("UTF-8").trim();
        if (s.isEmpty() || s.equals("null")) return null;
        return new JSONObject(s);
    }

    private void check() throws Exception {
        SharedPreferences sp = getSharedPreferences("anka", MODE_PRIVATE);
        String user = sp.getString("user", "");
        if (user.isEmpty()) return;

        String base = DB + "/notifications/" + Uri.encode(user) + ".json?orderBy=%22%24key%22";
        String prefKey = "last_" + user;
        String last = sp.getString(prefKey, "");

        // İlk çalışma: eski bildirimleri gösterme, sadece en yeni anahtarı kaydet
        if (last.isEmpty()) {
            JSONObject first = get(base + "&limitToLast=1");
            String newest = "-";
            if (first != null) {
                Iterator<String> it = first.keys();
                if (it.hasNext()) newest = it.next();
            }
            sp.edit().putString(prefKey, newest).apply();
            return;
        }

        JSONObject res = get(base + "&startAt=%22" + Uri.encode(last) + "%22");
        if (res == null) return;

        List<String> keys = new ArrayList<>();
        Iterator<String> it = res.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (k.compareTo(last) > 0) keys.add(k);
        }
        if (keys.isEmpty()) return;
        Collections.sort(keys);
        sp.edit().putString(prefKey, keys.get(keys.size() - 1)).apply();

        // Uygulama açıkken site zaten kendi bildirimini gösteriyor
        if (MainActivity.foreground) return;

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
            String title;
            String body;
            if (type.equals("msg")) {
                title = "💬 " + from;
                body = text;
            } else if (type.equals("friend")) {
                title = "👥 Arkadaşlık isteği";
                body = from + " sana arkadaşlık isteği gönderdi";
            } else if (type.equals("accepted")) {
                title = "✅ Arkadaş oldunuz";
                body = from + " isteğini kabul etti";
            } else if (type.equals("reply")) {
                title = "↩️ " + from;
                body = "Yorumunu yanıtladı: " + text;
            } else {
                title = "🔔 Anka";
                body = text;
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
                NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Anka Bildirimleri", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, CHANNEL)
                : new Notification.Builder(ctx);
            b.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true);
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH);
            nm.notify(id, b.build());
        } catch (Throwable ignored) {
            // Bildirim gösterirken oluşabilecek simge/sistem hatalarını yut ve çökmesini engelle[span_3](start_span)[span_3](end_span)
        }
    }
}
