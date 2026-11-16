package com.anka.oyunclub;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class XpWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_xp);

            // Başlangıç için havalı neon yüklenme metinleri
            views.setTextViewText(R.id.widget_xp_count, "⚡ XP: HESaplanıyor...");
            views.setTextViewText(R.id.widget_rank_title, "🛡️ RÜTBE: YÜKLENİYOR...");

            // Firebase'den kullanıcının XP ve rütbe verisini güvenli şekilde çekiyoruz
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // XP değerini esnek okuma (Long, Integer veya String olabilir)
                    Object xpObj = snapshot.child("xp").getValue();
                    String xpDegeri = xpObj != null ? xpObj.toString() : null;

                    // Rütbe değerini okuma
                    String rutbeDegeri = snapshot.child("rutbe").getValue(String.class);

                    // Eğer direkt users altında değil de çocuk düğümlerde tutuluyorsa tarayalım
                    if (xpDegeri == null) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.hasChild("xp")) {
                                Object childXp = child.child("xp").getValue();
                                if (childXp != null) xpDegeri = childXp.toString();
                            }
                            if (child.hasChild("rutbe")) {
                                String childRutbe = child.child("rutbe").getValue(String.class);
                                if (childRutbe != null) rutbeDegeri = childRutbe;
                            }
                        }
                    }

                    if (xpDegeri != null) {
                        views.setTextViewText(R.id.widget_xp_count, "⚡ TOPLAM XP » " + xpDegeri);
                    } else {
                        views.setTextViewText(R.id.widget_xp_count, "⚡ TOPLAM XP » 0");
                    }

                    if (rutbeDegeri != null) {
                        views.setTextViewText(R.id.widget_rank_title, "🛡️ RÜTBE » " + rutbeDegeri.toUpperCase());
                    } else {
                        views.setTextViewText(R.id.widget_rank_title, "🛡️ RÜTBE » YAZILIM ÇIRAĞI");
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.widget_xp_count, "⚡ XP: OFFLINE");
                    views.setTextViewText(R.id.widget_rank_title, "🛡️ RÜTBE: BAĞLANTI KOPTU");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
