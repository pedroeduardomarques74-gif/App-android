package com.bigger.floatapp;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloatingService extends Service {
    private WindowManager wm;
    private View bubble;
    private WindowManager.LayoutParams params;
    private float downRawX, downRawY;
    private int startX, startY;
    private boolean moved;
    private final AtomicBoolean removing = new AtomicBoolean(false);

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1001, buildNotification());
        showBubble();
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "bigger_float") : new Notification.Builder(this);
        return b.setContentTitle("BIGGER FLOAT ativo").setContentText("A bolinha flutuante está em execução")
                .setSmallIcon(android.R.drawable.presence_online).setOngoing(true).setContentIntent(pi).build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel("bigger_float", "BIGGER FLOAT", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Mantém a bolinha flutuante funcionando");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private void showBubble() {
        if (!Settings.canDrawOverlays(this) || bubble != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        TextView v = new TextView(this);
        v.setText("●"); v.setGravity(Gravity.CENTER); v.setTextColor(0xFFFFFFFF); v.setTextSize(28);
        int sizeDp = Prefs.get(this).getInt("size", 64);
        int px = dp(sizeDp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL); bg.setColor(0xFF246BFD); bg.setStroke(dp(2), 0xFFFFFFFF);
        v.setBackground(bg);
        v.setAlpha(Prefs.get(this).getInt("alpha", 90) / 100f);

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(px, px, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Prefs.get(this).getInt("x", dp(16));
        params.y = Prefs.get(this).getInt("y", dp(180));
        clampToScreen();

        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = e.getRawX(); downRawY = e.getRawY(); startX = params.x; startY = params.y; moved = false; return true;
                case MotionEvent.ACTION_MOVE:
                    int nx = startX + Math.round(e.getRawX() - downRawX);
                    int ny = startY + Math.round(e.getRawY() - downRawY);
                    if (Math.abs(e.getRawX()-downRawX) > dp(4) || Math.abs(e.getRawY()-downRawY) > dp(4)) moved = true;
                    params.x = nx; params.y = ny; clampToScreen(); safeUpdate(); return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) launchTarget();
                    else {
                        if (Prefs.get(this).getBoolean("snap", true)) snapSide();
                        savePosition();
                    }
                    return true;
            }
            return false;
        });
        bubble = v;
        try { wm.addView(bubble, params); } catch (Exception ignored) { bubble = null; }
    }

    private void launchTarget() {
        String pkg = Prefs.get(this).getString("target", "");
        if (pkg == null || pkg.isEmpty()) {
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i); return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try { startActivity(launch); } catch (Exception ignored) {}
        }
    }

    private void snapSide() {
        android.graphics.Point p = new android.graphics.Point(); wm.getDefaultDisplay().getSize(p);
        int maxX = Math.max(0, p.x - params.width);
        params.x = params.x + params.width/2 < p.x/2 ? 0 : maxX;
        clampToScreen(); safeUpdate();
    }

    private void clampToScreen() {
        if (wm == null || params == null) return;
        android.graphics.Point p = new android.graphics.Point(); wm.getDefaultDisplay().getSize(p);
        params.x = Math.max(0, Math.min(params.x, Math.max(0, p.x - params.width)));
        params.y = Math.max(0, Math.min(params.y, Math.max(0, p.y - params.height)));
    }

    private void savePosition() {
        Prefs.get(this).edit().putInt("x", params.x).putInt("y", params.y).apply();
    }

    private void safeUpdate() { try { if (bubble != null) wm.updateViewLayout(bubble, params); } catch (Exception ignored) {} }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "refresh".equals(intent.getAction())) { removeBubble(); showBubble(); }
        return START_STICKY;
    }

    private void removeBubble() {
        if (bubble != null && removing.compareAndSet(false, true)) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null; removing.set(false);
        }
    }

    @Override public void onDestroy() { removeBubble(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }
}
