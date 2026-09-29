package com.bigger.floatapp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean enabled = Prefs.get(context).getBoolean("enabled", false);
        boolean boot = Prefs.get(context).getBoolean("boot", true);
        if (enabled && boot && Settings.canDrawOverlays(context)) {
            Intent s = new Intent(context, FloatingService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(s); else context.startService(s);
        }
    }
}
