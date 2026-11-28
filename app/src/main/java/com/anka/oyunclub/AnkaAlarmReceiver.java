package com.anka.oyunclub;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AnkaAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        schedule(ctx); // zinciri devam ettir
        try {
            JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js != null) {
                js.schedule(new JobInfo.Builder(1003, new ComponentName(ctx, AnkaNotifyJob.class))
                    .setOverrideDeadline(1000)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build());
            }
        } catch (Throwable ignored) { }
    }

    static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 77,
                new Intent(ctx, AnkaAlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            long at = System.currentTimeMillis() + 10 * 60 * 1000L;
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Throwable ignored) { }
    }
}
