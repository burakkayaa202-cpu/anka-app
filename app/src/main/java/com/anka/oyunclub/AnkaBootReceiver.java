package com.anka.oyunclub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AnkaBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Servisi burada tetikleyebilir veya MainActivity içindeki planlamayı devreye sokabilirsin
        }
    }
}
