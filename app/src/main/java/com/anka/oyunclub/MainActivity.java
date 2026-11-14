package com.anka.oyunclub;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    private static final String SITE = "https://ankaoyunclub.blogspot.com";
    private static final int PICK = 1;
    private WebView web;
    private ProgressBar bar;
    private FrameLayout splash;
    private ValueCallback<Uri[]> filePathCallback;

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density);
    }

    private void hideSplash() {
        if (splash.getVisibility() == View.VISIBLE) {
            splash.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> splash.setVisibility(View.GONE));
        }
    }

    private void showOffline() {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>body{margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;"
            + "justify-content:center;background:#0b1120;color:#e6faff;font-family:sans-serif;"
            + "text-align:center;padding:24px;box-sizing:border-box}"
            + "h1{color:#00e5ff;letter-spacing:2px;text-shadow:0 0 12px #00e5ff}"
            + "p{opacity:.8;line-height:1.5}"
            + "button{margin-top:24px;padding:14px 32px;border:2px solid #00e5ff;border-radius:30px;"
            + "background:transparent;color:#00e5ff;font-size:16px;font-weight:bold}</style></head>"
            + "<body><h1>ANKA APK</h1><p>İnternet bağlantısı yok.<br>Bağlantını kontrol edip tekrar dene.</p>"
            + "<button onclick=\"location.href='" + SITE + "'\">Tekrar Dene</button></body></html>";
        web.loadDataWithBaseURL(SITE, html, "text/html", "UTF-8", null);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int dark = Color.parseColor("#0B1120");
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);
        getWindow().setBackgroundDrawable(new ColorDrawable(dark));
        getWindow().getDecorView().setSystemUiVisibility(0);

        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(dark);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
        bar.setVisibility(View.GONE);
        root.addView(bar, new FrameLayout.LayoutParams(-1, dp(3)));

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
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setDownloadListener((url, ua, cd, mime, len) ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String host = r.getUrl().getHost();
                if (host != null && host.endsWith("blogspot.com")) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                hideSplash();
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showOffline();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), PICK);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (b == null) web.loadUrl(SITE); else web.restoreState(b);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && filePathCallback != null) {
            Uri[] result = null;
            if (res == RESULT_OK && data != null) {
                result = WebChromeClient.FileChooserParams.parseResult(res, data);
            }
            filePathCallback.onReceiveValue(result);
            filePathCallback = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        web.saveState(o);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
