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
    private static final int PURPLE = 0xFF7C3AED;
    private static final int DARK = 0xFF334155;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF94A3B8;

    static class AppItem {
        String name, pkg;
        android.graphics.drawable.Drawable icon;
        AppItem(String n, String p, android.graphics.drawable.Drawable i){ name=n; pkg=p; icon=i; }
    }

    static class Candidate {
        String title;
        ComponentName component;
        Candidate(String t, ComponentName c){ title=t; component=c; }
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

        root.addView(tv("BIGGER OVERLAY V3", 28, true, TEXT));
        root.addView(tv(
                "Sobreposição e bolha flutuante tratadas separadamente",
                14, false, 0xFFCBD5E1
        ), lp(-1,-2,0,4,0,18));

        LinearLayout info = card();
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(tv("COMO FUNCIONA", 13, true, 0xFF60A5FA));
        info.addView(tv(
                "• SOBREPOSIÇÃO: abre a permissão do Android.\n" +
                "• BOLHA / JANELA: procura uma tela própria que o aplicativo tenha disponibilizado.\n" +
                "• Se o app não expuser essa configuração, o BIGGER não altera a função interna escondida.",
                13, false, 0xFFCBD5E1
        ), lp(-1,-2,0,8,0,0));
        root.addView(info, lp(-1,-2,0,0,0,14));

        Button geral = btn("GERENCIAR TODAS AS SOBREPOSIÇÕES", BLUE);
        geral.setOnClickListener(v -> openGeneralOverlaySettings());
        root.addView(geral, lp(-1,dp(50),0,0,0,14));

        search = new EditText(this);
        search.setHint("Pesquisar 99, Waze, Uber...");
        search.setHintTextColor(0xFF64748B);
        search.setTextColor(TEXT);
        search.setSingleLine(true);
        search.setPadding(dp(14),0,dp(14),0);
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(CARD); sBg.setCornerRadius(dp(14));
        search.setBackground(sBg);
        root.addView(search, lp(-1,dp(50),0,0,0,18));

        root.addView(tv("Aplicativos instalados", 17, true, TEXT), lp(-1,-2,0,0,0,8));

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
            allApps.add(new AppItem(name, pkg, r.loadIcon(pm)));
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
            if (!query.isEmpty() &&
                    !a.name.toLowerCase(Locale.ROOT).contains(query) &&
                    !a.pkg.toLowerCase(Locale.ROOT).contains(query)) continue;
            listContainer.addView(appRow(a), lp(-1,-2,0,0,0,10));
            count++;
        }

        if (count == 0) {
            listContainer.addView(tv("Nenhum aplicativo encontrado.", 14, false, MUTED),
                    lp(-1,-2,0,14,0,0));
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
        names.addView(tv(a.name, 16, true, TEXT));
        names.addView(tv(a.pkg, 11, false, MUTED));
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

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0,dp(12),0,0);

        Button overlay = btn("SOBREPOSIÇÃO", BLUE);
        Button bubble = btn("BOLHA / JANELA", PURPLE);
        row1.addView(overlay, new LinearLayout.LayoutParams(0,dp(46),1f));
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0,dp(46),1f);
        b2.setMargins(dp(8),0,0,0);
        row1.addView(bubble,b2);

        overlay.setOnClickListener(v -> openOverlayFor(a));
        bubble.setOnClickListener(v -> openBubbleSettings(a));
        card.addView(row1);

        Button details = btn("CONFIGURAÇÕES DO APLICATIVO", DARK);
        details.setOnClickListener(v -> openAppDetails(a.pkg));
        card.addView(details, lp(-1,dp(44),0,8,0,0));

        return card;
    }

    private void openOverlayFor(AppItem a) {
        remember(a);
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + a.pkg));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                Toast.makeText(this,
                        "Procure " + a.name + " e ative ou desative a sobreposição.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                openAppDetails(a.pkg);
            }
        }
    }

    private void openBubbleSettings(AppItem a) {
        remember(a);
        ArrayList<Candidate> candidates = findBubbleCandidates(a);

        if (candidates.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Bolha / janela de " + a.name)
                    .setMessage(
                            "Não encontrei uma tela pública de configuração de bolha/janela flutuante dentro desse aplicativo. " +
                            "Isso normalmente significa que essa opção fica escondida dentro do próprio app ou não existe nessa versão.\n\n" +
                            "Você pode abrir o aplicativo ou as configurações dele para procurar manualmente."
                    )
                    .setPositiveButton("ABRIR APP", (d,w) -> launchApp(a.pkg))
                    .setNeutralButton("CONFIG. DO APP", (d,w) -> openAppDetails(a.pkg))
                    .setNegativeButton("CANCELAR", null)
                    .show();
            return;
        }

        String[] labels = new String[candidates.size()];
        for (int i=0;i<candidates.size();i++) labels[i] = candidates.get(i).title;

        new AlertDialog.Builder(this)
                .setTitle("Possíveis telas de bolha / janela")
                .setMessage("Escolha uma tela disponibilizada pelo próprio aplicativo:")
                .setItems(labels, (d,which) -> launchCandidate(candidates.get(which), a))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private ArrayList<Candidate> findBubbleCandidates(AppItem a) {
        ArrayList<Candidate> result = new ArrayList<>();
        PackageManager pm = getPackageManager();

        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = pm.getPackageInfo(a.pkg,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES));
            } else {
                info = pm.getPackageInfo(a.pkg, PackageManager.GET_ACTIVITIES);
            }

            if (info.activities == null) return result;

            String[] keys = {
                    "bubble","bubbles","float","floating","popup","pop_up",
                    "overlay","window","chathead","chat_head","pictureinpicture",
                    "pip","flutu","janela","sobrepos"
            };

            HashSet<String> seen = new HashSet<>();

            for (ActivityInfo ai : info.activities) {
                if (!ai.exported || !ai.enabled) continue;

                String cls = ai.name == null ? "" : ai.name;
                String label = "";
                try {
                    CharSequence cs = ai.loadLabel(pm);
                    if (cs != null) label = cs.toString();
                } catch (Exception ignored) {}

                String hay = (cls + " " + label).toLowerCase(Locale.ROOT);
                boolean match = false;
                for (String k : keys) {
                    if (hay.contains(k)) { match = true; break; }
                }
                if (!match) continue;

                String key = a.pkg + "/" + cls;
                if (seen.contains(key)) continue;
                seen.add(key);

                String pretty = label == null || label.trim().isEmpty()
                        ? shortClassName(cls)
                        : label + " (" + shortClassName(cls) + ")";
                result.add(new Candidate(pretty, new ComponentName(a.pkg, cls)));
            }

            Collections.sort(result, (x,y) -> x.title.compareToIgnoreCase(y.title));
        } catch (Exception ignored) {}

        return result;
    }

    private void launchCandidate(Candidate c, AppItem a) {
        try {
            Intent i = new Intent();
            i.setComponent(c.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Essa tela existe, mas " + a.name + " não permitiu que fosse aberta externamente.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String shortClassName(String cls) {
        if (cls == null) return "Tela";
        int idx = cls.lastIndexOf('.');
        return idx >= 0 && idx < cls.length()-1 ? cls.substring(idx+1) : cls;
    }

    private void launchApp(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } else {
                Toast.makeText(this, "Não encontrei a tela principal desse app.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o aplicativo.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGeneralOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) {}
        }
    }

    private void openAppDetails(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir as configurações.", Toast.LENGTH_SHORT).show();
        }
    }

    private void remember(AppItem a) {
        getSharedPreferences("overlay_manager", MODE_PRIVATE)
                .edit().putString("last_pkg", a.pkg).putString("last_name", a.name).apply();
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

    private Button btn(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(12));
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
