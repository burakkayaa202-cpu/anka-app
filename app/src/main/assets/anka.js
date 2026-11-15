WebSettings s = web.getSettings();
s.setJavaScriptEnabled(true);
s.setDomStorageEnabled(true);
s.setAllowFileAccess(true);
s.setLoadsImagesAutomatically(true);
s.setMediaPlaybackRequiresUserGesture(true);
s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

// Chrome'daki gibi görünmesi için:
s.setUseWideViewPort(true);
s.setLoadWithOverviewMode(false);   // true'ydu, sayfayı küçültüp ölçüyü bozuyor
s.setTextZoom(100);                 // telefonun yazı boyutu ayarı siteyi bozmasın
s.setSupportZoom(false);
s.setBuiltInZoomControls(false);

// Site "wv" (WebView) algılayıp farklı çizmesin diye Chrome kimliği:
String ua = s.getUserAgentString().replace("; wv", "");
s.setUserAgentString(ua);

s.setCacheMode(WebSettings.LOAD_DEFAULT);
s.setDatabaseEnabled(true);
