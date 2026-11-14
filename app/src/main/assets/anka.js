package com.anka.oyunclub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final String SITE = "https://ankaoyunclub.blogspot.com";
    private static final int PICK = 1;
    private FrameLayout root;
    private WebView web;
    private FrameLayout splash;
    private ValueCallback<Uri[]> filePathCallback;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private long lastBack = 0;
    private String fxJs = null;

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density);
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
            fxJs = ""; // assets içinde anka.js yoksa uygulamanın çökmesi engellendi
        }
        return fxJs;
    }

    private void hideSplash() {
        if (splash != null && splash.getVisibility() == View.VISIBLE) {
            splash.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> splash.setVisibility(View.GONE));
        }
    }

    private void showOffline() {
        String html = "<html><body style='margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#0b1120;color:#e6faff;font-family:sans-serif;text-align:center;padding:24px;'>";
        html += "<h1 style='color:#00e5ff;letter-spacing:2px;'>ANKA APK</h1>";
        html += "<p style='opacity:.8;'>Bağlantı koptu!<br>Anka'ya ulaşmak için internetini kontrol et.</p>";
        html += "<button onclick=\"location.href='" + SITE + "'\" style='margin-top:24px;padding:14px 32px;border:2px solid #00e5ff;border-radius:30px;background:transparent;color:#00e5ff;font-size:16px;font-weight:bold;cursor:pointer;'>Yeniden Bağlan</button>";
        html += "</body></html>";
        web.loadDataWithBaseURL(SITE, html, "text/html", "UTF-8", null);
    }

    private void leaveFullscreen() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        web.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int dark = Color.parseColor("#0B1120");
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);
        getWindow().setBackgroundDrawable(new ColorDrawable(dark));

        root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(dark);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        splash = new FrameLayout(this);
        splash.setBackgroundColor(dark);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        splash.addView(logo, new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER));
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDatabaseEnabled(true);

        // Güvenli İndirme Yöneticisi
        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Dosya indirilemedi!", Toast.LENGTH_SHORT).show();
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String scheme = u.getScheme();
                String host = u.getHost();
                boolean isWeb = "http".equals(scheme) || "https".equals(scheme);
                if (isWeb && host != null && host.endsWith("blogspot.com")) return false;
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
            public void onPageFinished(WebView v, String url) {
                String js = loadFx();
                if (js != null && !js.isEmpty()) v.evaluateJavascript(js, null);
                hideSplash();
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                // Sadece ana sayfa yüklenemezse hata ekranını göster
                if (r != null && r.isForMainFrame()) {
                    showOffline();
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("ANKA BİLDİRİMİ")
                    .setMessage(message)
                    .setPositiveButton("TAMAM", (dialog, which) -> result.confirm())
                    .setCancelable(false)
                    .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, android.webkit.JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("ANKA ONAY")
                    .setMessage(message)
                    .setPositiveButton("TAMAM", (dialog, which) -> result.confirm())
                    .setNegativeButton("İPTAL", (dialog, which) -> result.cancel())
                    .setCancelable(false)
                    .show();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = cb;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, PICK);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show();
                    return false;
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
                web.setVisibility(View.GONE);
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

        // Modern Geri Tuşu Yönetimi (OnBackPressedDispatcher)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null) {
                    leaveFullscreen();
                } else if (web.canGoBack()) {
                    web.goBack();
                } else if (System.currentTimeMillis() - lastBack < 2000) {
                    finish();
                } else {
                    lastBack = System.currentTimeMillis();
                    Toast.makeText(MainActivity.this, "Anka'dan ayrılıyor musun? Çıkmak için bir kez daha geri bas", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (b == null) {
            web.loadUrl(SITE);
        } else {
            web.restoreState(b);
        }
        
        web.postDelayed(this::hideSplash, 6000);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK) {
            if (filePathCallback != null) {
                Uri[] result = null;
                if (res == RESULT_OK && data != null) {
                    result = WebChromeClient.FileChooserParams.parseResult(res, data);
                }
                filePathCallback.onReceiveValue(result);
                filePathCallback = null;
            }
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        web.saveState(o);
    }
}
