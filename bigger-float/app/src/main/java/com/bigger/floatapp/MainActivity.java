package com.bigger.floatapp;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout listContainer;
    private EditText search;
    private final ArrayList<AppItem> allApps = new ArrayList<>();
    private final HashSet<String> favorites = new HashSet<>();
    private static final int BG = 0xFF0F172A;
    private static final int CARD = 0xFF1E293B;
    private static final int BLUE = 0xFF246BFD;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF94A3B8;

    static class AppItem {
        String name, pkg;
        android.graphics.drawable.Drawable icon;
        AppItem(String n, String p, android.graphics.drawable.Drawable i){ name=n; pkg=p; icon=i; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        loadFavorites();
        buildUi();
        loadApps();
        render("");
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
        root.setBackgroundColor(BG);
        sv.addView(root);

        TextView title = tv("BIGGER OVERLAY", 28, true, TEXT);
        root.addView(title);

        TextView sub = tv("Atalhos para ativar ou remover a sobreposição dos próprios aplicativos", 14, false, 0xFFCBD5E1);
        root.addView(sub, lp(-1,-2,0,4,0,18));

        TextView info = tv(
                "Este app não cria bolinha flutuante. Ele abre as permissões do Android. Se o aplicativo escolhido tiver uma bolha/janela própria, é ele que vai mostrar.",
                13, false, 0xFFCBD5E1
        );
        LinearLayout infoCard = card();
        infoCard.addView(info);
        root.addView(infoCard, lp(-1,-2,0,0,0,14));

        Button geral = btn("GERENCIAR TODAS AS SOBREPOSIÇÕES");
        geral.setOnClickListener(v -> openGeneralOverlaySettings());
        root.addView(geral, lp(-1,dp(50),0,0,0,14));

        search = new EditText(this);
        search.setHint("Pesquisar aplicativo...");
        search.setHintTextColor(0xFF64748B);
        search.setTextColor(TEXT);
        search.setSingleLine(true);
        search.setPadding(dp(14),0,dp(14),0);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(CARD); sBg.setCornerRadius(dp(14));
        search.setBackground(sBg);
        root.addView(search, lp(-1,dp(50),0,0,0,18));

        TextView heading = tv("Aplicativos instalados", 17, true, TEXT);
        root.addView(heading, lp(-1,-2,0,0,0,8));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ render(s.toString()); }
            public void afterTextChanged(Editable e){}
        });

        setContentView(sv);
    }

    private void loadApps() {
        allApps.clear();
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN, null);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> list = pm.queryIntentActivities(launcher, 0);
        HashSet<String> seen = new HashSet<>();

        for (ResolveInfo r : list) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || seen.contains(pkg)) continue;
            seen.add(pkg);
            String name = r.loadLabel(pm).toString();
            android.graphics.drawable.Drawable icon = r.loadIcon(pm);
            allApps.add(new AppItem(name, pkg, icon));
        }

        Collections.sort(allApps, (a,b) -> {
            boolean af = favorites.contains(a.pkg), bf = favorites.contains(b.pkg);
            if (af != bf) return af ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    private void render(String q) {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        int count = 0;
        for (AppItem a : allApps) {
            if (!query.isEmpty() && !a.name.toLowerCase(Locale.ROOT).contains(query)
                    && !a.pkg.toLowerCase(Locale.ROOT).contains(query)) continue;
            listContainer.addView(appRow(a), lp(-1,-2,0,0,0,10));
            count++;
        }

        if (count == 0) {
            TextView empty = tv("Nenhum aplicativo encontrado.", 14, false, MUTED);
            listContainer.addView(empty, lp(-1,-2,0,14,0,0));
        }
    }

    private View appRow(AppItem a) {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(a.icon);
        top.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView name = tv(a.name, 16, true, TEXT);
        TextView pkg = tv(a.pkg, 11, false, MUTED);
        names.addView(name);
        names.addView(pkg);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0,-2,1f);
        np.setMargins(dp(12),0,dp(8),0);
        top.addView(names,np);

        TextView star = tv(favorites.contains(a.pkg) ? "★" : "☆", 28, false,
                favorites.contains(a.pkg) ? 0xFFFFC107 : 0xFF64748B);
        star.setGravity(Gravity.CENTER);
        star.setPadding(dp(8),0,dp(8),0);
        star.setOnClickListener(v -> {
            if (favorites.contains(a.pkg)) favorites.remove(a.pkg); else favorites.add(a.pkg);
            saveFavorites();
            loadApps();
            render(search.getText().toString());
        });
        top.addView(star, new LinearLayout.LayoutParams(dp(48),dp(48)));

        card.addView(top);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0,dp(12),0,0);

        Button overlay = btn("SOBREPOSIÇÃO");
        Button details = btn("CONFIG. DO APP");
        actions.addView(overlay, new LinearLayout.LayoutParams(0,dp(46),1f));
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(0,dp(46),1f);
        dp2.setMargins(dp(8),0,0,0);
        actions.addView(details,dp2);

        overlay.setOnClickListener(v -> openOverlayFor(a));
        details.setOnClickListener(v -> openAppDetails(a.pkg));

        card.addView(actions);
        return card;
    }

    private void openOverlayFor(AppItem a) {
        getSharedPreferences("overlay_manager", MODE_PRIVATE)
                .edit().putString("last_pkg", a.pkg).putString("last_name", a.name).apply();

        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + a.pkg));
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(i);
                Toast.makeText(this,
                        "Procure " + a.name + " na lista e ative ou desative a permissão.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                openAppDetails(a.pkg);
            }
        }
    }

    private void openGeneralOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    private void openAppDetails(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir as configurações desse app.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFavorites() {
        Set<String> saved = getSharedPreferences("overlay_manager", MODE_PRIVATE)
                .getStringSet("favorites", Collections.emptySet());
        favorites.clear();
        favorites.addAll(saved);
    }

    private void saveFavorites() {
        getSharedPreferences("overlay_manager", MODE_PRIVATE)
                .edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setPadding(dp(14),dp(14),dp(14),dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD); bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        return c;
    }

    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE); bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        return b;
    }

    private TextView tv(String s,int sp,boolean bold,int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int w,int h,int l,int top,int r,int bottom){
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h);
        p.setMargins(l,top,r,bottom);
        return p;
    }

    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }
}
