package com.bigger.floatapp;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView status, targetLabel;
    private Switch enable, boot, snap;
    private SeekBar size, alpha;

    @Override public void onCreate(Bundle b) { super.onCreate(b); buildUi(); loadUi(); }
    @Override protected void onResume() { super.onResume(); updatePermissionStatus(); }

    private void buildUi() {
        ScrollView sv = new ScrollView(this); sv.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(40)); root.setBackgroundColor(Color.rgb(15,23,42)); sv.addView(root);

        TextView title = text("BIGGER FLOAT", 30, true); root.addView(title);
        TextView sub = text("Controle completo da sua bolinha flutuante", 15, false); sub.setTextColor(0xFFCBD5E1); root.addView(sub, lp(-1,-2,0,0,0,18));

        status = text("", 15, true); root.addView(card(status));
        Button permission = button("ABRIR PERMISSÃO DE SOBREPOSIÇÃO");
        permission.setOnClickListener(v -> openOverlaySettings()); root.addView(permission, lp(-1,dp(52),0,12,0,18));

        enable = sw("Ativar bolinha flutuante"); root.addView(enable);
        enable.setOnCheckedChangeListener((v, checked) -> toggleBubble(checked));

        targetLabel = text("Aplicativo ao tocar: Nenhum", 16, true); root.addView(card(targetLabel), lp(-1,-2,0,16,0,8));
        Button choose = button("ESCOLHER APLICATIVO"); choose.setOnClickListener(v -> chooseApp()); root.addView(choose);

        addSection("Tamanho da bolinha");
        size = new SeekBar(this); size.setMax(60); root.addView(size);
        size.setOnSeekBarChangeListener(listener("size", 40, true));

        addSection("Transparência");
        alpha = new SeekBar(this); alpha.setMax(60); root.addView(alpha);
        alpha.setOnSeekBarChangeListener(listener("alpha", 40, true));

        snap = sw("Encostar automaticamente na lateral"); root.addView(snap, lp(-1,-2,0,14,0,0));
        snap.setOnCheckedChangeListener((b,c)-> Prefs.get(this).edit().putBoolean("snap", c).apply());

        boot = sw("Iniciar automaticamente ao ligar o celular"); root.addView(boot);
        boot.setOnCheckedChangeListener((b,c)-> Prefs.get(this).edit().putBoolean("boot", c).apply());

        Button center = button("REDEFINIR POSIÇÃO DA BOLINHA");
        center.setOnClickListener(v -> {
            Prefs.get(this).edit().putInt("x", dp(16)).putInt("y", dp(180)).apply();
            refreshService(); Toast.makeText(this, "Posição redefinida", Toast.LENGTH_SHORT).show();
        });
        root.addView(center, lp(-1,dp(52),0,20,0,8));

        TextView tip = text("A posição é salva automaticamente sempre que você arrasta e solta a bolinha.", 14, false);
        tip.setTextColor(0xFF94A3B8); root.addView(tip);
        setContentView(sv);
    }

    private void loadUi() {
        enable.setChecked(Prefs.get(this).getBoolean("enabled", false));
        boot.setChecked(Prefs.get(this).getBoolean("boot", true));
        snap.setChecked(Prefs.get(this).getBoolean("snap", true));
        size.setProgress(Prefs.get(this).getInt("size",64)-40);
        alpha.setProgress(Prefs.get(this).getInt("alpha",90)-40);
        updateTargetLabel(); updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean ok = Settings.canDrawOverlays(this);
        status.setText(ok ? "✓ Permissão de sobreposição ativada" : "⚠ Permissão de sobreposição necessária");
        status.setTextColor(ok ? 0xFF22C55E : 0xFFF59E0B);
        if (enable != null && enable.isChecked() && ok) startBubbleService();
    }

    private void toggleBubble(boolean checked) {
        Prefs.get(this).edit().putBoolean("enabled", checked).apply();
        if (checked) {
            if (!Settings.canDrawOverlays(this)) { openOverlaySettings(); return; }
            startBubbleService();
        } else stopService(new Intent(this, FloatingService.class));
    }

    private void startBubbleService() {
        Intent i = new Intent(this, FloatingService.class);
        try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); }
        catch (Exception e) { Toast.makeText(this, "Não foi possível iniciar a bolinha", Toast.LENGTH_LONG).show(); }
    }

    private void refreshService() {
        if (!Prefs.get(this).getBoolean("enabled", false) || !Settings.canDrawOverlays(this)) return;
        Intent i = new Intent(this, FloatingService.class); i.setAction("refresh");
        try { if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i); } catch(Exception ignored) {}
    }

    private void openOverlaySettings() {
        try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
        catch(Exception e){ startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); }
    }

    private void chooseApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN); intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(getPackageManager()));
        ArrayList<ResolveInfo> filtered = new ArrayList<>(); ArrayList<String> names = new ArrayList<>();
        for (ResolveInfo r : list) {
            if (r.activityInfo.packageName.equals(getPackageName())) continue;
            filtered.add(r); names.add(r.loadLabel(getPackageManager()).toString());
        }
        new AlertDialog.Builder(this).setTitle("Escolha o aplicativo")
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    ResolveInfo r = filtered.get(which);
                    Prefs.get(this).edit().putString("target", r.activityInfo.packageName).putString("target_name", names.get(which)).apply();
                    updateTargetLabel();
                }).setNegativeButton("Cancelar", null).show();
    }

    private void updateTargetLabel() {
        String n = Prefs.get(this).getString("target_name", "Nenhum");
        targetLabel.setText("Aplicativo ao tocar: " + n);
    }

    private SeekBar.OnSeekBarChangeListener listener(String key, int base, boolean refresh) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int p,boolean from){ if(from){ Prefs.get(MainActivity.this).edit().putInt(key, base+p).apply(); if(refresh) refreshService(); } }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        };
    }

    private void addSection(String s){ TextView t=text(s,16,true); root.addView(t, lp(-1,-2,0,20,0,4)); }
    private Switch sw(String s){ Switch x=new Switch(this); x.setText(s); x.setTextColor(Color.WHITE); x.setTextSize(16); x.setPadding(0,dp(10),0,dp(10)); return x; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(13); b.setAllCaps(false); return b; }
    private TextView text(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(sp); if(bold)t.setTypeface(null,1); return t; }
    private View card(TextView t){ LinearLayout c=new LinearLayout(this); c.setPadding(dp(16),dp(16),dp(16),dp(16)); c.setBackgroundColor(0xFF1E293B); c.addView(t); return c; }
    private LinearLayout.LayoutParams lp(int w,int h,int l,int top,int r,int bot){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(l,top,r,bot); return p; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
