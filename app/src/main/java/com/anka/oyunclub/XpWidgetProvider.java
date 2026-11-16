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

            // Firebase'den kullanıcının XP ve rütbe verisini çektiğimiz yer
            // (Kendi veritabanı yapına göre buradaki "users/aktif_kullanici" yolunu güncelleyebilirsin)
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Örnek olarak veritabanından çekiyoruz, kendi node adına göre ayarlayabilirsin
                    String xpDegeri = snapshot.child("xp").getValue(String.class);
                    String rutbeDegeri = snapshot.child("rutbe").getValue(String.class);

                    if (xpDegeri != null) {
                        views.setTextViewText(R.id.widget_xp_count, "Toplam XP: " + xpDegeri);
                    } else {
                        views.setTextViewText(R.id.widget_xp_count, "Toplam XP: 0");
                    }

                    if (rutbeDegeri != null) {
                        views.setTextViewText(R.id.widget_rank_title, "Rütbe: " + rutbeDegeri);
                    } else {
                        views.setTextViewText(R.id.widget_rank_title, "Rütbe: Yazılım Çırağı");
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.widget_xp_count, "Toplam XP: Hata!");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
