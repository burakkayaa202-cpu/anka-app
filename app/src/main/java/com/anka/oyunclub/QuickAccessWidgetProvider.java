package com.anka.oyunclub;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class QuickAccessWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_access);

            // Başlangıç için havalı neon yüklenme metni
            views.setTextViewText(R.id.widget_status_text, "⚡ ANKA BAĞLANTI KURULUYOR...");

            // 1. Firebase'den anlık duyuru veya genel sohbet durumunu güvenle çekiyoruz
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("durum");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Object val = snapshot.getValue();
                    String anlikDurum = val != null ? val.toString() : null;

                    if (anlikDurum != null && !anlikDurum.isEmpty()) {
                        views.setTextViewText(R.id.widget_status_text, "🚀 " + anlikDurum);
                    } else {
                        views.setTextViewText(R.id.widget_status_text, "⚡ ANKA APK » AKTİF & ONLINE");
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.widget_status_text, "⚡ SİSTEM OFFLINE - TIKLA");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            // 2. Widget'a tıklayınca doğrudan siteye (Blogger adresine) gitmesini sağlayan Intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ankaoyunclub.blogspot.com"));
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            views.setOnClickPendingIntent(R.id.widget_status_text, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
