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

            // Firebase'den streak (seri) verisini çektiğimiz düğüm
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String streakDegeri = snapshot.child("streak").getValue(String.class);

                    if (streakDegeri != null) {
                        views.setTextViewText(R.id.widget_streak_text, "🔥 Seri: " + streakDegeri + " Gün");
                    } else {
                        views.setTextViewText(R.id.widget_streak_text, "🔥 Seri: 0 Gün");
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    views.setTextViewText(R.id.widget_streak_text, "🔥 Seri: Hata!");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            });

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
