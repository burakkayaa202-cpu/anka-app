package com.anka.oyunclub;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String SITE = "https://ankaoyunclub.blogspot.com";
    private static final String SITE_HOST = "ankaoyunclub.blogspot.com";
    private static final int PICK = 1;
    private static final int PERM_STORAGE = 2;
    private static final int PERM_READ_MEDIA = 4;

    // ankaoyunclub.blogspot.com ve ülke uzantılı hali (.com.tr vb.) uygulama içinde açılsın
    private boolean isSiteHost(String host) {
        return host != null && (host.equals(SITE_HOST)
            || host.startsWith("ankaoyunclub.blogspot."));
    }

    private FrameLayout root;
    private WebView web;
    private FrameLayout splash;
    private ValueCallback<Uri[]> filePathCallback;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private long lastBack = 0;
    private String fxJs = null;

    private LinearLayout loader;
    private View ring;
    private ImageView loaderLogo;
    private ObjectAnimator ringAnim;
    private ObjectAnimator pulseAnim;
    private boolean pageLoading = false;
    private final Runnable showLoaderRun = () -> { if (pageLoading) showLoader(); };

    private final Map<Long, String[]> downloads = new HashMap<>();
    private String[] pendingDl = null;

    private static final String APP_VERSION = "1.3.2";
    private static final String REPO = "burakkayaa202-cpu/anka-app";
    private long updateId = -1;
    private Uri pendingInstall = null;
    private boolean updateChecked = false;

    static volatile boolean foreground = false;
    private boolean notifAsked = false;
    private int dbgUserCalls = 0;
    private boolean receiverRegistered = false; // Alıcı çakışmasını önlemek için

    private interface Done { void run(boolean ok, String text); }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density);
    }

    private boolean dead() {
        return isFinishing() || (Build.VERSION.SDK_INT >= 17 && isDestroyed());
    }

    private void setLogo(ImageView iv) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, o);
            if (o.outWidth > 0 && o.outHeight > 0) {
                int sample = 1;
                int max = Math.max(o.outWidth, o.outHeight);
                while (max / sample > 256) sample *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = sample;
                o2.inScaled = false;
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher, o2);
                if (bm != null) {
                    iv.setImageBitmap(bm);
                    return;
                }
            }
        } catch (Throwable ignored) { }
        try { iv.setImageResource(R.drawable.ic_launcher); } catch (Throwable ignored) { }
    }

    private void showCrashScreen(String report) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.parseColor("#0B1120"));
        box.setPadding(dp(16), dp(36), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("ÇÖKME RAPORU\n(bu ekranın görüntüsünü gönder)");
        title.setTextColor(Color.parseColor("#00E5FF"));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setPadding(0, 0, 0, dp(12));
        box.addView(title);

        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(report.substring(0, Math.min(report.length(), 2500)));
        tv.setTextColor(Color.parseColor("#E6FAFF"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        sv.addView(tv);
        box.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView btn = ankaBtn("UYGULAMAYI AÇ", true);
        btn.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(MainActivity.this, MainActivity.class));
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.topMargin = dp(12);
        box.addView(btn, bl);

        setContentView(box);
    }

    private String loadFx() {
        if (fxJs != null) return fxJs;
        try {
            InputStream in = getAssets().open("anka.js");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            fxJs = out.toString("UTF-8");
        } catch (Exception e) {
            fxJs = "";
        }
        return fxJs;
    }

    private void hideSplash() {
        if (splash != null && splash.getVisibility() == View.VISIBLE) {
            splash.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> splash.setVisibility(View.GONE));
        }
    }

    private void buildLoader() {
        loader = new LinearLayout(this);
        loader.setOrientation(LinearLayout.VERTICAL);
        loader.setGravity(Gravity.CENTER);
        loader.setBackgroundColor(Color.parseColor("#990B1120"));
        loader.setVisibility(View.GONE);

        FrameLayout stack = new FrameLayout(this);

        ring = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RING);
        if (Build.VERSION.SDK_INT >= 29) g.setThickness(dp(4));
        if (Build.VERSION.SDK_INT >= 29) g.setInnerRadius(dp(46));
        g.setUseLevel(false);
        g.setGradientType(GradientDrawable.SWEEP_GRADIENT);
        g.setColors(new int[]{Color.parseColor("#0000E5FF"), Color.parseColor("#FF00E5FF")});
        ring.setBackground(g);
        stack.addView(ring, new FrameLayout.LayoutParams(dp(100), dp(100)));

        loaderLogo = new ImageView(this);
        setLogo(loaderLogo);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER);
        stack.addView(loaderLogo, lp);

        loader.addView(stack, new LinearLayout.LayoutParams(dp(100), dp(100)));

        TextView label = new TextView(this);
        label.setText("ANKA YÜKLENİYOR");
        label.setTextColor(Color.parseColor("#00E5FF"));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setLetterSpacing(0.2f);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = dp(16);
        loader.addView(label, tl);

        ringAnim = ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f);
        ringAnim.setDuration(1000);
        ringAnim.setRepeatCount(ValueAnimator.INFINITE);
        ringAnim.setInterpolator(new LinearInterpolator());

        pulseAnim = ObjectAnimator.ofPropertyValuesHolder(loaderLogo,
            PropertyValuesHolder.ofFloat("scaleX", 0.85f, 1.1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.85f, 1.1f),
            PropertyValuesHolder.ofFloat("alpha", 0.6f, 1f));
        pulseAnim.setDuration(700);
        pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
    }

    private void scheduleLoader() {
        pageLoading = true;
        if (web != null) {
            web.removeCallbacks(showLoaderRun);
            web.postDelayed(showLoaderRun, 500);
        }
    }

    private void showLoader() {
        if (dead() || loader == null || loader.getVisibility() == View.VISIBLE) return;
        loader.setAlpha(0f);
        loader.setVisibility(View.VISIBLE);
        loader.animate().alpha(1f).setDuration(200).start();
        if (ringAnim != null) ringAnim.start();
        if (pulseAnim != null) pulseAnim.start();
    }

    private void hideLoader() {
        pageLoading = false;
        if (web != null) web.removeCallbacks(showLoaderRun);
        if (loader != null && loader.getVisibility() == View.VISIBLE) {
            loader.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                if (loader != null) loader.setVisibility(View.GONE);
                if (ringAnim != null) ringAnim.cancel();
                if (pulseAnim != null) pulseAnim.cancel();
            }).start();
        }
    }

    private GradientDrawable shape(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(2), stroke);
        return g;
    }

    private TextView ankaBtn(String text, boolean primary) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setPadding(dp(8), dp(16), dp(8), dp(16));
        if (primary) {
            GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#FF4D4D"), Color.parseColor("#CC0000")});
            g.setCornerRadius(dp(14));
            t.setBackground(g);
            t.setTextColor(Color.WHITE);
        } else {
            t.setBackground(shape(Color.parseColor("#0B1120"), Color.parseColor("#00E5FF"), 14));
            t.setTextColor(Color.parseColor("#00E5FF"));
        }
        return t;
    }

    @SuppressWarnings("deprecation")
    private String cleanText(String s) {
        if (s == null) return "";
        if (s.contains("<") && s.contains(">")) {
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    s = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString();
                } else {
                    s = Html.fromHtml(s).toString();
                }
            } catch (Throwable ignored) { }
        }
        return s.trim();
    }

    private void ankaDialog(String title, String msg, String input,
                            String okText, String cancelText, Done done) {
        if (dead()) {
            done.run(false, null);
            return;
        }
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(false);
        final boolean[] finished = {false};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(22), dp(24), dp(24));
        box.setBackground(shape(Color.parseColor("#0B1120"), Color.parseColor("#00E5FF"), 24));

        TextView tt = new TextView(this);
        tt.setText(title);
        tt.setGravity(Gravity.CENTER);
        tt.setTextColor(Color.parseColor("#00E5FF"));
        tt.setTypeface(Typeface.DEFAULT_BOLD);
        tt.setLetterSpacing(0.12f);
        tt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        box.addView(tt);

        TextView mm = new TextView(this);
        mm.setText(msg == null ? "" : msg);
        mm.setGravity(Gravity.CENTER);
        mm.setTextColor(Color.parseColor("#E6FAFF"));
        mm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        mm.setPadding(0, dp(12), 0, dp(16));
        box.addView(mm);

        final EditText et;
        if (input != null) {
            et = new EditText(this);
            et.setText(input);
            et.setTextColor(Color.WHITE);
            et.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            et.setMaxLines(4);
            et.setPadding(dp(14), dp(12), dp(14), dp(12));
            et.setBackground(shape(Color.parseColor("#050810"), Color.parseColor("#00E5FF"), 14));
            et.setSelection(et.getText().length());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(16);
            box.addView(et, lp);
        } else {
            et = null;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView ok = ankaBtn(okText, true);
        row.addView(ok, new LinearLayout.LayoutParams(0, -2, 1f));
        if (cancelText != null) {
            TextView no = ankaBtn(cancelText, false);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1f);
            p2.leftMargin = dp(12);
            row.addView(no, p2);
            no.setOnClickListener(v -> {
                if (finished[0]) return;
                finished[0] = true;
                try { d.dismiss(); } catch (Exception ignored) { }
                done.run(false, null);
            });
        }
        box.addView(row);

        ok.setOnClickListener(v -> {
            if (finished[0]) return;
            finished[0] = true;
            try { d.dismiss(); } catch (Exception ignored) { }
            done.run(true, et != null ? et.getText().toString() : null);
        });

        try {
            d.setContentView(box);
            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                d.getWindow().setDimAmount(0.75f);
                d.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                d.show();
                int w = Math.min((int) (getResources().getDisplayMetrics().widthPixels * 0.9f), dp(560));
                d.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable e) {
            if (!finished[0]) {
                finished[0] = true;
                done.run(false, null);
            }
        }
    }

    private void askDownload(String url, String ua, String cd, String mime) {
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            handleBlob(url, mime);
            return;
        }
        final String name = URLUtil.guessFileName(url, cd, mime);
        ankaDialog("ANKA İNDİRME", "📥 " + name + "\nindirilsin mi?", null, "İNDİR", "İPTAL",
            (ok, t) -> { if (ok) startDownload(url, ua, cd, mime, name); });
    }

    private void startDownload(String url, String ua, String cd, String mime, String name) {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
               != PackageManager.PERMISSION_GRANTED) {
            pendingDl = new String[]{url, ua, cd, mime, name};
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM_STORAGE);
            return;
        }
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            if (mime != null && !mime.isEmpty()) r.setMimeType(mime);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) r.addRequestHeader("cookie", cookie);
            if (ua != null) r.addRequestHeader("User-Agent", ua);
            r.addRequestHeader("Referer", SITE);
            r.setTitle(name);
            r.setDescription("Anka indiriyor...");
            r.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long id = dm.enqueue(r);
            downloads.put(id, new String[]{name, url});
            Toast.makeText(this, "İndirme başladı: " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Dosya indirilemedi, tarayıcıda açılıyor...", Toast.LENGTH_SHORT).show();
            openExternal(url);
        }
    }

    private final BroadcastReceiver dlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            try {
                long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == updateId) {
                    updateId = -1;
                    finishUpdate(id);
                    return;
                }
                String[] info = downloads.remove(id);
                if (info == null) return;
                final String name = info[0];
                final String dlUrl = info[1];
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id));
                boolean success = false;
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        int col = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        success = col >= 0 && cur.getInt(col) == DownloadManager.STATUS_SUCCESSFUL;
                    }
                    cur.close();
                }
                if (dead()) return;
                if (success) {
                    final Uri file = dm.getUriForDownloadedFile(id);
                    final String type = dm.getMimeTypeForDownloadedFile(id);
                    ankaDialog("ANKA İNDİRME", "✅ " + name + "\nindirildi!", null, "AÇ", "KAPAT",
                        (ok, t) -> {
                            if (!ok || file == null) return;
                            openFile(file, type);
                        });
                } else {
                    ankaDialog("ANKA İNDİRME", "❌ " + name + "\nindirilemedi!", null,
                        "TARAYICIDA AÇ", "KAPAT", (ok, t) -> { if (ok) openExternal(dlUrl); });
                }
            } catch (Throwable ignored) { }
        }
    };

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 3 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            debugRun();
        }
        if (code == PERM_STORAGE && pendingDl != null) {
            String[] p = pendingDl;
            pendingDl = null;
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload(p[0], p[1], p[2], p[3], p[4]);
            } else {
                Toast.makeText(this, "Depolama izni verilmedi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleBlob(String url, String mime) {
        if (url.startsWith("data:")) {
            try {
                int comma = url.indexOf(',');
                String meta = url.substring(5, comma);
                String data = url.substring(comma + 1);
                String type = meta.contains(";") ? meta.substring(0, meta.indexOf(';')) : meta;
                byte[] bytes = meta.contains(";base64")
                    ? Base64.decode(data, Base64.DEFAULT)
                    : Uri.decode(data).getBytes("UTF-8");
                askSave(bytes, type.isEmpty() ? "application/octet-stream" : type, "");
            } catch (Exception e) {
                Toast.makeText(this, "Dosya indirilemedi!", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        String js = "(function(){try{var n=window.__ankaLastDl||'';var x=new XMLHttpRequest();"
            + "x.open('GET','" + url.replace("'", "%27") + "',true);x.responseType='blob';"
            + "x.onload=function(){var r=new FileReader();r.onloadend=function(){var d=String(r.result);"
            + "AnkaBridge.saveBase64(d.substring(d.indexOf(',')+1),x.response.type||'application/octet-stream',n);};"
            + "r.readAsDataURL(x.response);};x.onerror=function(){AnkaBridge.fail();};x.send();}"
            + "catch(e){AnkaBridge.fail();}})();";
        if (web != null) web.evaluateJavascript(js, null);
    }

    private class AnkaBridge {
        @JavascriptInterface
        public void setUser(String user) {
            final String u = user == null ? "" : user.trim();
            if (AnkaNotifyJob.DEBUG && dbgUserCalls < 3) {
                final boolean first = (dbgUserCalls == 0);
                dbgUserCalls++;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "SETUSER: [" + u + "]", Toast.LENGTH_LONG).show();
                    if (first && !u.isEmpty() && (Build.VERSION.SDK_INT < 33
                        || checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                           == PackageManager.PERMISSION_GRANTED)) {
                        debugRun();
                    }
                });
            }
            SharedPreferences sp = getSharedPreferences("anka", MODE_PRIVATE);
            if (!u.equals(sp.getString("user", ""))) {
                sp.edit().putString("user", u).apply();
            }
            if (!u.isEmpty()) runOnUiThread(() -> askNotifPermission());
        }

        @JavascriptInterface
        public void saveBase64(String b64, String mime, String name) {
            try {
                final byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                final String m = (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime;
                final String n = name;
                runOnUiThread(() -> askSave(bytes, m, n));
            } catch (Exception e) {
                fail();
            }
        }

        @JavascriptInterface
        public void fail() {
            runOnUiThread(() ->
                Toast.makeText(MainActivity.this, "Dosya indirilemedi!", Toast.LENGTH_SHORT).show());
        }
    }

    private void askSave(final byte[] bytes, final String mime, String name) {
        String clean = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (clean.isEmpty()) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            clean = "anka_" + System.currentTimeMillis() + (ext != null ? "." + ext : "");
        }
        final String fn = clean;
        ankaDialog("ANKA İNDİRME", "📥 " + fn + "\nindirilsin mi?", null, "İNDİR", "İPTAL",
            (ok, t) -> { if (ok) saveBytes(bytes, mime, fn); });
    }

    private void saveBytes(byte[] bytes, final String mime, String fn) {
        try {
            Uri out = null;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, fn);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                out = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (out == null) throw new Exception("uri yok");
                OutputStream os = getContentResolver().openOutputStream(out);
                if (os == null) throw new Exception("akis yok");
                os.write(bytes);
                os.close();
            } else {
                if (Build.VERSION.SDK_INT >= 23
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                       != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 99);
                    Toast.makeText(this, "Depolama iznini ver, sonra tekrar dene", Toast.LENGTH_LONG).show();
                    return;
                }
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                dir.mkdirs();
                FileOutputStream os = new FileOutputStream(new File(dir, fn));
                os.write(bytes);
                os.close();
            }
            final Uri file = out;
            ankaDialog("ANKA İNDİRME",
                "✅ " + fn + "\nindirildi!" + (file == null ? "\n(İndirilenler klasörüne bak)" : ""),
                null, file != null ? "AÇ" : "TAMAM", file != null ? "KAPAT" : null,
                (ok, t) -> { if (ok && file != null) openFile(file, mime); });
        } catch (Exception e) {
            Toast.makeText(this, "Dosya kaydedilemedi!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(Uri file, String type) {
        try {
            Intent v = new Intent(Intent.ACTION_VIEW);
            v.setDataAndType(file, type);
            v.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(v);
        } catch (Exception e) {
            Toast.makeText(this, "Bu dosyayı açacak uygulama yok. İndirilenler klasörüne bak.",
                Toast.LENGTH_LONG).show();
        }
    }

    private void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUpdate() {
        if (updateChecked) return;
        updateChecked = true;
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)
                    new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                if (c.getResponseCode() != 200) return;
                InputStream in = c.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close();
                JSONObject j = new JSONObject(out.toString("UTF-8"));
                final String tag = j.optString("tag_name", "");
                final String notes = j.optString("body", "");
                String apk = null;
                JSONArray assets = j.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        if (a.optString("name", "").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            apk = a.optString("browser_download_url", null);
                            break;
                        }
                    }
                }
                if (apk == null || !isNewer(tag, APP_VERSION)) return;
                final String fApk = apk;
                runOnUiThread(() -> showUpdate(tag, notes, fApk));
            } catch (Throwable ignored) { }
        }).start();
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

    private void showUpdate(String tag, String notes, final String apkUrl) {
        if (dead()) return;
        String n = notes == null ? "" : notes.trim();
        if (n.length() > 300) n = n.substring(0, 300) + "...";
        String msg = "🚀 Yeni sürüm çıktı: " + tag + (n.isEmpty() ? "" : "\n\n" + n);
        try {
            ankaDialog("ANKA GÜNCELLEME", msg, null, "GÜNCELLE", "SONRA",
                (ok, t) -> { if (ok) startUpdateDownload(apkUrl); });
        } catch (Exception ignored) { }
    }

    private void startUpdateDownload(String apkUrl) {
        try {
            File old = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "anka-update.apk");
            if (old.exists()) old.delete();
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(apkUrl));
            r.setTitle("Anka güncellemesi");
            r.setDescription("Yeni sürüm indiriliyor...");
            r.setMimeType("application/vnd.android.package-archive");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "anka-update.apk");
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            updateId = dm.enqueue(r);
            Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Güncelleme indirilemedi!", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishUpdate(long id) {
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id));
        boolean success = false;
        if (cur != null) {
            if (cur.moveToFirst()) {
                int col = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
                success = col >= 0 && cur.getInt(col) == DownloadManager.STATUS_SUCCESSFUL;
            }
            cur.close();
        }
        Uri u = success ? dm.getUriForDownloadedFile(id) : null;
        if (u == null) {
            Toast.makeText(this, "Güncelleme indirilemedi!", Toast.LENGTH_LONG).show();
            return;
        }
        installApk(u);
    }

    private void installApk(final Uri apk) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingInstall = apk;
            ankaDialog("İZİN GEREKLİ",
                "Güncellemeyi kurmak için Anka'ya \"bu kaynaktan yüklemeye izin ver\" iznini açman lazım.",
                null, "AYARLARA GİT", "İPTAL",
                (ok, t) -> {
                    if (!ok) { pendingInstall = null; return; }
                    try {
                        Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()));
                        startActivity(s);
                    } catch (Exception e) {
                        Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
                    }
                });
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(apk, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Güncelleme kurulamadı", Toast.LENGTH_LONG).show();
        }
    }

    private void askNotifPermission() {
        if (notifAsked) return;
        notifAsked = true;
        if (Build.VERSION.SDK_INT >= 33
            && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
               != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 3);
        }
    }

    // TEST: hemen bir deneme bildirimi at + bildirim kontrol işini 1 sn sonra çalıştır
    private void debugRun() {
        if (!AnkaNotifyJob.DEBUG) return;
        try {
            AnkaNotifyJob.show(this, 7778, "ANKA TEST", "Bildirimler çalışıyor");
        } catch (Throwable ignored) { }
        try {
            JobScheduler js = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            js.schedule(new JobInfo.Builder(1002, new ComponentName(this, AnkaNotifyJob.class))
                .setMinimumLatency(1000)
                .setOverrideDeadline(5000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
        } catch (Throwable ignored) { }
    }

    private void scheduleNotifyJob() {
        try {
            JobScheduler js = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            // TEST: uygulama açılınca ~8 sn sonra bir kez hemen çalıştır (AnkaNotifyJob.DEBUG açıkken)
            if (AnkaNotifyJob.DEBUG) {
                try {
                    js.schedule(new JobInfo.Builder(1002, new ComponentName(this, AnkaNotifyJob.class))
                        .setMinimumLatency(8000)
                        .setOverrideDeadline(20000)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .build());
                } catch (Throwable ignored) { }
            }
            if (Build.VERSION.SDK_INT >= 24 && js.getPendingJob(1001) != null) return;
            JobInfo job = new JobInfo.Builder(1001, new ComponentName(this, AnkaNotifyJob.class))
                .setPeriodic(15 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build();
            js.schedule(job);
        } catch (Throwable ignored) { }
    }

    private void showOffline() {
        if (web == null) return;
        String html = "<html><body style='margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#0b1120;color:#e6faff;font-family:sans-serif;text-align:center;padding:24px;'>";
        html += "<h1 style='color:#00e5ff;letter-spacing:2px;'>ANKA APK</h1>";
        html += "<p style='opacity:.8;'>Bağlantı koptu!<br>Anka'ya ulaşmak için internetini kontrol et.</p>";
        html += "<button onclick=\"location.href='" + SITE + "'\" style='margin-top:24px;padding:14px 32px;border:2px solid #00e5ff;border-radius:30px;background:transparent;color:#00e5ff;font-size:16px;font-weight:bold;'>Yeniden Bağlan</button>";
        html += "</body></html>";
        web.loadDataWithBaseURL(SITE, html, "text/html", "UTF-8", null);
    }

    private void leaveFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        if (web != null) web.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
    }

    private void showNoWebView(Throwable t) {
        TextView tv = new TextView(this);
        tv.setBackgroundColor(Color.parseColor("#0B1120"));
        tv.setTextColor(Color.parseColor("#E6FAFF"));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(24), dp(24), dp(24), dp(24));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setText("Anka açılamadı.\n\nTelefonunda \"Android System WebView\" "
            + "kapalı ya da eski olabilir. Play Store'dan WebView'i güncelle/etkinleştir, sonra tekrar dene.\n\n"
            + "Android " + Build.VERSION.RELEASE + " • " + Build.MODEL
            + (t != null ? "\n" + t.getClass().getSimpleName() : ""));
        setContentView(tv);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        try {
            SharedPreferences cp0 = getSharedPreferences("crash", MODE_PRIVATE);
            final String crash = cp0.getString("last", null);
            if (crash != null) {
                cp0.edit().remove("last").commit();
                showCrashScreen(crash);
                return;
            }
        } catch (Throwable ignored) { }

        int dark = Color.parseColor("#0B1120");
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);
        getWindow().setBackgroundDrawable(new ColorDrawable(dark));

        try {
            web = new WebView(this);
        } catch (Throwable t) {
            web = null;
            showNoWebView(t);
            return;
        }

        root = new FrameLayout(this);
        web.setBackgroundColor(dark);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        buildLoader();
        root.addView(loader, new FrameLayout.LayoutParams(-1, -1));

        splash = new FrameLayout(this);
        splash.setBackgroundColor(dark);
        ImageView logo = new ImageView(this);
        setLogo(logo);
        splash.addView(logo, new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER));
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // DÜZELTME: bazı galeriler file:// yolu döndürüyor; false iken video/dosya okunamıyordu.
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        try {
            s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        } catch (Throwable ignored) { }

        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDatabaseEnabled(true);

        // CAPTCHA muafiyet çerezi WebView'da kalsın
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(web, true);
        } catch (Throwable ignored) { }

        web.addJavascriptInterface(new AnkaBridge(), "AnkaBridge");
        scheduleNotifyJob();

        // Android 9 için güvenli BroadcastReceiver kaydı
        try {
            IntentFilter f = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(dlReceiver, f, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(dlReceiver, f);
            }
            receiverRegistered = true;
        } catch (Throwable ignored) { }

        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            askDownload(url, userAgent, contentDisposition, mimetype));

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String scheme = u.getScheme();
                boolean isWeb = "http".equals(scheme) || "https".equals(scheme);
                if (isWeb && isSiteHost(u.getHost())) return false;

                // Google "olağan dışı trafik" (CAPTCHA) sayfası uygulama içinde çözülsün
                String gh = u.getHost();
                String gp = u.getPath();
                if (isWeb && gh != null && gp != null
                    && (gh.endsWith("google.com") || gh.endsWith("google.com.tr"))
                    && (gp.startsWith("/sorry") || gp.startsWith("/recaptcha"))) {
                    return false;
                }

                // Sayfa kendiliğinden yönleniyorsa (dokunma yok) uygulama içinde kal
                if (isWeb && (!r.hasGesture() || !r.isForMainFrame())) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, u);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Bu bağlantıyı açacak uygulama bulunamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap favicon) {
                super.onPageStarted(v, url, favicon);
                scheduleLoader();
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                Uri pu = url == null ? null : Uri.parse(url);
                if (pu != null && isSiteHost(pu.getHost())) {
                    String js = loadFx();
                    if (!js.isEmpty()) v.evaluateJavascript(js, null);
                }
                hideLoader();
                hideSplash();
                checkUpdate();
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    hideLoader();
                    showOffline();
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView v, android.webkit.RenderProcessGoneDetail d) {
                try {
                    if (root != null) root.removeView(v);
                    v.destroy();
                } catch (Throwable ignored) { }
                web = null;
                // DÜZELTME: sorunun bellek olup olmadığı anlaşılsın diye bilgi ver
                try {
                    Toast.makeText(MainActivity.this,
                        "Sayfa bellek yüzünden yenilendi (büyük dosya?)", Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) { }
                recreate();
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                ankaDialog("ANKA BİLDİRİMİ", message, null, "TAMAM", null,
                    (ok, t) -> result.confirm());
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                boolean del = message != null
                    && message.toLowerCase(new Locale("tr")).contains("sil");
                ankaDialog("ANKA ONAY", message, null,
                    del ? "SİL" : "TAMAM", del ? "KAPAT" : "İPTAL",
                    (ok, t) -> { if (ok) result.confirm(); else result.cancel(); });
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                                      String defaultValue, JsPromptResult result) {
                boolean edit = message != null
                    && message.toLowerCase(new Locale("tr")).contains("düzenle");
                ankaDialog(edit ? "ANKA DÜZENLE" : "ANKA GİRİŞ", message,
                    cleanText(defaultValue),
                    edit ? "KAYDET" : "TAMAM", "İPTAL",
                    (ok, t) -> { if (ok) result.confirm(t); else result.cancel(); });
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }

                // DÜZELTME: Android 6-12'de galeriden dosya okumak için depolama izni (ilk seferde sorar)
                if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 33
                    && MainActivity.this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                       != PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_READ_MEDIA);
                }

                filePathCallback = cb;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, PICK);
                } catch (Exception e) {
                    try {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        startActivityForResult(Intent.createChooser(i, "Dosya Seç"), PICK);
                    } catch (Exception ex) {
                        filePathCallback.onReceiveValue(null);
                        filePathCallback = null;
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) {
                    cb.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = cb;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                if (web != null) web.setVisibility(View.GONE);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                leaveFullscreen();
            }
        });

        web.loadUrl(SITE);
        web.postDelayed(this::hideSplash, 6000);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK) {
            if (filePathCallback == null) return;
            // DÜZELTME: seçici sonucunu resmi yöntemle oku
            Uri[] result = null;
            try {
                result = WebChromeClient.FileChooserParams.parseResult(res, data);
            } catch (Throwable ignored) { }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        if (web != null) web.onResume();
        if (pendingInstall != null && Build.VERSION.SDK_INT >= 26
            && getPackageManager().canRequestPackageInstalls()) {
            Uri u = pendingInstall;
            pendingInstall = null;
            installApk(u);
        }
    }

    @Override
    protected void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(dlReceiver); } catch (Exception ignored) { }
        }
        if (ringAnim != null) ringAnim.cancel();
        if (pulseAnim != null) pulseAnim.cancel();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (loader != null && loader.getVisibility() == View.VISIBLE) {
            hideLoader();
        }
        if (web == null) {
            super.onBackPressed();
        } else if (customView != null) {
            leaveFullscreen();
        } else if (web.canGoBack()) {
            web.goBack();
        } else if (System.currentTimeMillis() - lastBack < 2000) {
            super.onBackPressed();
        } else {
            lastBack = System.currentTimeMillis();
            Toast.makeText(this, "Anka'dan ayrılıyor musun? Çıkmak için bir kez daha geri bas", Toast.LENGTH_SHORT).show();
        }
    }
}
