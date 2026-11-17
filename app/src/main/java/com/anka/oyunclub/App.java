package com.anka.oyunclub;

import android.app.Application;
import android.os.Build;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler old =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                String s = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") "
                    + Build.MANUFACTURER + " " + Build.MODEL + "\nThread: " + t.getName() + "\n" + sw;
                getSharedPreferences("crash", MODE_PRIVATE)
                    .edit().putString("last", s).commit();
            } catch (Throwable ignored) { }
            if (old != null) old.uncaughtException(t, e);
        });
    }
}
