package com.anka.oyunclub;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Android 9 ve altı cihazlarda sınıf yükleme çökmesini kesin olarak önler
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));

                String s = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") "
                        + Build.MANUFACTURER + " " + Build.MODEL
                        + "\nThread: " + t.getName() + "\n\n" + sw.toString();

                // Çökme anında senkron olarak (commit) kaydediyoruz ki silinmesin
                getSharedPreferences("crash", MODE_PRIVATE)
                        .edit()
                        .putString("last", s)
                        .commit();

                // Ek olarak doğrudan Download klasörüne txt olarak yazdırıyoruz
                File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "anka_hata.txt");
                FileWriter writer = new FileWriter(logFile, false);
                writer.write(s);
                writer.flush();
                writer.close();
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
