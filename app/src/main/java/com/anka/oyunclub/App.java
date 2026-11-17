package com.anka.oyunclub;

import android.app.Application;
import android.os.Build;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler old =
            Thread.getDefaultUncaughtExceptionHandler();
            
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                
                String s = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") "
                    + Build.MANUFACTURER + " " + Build.MODEL 
                    + "\nThread: " + t.getName() + "\n\n" + sw.toString();
                
                // commit() yerine apply() kullanarak çökme anında main thread'in kilitlenmesini önlüyoruz
                getSharedPreferences("crash", MODE_PRIVATE)
                    .edit()
                    .putString("last", s)
                    .apply();
            } catch (Throwable ignored) { }
            
            if (old != null) {
                old.uncaughtException(t, e);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }
}
