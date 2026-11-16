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
            
            // Firebase veritabanı düğümün (Sitenin veriyi kaydettiği yer)
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("liderlik");
            
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int sira = 1;
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String isim = child.child("isim").getValue(String.class);
                        String xp = child.child("xp").getValue(String.class);

                        if (isim != null && xp != null) {
                            if (sira == 1) {
                                views.setTextViewText(R.id.rank_1, "1. 🥇 " + isim + " - " + xp + " XP");
                            } else if (sira == 2) {
                                views.setTextViewText(R.id.rank_2, "2. 🥈 " + isim + " - " + xp + " XP");
                            } else if (sira == 3) {
                                views.setTextViewText(R.id.rank_3, "3. 🥉 " + isim + " - " + xp + " XP");
                            }
                        }
                        sira++;
                        if (sira > 3) break;
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.rank_1, "Bağlantı hatası!");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
