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

public class StreakWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_streak);

            // Firebase Realtime Database referansı
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String streakStr = "0";

                    // Veriyi güvenli bir şekilde çekelim
                    Object val = snapshot.child("streak").getValue();
                    if (val != null) {
                        streakStr = val.toString();
                    } else {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.hasChild("streak")) {
                                Object childVal = child.child("child").getValue(); // veya alt düğüm değeri
                                if (childVal != null) {
                                    streakStr = childVal.toString();
                                    break;
                                }
                            }
                        }
                    }

                    // Havalı ve dinamik metin yapısı
                    views.setTextViewText(R.id.widget_streak_text, "🔥 " + streakStr + " GÜN 🔥");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.widget_streak_text, "⚡ ANKA OFFLINE");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            // İlk yüklenme anı için havalı geçici yazı
            views.setTextViewText(R.id.widget_streak_text, "⚡ YÜKLENİYOR...");
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
