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

public class LeaderboardWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_leaderboard);
            
            // Havalı neon yüklenme ekranı
            views.setTextViewText(R.id.rank_1, "⚡ 1. [ VERİ ÇEKİLİYOR... ]");
            views.setTextViewText(R.id.rank_2, "⚡ 2. [ VERİ ÇEKİLİYOR... ]");
            views.setTextViewText(R.id.rank_3, "⚡ 3. [ VERİ ÇEKİLİYOR... ]");

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("liderlik");
            
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int sira = 1;
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String isim = child.child("isim").getValue(String.class);
                        
                        Object xpObj = child.child("xp").getValue();
                        String xp = xpObj != null ? xpObj.toString() : "0";

                        if (isim != null) {
                            if (sira == 1) {
                                views.setTextViewText(R.id.rank_1, "👑 1. " + isim.toUpperCase() + " » " + xp + " XP");
                            } else if (sira == 2) {
                                views.setTextViewText(R.id.rank_2, "🥈 2. " + isim.toUpperCase() + " » " + xp + " XP");
                            } else if (sira == 3) {
                                views.setTextViewText(R.id.rank_3, "🥉 3. " + isim.toUpperCase() + " » " + xp + " XP");
                            }
                        }
                        sira++;
                        if (sira > 3) break;
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.rank_1, "⚡ SİSTEM OFFLINE");
                    views.setTextViewText(R.id.rank_2, "⚡ BAĞLANTI KOPTU");
                    views.setTextViewText(R.id.rank_3, "⚡ YENİDEN DENENİYOR");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
