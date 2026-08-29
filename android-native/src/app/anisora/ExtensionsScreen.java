package app.anisora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Aniyomi-compatible extension manager.
 *
 * Repos are standard `index.min.json` indexes (same format Aniyomi consumes);
 * installing downloads the real extension APK into filesDir/extensions/ —
 * the same "private install" layout Aniyomi uses before class-loading.
 */
public class ExtensionsScreen {

    public static void open(final Context c, final MainActivity app) {
        final FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Theme.BG0);
        root.setClickable(true);

        final LinearLayout page = Ui.col(c);

        /* ---------- top bar ---------- */
        LinearLayout bar = Ui.row(c);
        bar.setPadding(Ui.dp(12), Ui.dp(12), Ui.dp(12), Ui.dp(10));
        FrameLayout back = new FrameLayout(c);
        back.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        Icons bi = new Icons(c, "arrow-left", 16, Theme.TXT);
        FrameLayout.LayoutParams bip = new FrameLayout.LayoutParams(Ui.dp(16), Ui.dp(16));
        bip.gravity = Gravity.CENTER;
        back.addView(bi, bip);
        bar.addView(back, Ui.lpm(Ui.dp(38), Ui.dp(38), 0, 0, 12, 0));
        LinearLayout tcol = Ui.col(c);
        tcol.addView(Ui.text(c, "Extensions", 18, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, app.store.repos().length() + " repos · Aniyomi-compatible", 11, Theme.MUT, Theme.SANS_MED);
        sub.setPadding(0, Ui.dp(2), 0, 0);
        tcol.addView(sub);
        bar.addView(tcol);
        page.addView(bar);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.extScreenRefresh = null;
                ViewGroup p = (ViewGroup) root.getParent();
                if (p != null) p.removeView(root);
            }
        });

        /* ---------- tabs + content ---------- */
        final LinearLayout tabsWrap = Ui.col(c);
        tabsWrap.setPadding(Ui.dp(16), Ui.dp(4), Ui.dp(16), Ui.dp(10));
        page.addView(tabsWrap);

        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout content = Ui.col(c);
        content.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(40));
        sc.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scp = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        scp.weight = 1;
        page.addView(sc, scp);

        final String[] tab = {"ANIME"};
        final Runnable[] render = new Runnable[1];
        render[0] = new Runnable() {
            public void run() {
                tabsWrap.removeAllViews();
                tabsWrap.addView(Widgets.seg(c, new String[][]{{"ANIME", "Anime"}, {"MANGA", "Manga"}}, tab[0],
                        new Widgets.OnSeg() {
                            public void pick(String id) {
                                tab[0] = id;
                                render[0].run();
                            }
                        }));
                renderContent(c, app, content, tab[0], render[0]);
            }
        };
        render[0].run();

        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // let MainActivity.onResume refresh this screen after install/uninstall popups
        app.extScreenRefresh = render[0];
        app.overlayRoot().addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!Theme.REDUCE_MOTION) {
            root.setTranslationY(Ui.dp(24));
            root.setAlpha(0f);
            root.animate().translationY(0).alpha(1f).setDuration(220).start();
        }
    }

    /* --------------------------------- content --------------------------------- */

    private static void renderContent(final Context c, final MainActivity app, final LinearLayout box,
                                      final String kind, final Runnable rerender) {
        box.removeAllViews();

        /* installed section: system packages (Aniyomi-style scan) + private registry */
        JSONObject reg = app.store.installedExts();
        List<JSONObject> installed = app.store.systemExts(kind);
        java.util.HashSet<String> sysPkgs = new java.util.HashSet<String>();
        for (int i = 0; i < installed.size(); i++) {
            String pkg = installed.get(i).optString("pkg");
            sysPkgs.add(pkg);
            // enrich scanned entries with repo metadata (icon, sources) if we downloaded it
            JSONObject meta = reg.optJSONObject(pkg);
            if (meta != null) {
                try {
                    if (meta.has("iconUrl")) installed.get(i).put("iconUrl", meta.optString("iconUrl"));
                    if (meta.has("sources")) installed.get(i).put("sources", meta.optJSONArray("sources"));
                } catch (Exception ignored) {
                }
            }
        }
        JSONArray names = reg.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                JSONObject e = reg.optJSONObject(names.optString(i));
                if (e == null || !kind.equals(e.optString("kind"))) continue;
                if (sysPkgs.contains(e.optString("pkg"))) continue;
                if (e.optBoolean("system", false)) {
                    try {
                        e.put("pending", true); // downloaded, but user hasn't finished the popup
                    } catch (Exception ignored) {
                    }
                }
                installed.add(e);
            }
        }
        Collections.sort(installed, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                return a.optString("name").compareToIgnoreCase(b.optString("name"));
            }
        });
        if (!installed.isEmpty()) {
            box.addView(Widgets.sectionHead(c, "check", "Installed", installed.size() + " on this device"));
            for (int i = 0; i < installed.size(); i++) {
                box.addView(extRow(c, app, installed.get(i), null, true, rerender),
                        Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 8, 0, 0));
            }
            box.addView(Ui.space(c, 24));
        }

        /* available from repos */
        JSONArray repos = app.store.repos();
        if (repos.length() == 0) {
            box.addView(Widgets.emptyState(c, "layers", "No extension repos",
                    "Add an Aniyomi repo (index.min.json URL) in Settings → Extensions."));
            return;
        }
        box.addView(Widgets.sectionHead(c, "download", "Available",
                "From " + repos.length() + " Aniyomi repo" + (repos.length() == 1 ? "" : "s")));
        final LinearLayout avail = Ui.col(c);
        box.addView(avail);
        for (int j = 0; j < 4; j++)
            avail.addView(Widgets.skel(c, 14), Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(60), 0, j == 0 ? 0 : 8, 0, 0));

        final List<JSONObject> found = new ArrayList<JSONObject>();
        final int[] pending = {repos.length()};
        for (int i = 0; i < repos.length(); i++) {
            final String repoUrl = repos.optString(i);
            Net.getText(repoUrl, new Net.Text() {
                public void ok(String body) {
                    try {
                        JSONArray idx = new JSONArray(body);
                        String base = repoUrl.substring(0, repoUrl.lastIndexOf('/') + 1);
                        for (int k = 0; k < idx.length(); k++) {
                            JSONObject e = idx.optJSONObject(k);
                            if (e == null) continue;
                            String pkg = e.optString("pkg", "");
                            String k2 = pkg.contains(".animeextension.") ? "ANIME" : "MANGA";
                            if (!kind.equals(k2)) continue;
                            e.put("_repo", base);
                            found.add(e);
                        }
                    } catch (Exception ignored) {
                    }
                    if (--pending[0] == 0) renderAvailable(c, app, avail, found, rerender);
                }

                public void fail(Exception ex) {
                    if (--pending[0] == 0) renderAvailable(c, app, avail, found, rerender);
                }
            });
        }
    }

    private static void renderAvailable(Context c, MainActivity app, LinearLayout avail,
                                        List<JSONObject> found, Runnable rerender) {
        if (!avail.isAttachedToWindow()) return;
        avail.removeAllViews();
        if (found.isEmpty()) {
            avail.addView(Widgets.emptyState(c, "cloud-off", "Couldn't reach any repo",
                    "Check your connection, or verify the repo URLs in Settings → Extensions."));
            return;
        }
        // en/all languages first, then by name; dedupe by pkg (first repo wins)
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        JSONObject reg = app.store.installedExts();
        java.util.HashSet<String> sysPkgs = new java.util.HashSet<String>();
        List<JSONObject> sys = app.store.systemExts(null);
        for (int i = 0; i < sys.size(); i++) sysPkgs.add(sys.get(i).optString("pkg"));
        Collections.sort(found, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                int pa = rank(a.optString("lang")), pb = rank(b.optString("lang"));
                if (pa != pb) return pa - pb;
                return a.optString("name").compareToIgnoreCase(b.optString("name"));
            }

            private int rank(String l) {
                return "all".equals(l) ? 0 : "en".equals(l) ? 1 : 2;
            }
        });
        int shown = 0;
        for (int i = 0; i < found.size() && shown < 150; i++) {
            JSONObject e = found.get(i);
            String pkg = e.optString("pkg");
            if (!seen.add(pkg) || reg.has(pkg) || sysPkgs.contains(pkg)) continue;
            avail.addView(extRow(c, app, e, e.optString("_repo"), false, rerender),
                    Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, shown == 0 ? 0 : 8, 0, 0));
            shown++;
        }
        if (found.size() > shown) {
            TextView more = Ui.text(c, "+ " + (found.size() - shown) + " more in these repos", 11.5f, Theme.MUT, Theme.SANS_MED);
            more.setGravity(Gravity.CENTER);
            more.setPadding(0, Ui.dp(14), 0, 0);
            avail.addView(more, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /* ---------------------------------- rows ---------------------------------- */

    private static View extRow(final Context c, final MainActivity app, final JSONObject e,
                               final String repoBase, final boolean isInstalled, final Runnable rerender) {
        LinearLayout row = Ui.row(c);
        row.setBackground(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1));
        row.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));

        String name = e.optString("name", "?").replace("Aniyomi: ", "").replace("Tachiyomi: ", "");
        String pkg = e.optString("pkg", "");

        // icon from repo
        FrameLayout ic = new FrameLayout(c);
        ic.setBackground(Ui.rounded(Theme.BG2, 11, Theme.LINE, 1));
        Widgets.clipRounded(ic, 11);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ic.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String iconUrl = isInstalled ? e.optString("iconUrl", null) : repoBase + "icon/" + pkg + ".png";
        if (iconUrl != null && iconUrl.length() > 0) Images.load(iconUrl, iv, Ui.dp(42));
        row.addView(ic, Ui.lpm(Ui.dp(42), Ui.dp(42), 0, 0, 11, 0));

        LinearLayout mid = Ui.col(c);
        LinearLayout nameRow = Ui.row(c);
        nameRow.addView(Ui.oneLine(Ui.text(c, name, 13.5f, Theme.TXT, Theme.SANS_SB)));
        if (e.optInt("nsfw", 0) == 1) {
            TextView nb = Ui.text(c, "18+", 8.5f, Theme.ROSE, Theme.MONO_BOLD);
            nb.setBackground(Ui.rounded(0x26FB7185, 5, 0, 0));
            nb.setPadding(Ui.dp(4), Ui.dp(1), Ui.dp(4), Ui.dp(1));
            nameRow.addView(nb, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 6, 0, 0, 0));
        }
        mid.addView(nameRow);
        int srcCount = e.optJSONArray("sources") != null ? e.optJSONArray("sources").length() : 0;
        TextView meta = Ui.oneLine(Ui.text(c, "v" + e.optString("version") + " · " + e.optString("lang")
                + (srcCount > 0 ? " · " + srcCount + " source" + (srcCount == 1 ? "" : "s") : ""), 11, Theme.MUT, Theme.SANS));
        meta.setPadding(0, Ui.dp(3), 0, 0);
        mid.addView(meta);
        LinearLayout.LayoutParams mp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.weight = 1;
        row.addView(mid, mp);

        if (isInstalled) {
            final boolean pending = e.optBoolean("pending", false);
            final boolean system = e.optBoolean("system", false);
            if (pending) {
                LinearLayout fin = Ui.row(c);
                fin.setGravity(Gravity.CENTER);
                fin.setBackground(Ui.ripple(Ui.rounded(Theme.ACC_SOFT, 11, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 60)));
                fin.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8));
                fin.addView(Ui.text(c, "Finish install", 12, Theme.ACC, Theme.SANS_BOLD));
                fin.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        launchInstaller(app, e.optString("pkg"));
                    }
                });
                row.addView(fin);
            }
            FrameLayout un = new FrameLayout(c);
            un.setBackground(Ui.ripple(Ui.rounded(0x1AFB7185, 11, 0x4DFB7185, 1), 0x33FB7185));
            Icons xi = new Icons(c, "x", 14, Theme.ROSE);
            FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
            xp.gravity = Gravity.CENTER;
            un.addView(xi, xp);
            un.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (system && !pending) {
                        // real app package: hand off to Android's uninstaller popup
                        try {
                            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_DELETE,
                                    android.net.Uri.parse("package:" + e.optString("pkg")));
                            app.startActivity(i);
                            app.toast("Confirm the uninstall, then come back", "info");
                        } catch (Exception ex) {
                            app.toast("Couldn't open the uninstaller", "info");
                        }
                    } else {
                        app.store.uninstallExt(e.optString("pkg"));
                        app.toast(e.optString("name") + " removed", "trash");
                        rerender.run();
                    }
                }
            });
            row.addView(un, Ui.lpm(Ui.dp(36), Ui.dp(36), pending ? 8 : 0, 0, 0, 0));
        } else {
            final LinearLayout inst = Ui.row(c);
            inst.setGravity(Gravity.CENTER);
            inst.setBackground(Ui.ripple(Ui.rounded(Theme.ACC_SOFT, 11, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 60)));
            inst.setPadding(Ui.dp(12), Ui.dp(8), Ui.dp(12), Ui.dp(8));
            inst.addView(new Icons(c, "download", 13, Theme.ACC), Ui.lp(Ui.dp(13), Ui.dp(13)));
            inst.addView(Ui.hspace(c, 6));
            final TextView it = Ui.text(c, "Install", 12, Theme.ACC, Theme.SANS_BOLD);
            inst.addView(it);
            final String fName = name;
            inst.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    it.setText("…");
                    inst.setEnabled(false);
                    String apkUrl = repoBase + "apk/" + e.optString("apk");
                    File out = new File(new File(c.getFilesDir(), "extensions"), e.optString("pkg") + ".apk");
                    final boolean systemInstall = "system".equals(app.store.getS("extInstaller", "system"));
                    Net.download(apkUrl, out, new Net.FileCb() {
                        public void ok(File f) {
                            try {
                                JSONObject meta = new JSONObject();
                                meta.put("pkg", e.optString("pkg"));
                                meta.put("name", fName);
                                meta.put("version", e.optString("version"));
                                meta.put("lang", e.optString("lang"));
                                meta.put("nsfw", e.optInt("nsfw", 0));
                                meta.put("kind", e.optString("pkg").contains(".animeextension.") ? "ANIME" : "MANGA");
                                meta.put("apkPath", f.getAbsolutePath());
                                meta.put("apkSize", f.length());
                                meta.put("iconUrl", repoBase + "icon/" + e.optString("pkg") + ".png");
                                if (e.optJSONArray("sources") != null) meta.put("sources", e.optJSONArray("sources"));
                                meta.put("system", systemInstall);
                                app.store.installExt(meta);
                            } catch (Exception ignored) {
                            }
                            try { ExtBridge.reload(); } catch (Throwable ignored) {}
                            if (systemInstall) {
                                // Aniyomi "Legacy" installer: Android shows the install popup
                                launchInstaller(app, e.optString("pkg"));
                                app.toast("Confirm the install in Android's popup", "info");
                            } else {
                                app.toast(fName + " v" + e.optString("version") + " installed privately ("
                                        + (f.length() / 1024) + " KB)", "check");
                            }
                            rerender.run();
                        }

                        public void fail(Exception ex) {
                            it.setText("Install");
                            inst.setEnabled(true);
                            app.toast("Download failed — " + ex.getMessage(), "info");
                        }
                    });
                }
            });
            row.addView(inst);
        }
        return row;
    }

    /** Hand the downloaded APK to Android's package installer (content:// via ApkProvider). */
    static void launchInstaller(MainActivity app, String pkg) {
        try {
            android.net.Uri uri = android.net.Uri.parse("content://app.anisora.apkprovider/" + pkg + ".apk");
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            app.startActivity(i);
        } catch (Exception ex) {
            app.toast("Couldn't open the package installer", "info");
        }
    }

    /* ------------------------------ add-repo sheet ------------------------------ */

    public static void showAddRepoSheet(final MainActivity app, final Runnable onChanged) {
        final Context c = app;
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(18), Ui.dp(18), Ui.dp(18), Ui.dp(18));

        sheet.addView(Ui.text(c, "Add extension repo", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, "Paste an Aniyomi-compatible index.min.json URL", 11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(3), 0, Ui.dp(12));
        sheet.addView(sub);

        final EditText input = new EditText(c);
        input.setHint("https://…/index.min.json");
        input.setHintTextColor(Theme.alpha(Theme.MUT, 150));
        input.setTextColor(Theme.TXT);
        input.setTextSize(12.5f);
        input.setTypeface(Theme.MONO_MED);
        input.setSingleLine(true);
        input.setBackground(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1));
        input.setPadding(Ui.dp(12), Ui.dp(11), Ui.dp(12), Ui.dp(11));
        sheet.addView(input, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sheet.addView(Ui.space(c, 12));
        TextView sg = Ui.text(c, "Popular repos", 11, Theme.MUT, Theme.SANS_SB);
        sheet.addView(sg);
        sheet.addView(Ui.space(c, 7));
        String[] labels = {"Kohi-den (anime, official)", "Keiyoushi (manga)", "almightyhak (anime)", "yuzono (anime)"};
        for (int i = 0; i < Store.SUGGESTED_REPOS.length; i++) {
            final String url = Store.SUGGESTED_REPOS[i];
            LinearLayout s = Ui.row(c);
            s.setBackground(Ui.ripple(Ui.rounded(Theme.BG2, 10, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            s.setPadding(Ui.dp(11), Ui.dp(8), Ui.dp(11), Ui.dp(8));
            s.addView(new Icons(c, "layers", 12, Theme.ACC), Ui.lp(Ui.dp(12), Ui.dp(12)));
            s.addView(Ui.hspace(c, 8));
            s.addView(Ui.oneLine(Ui.text(c, labels[i], 11.5f, Theme.TXT, Theme.SANS_SB)));
            s.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    input.setText(url);
                }
            });
            sheet.addView(s, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 6, 0, 0));
        }

        sheet.addView(Ui.space(c, 14));
        LinearLayout save = Ui.row(c);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 12, 0, 0), 0x33000000));
        save.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
        save.addView(Ui.text(c, "Add repo", 13.5f, Theme.ACC_INK, Theme.SANS_BOLD));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String url = input.getText().toString().trim();
                if (url.length() < 10 || !url.startsWith("http")) {
                    app.toast("That doesn't look like a repo URL", "info");
                    return;
                }
                app.store.addRepo(url);
                app.toast("Repo added — pull it up in Browse extensions", "check");
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
                if (onChanged != null) onChanged.run();
            }
        });
        sheet.addView(save, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(20));
        overlay.addView(sheet, shp);
        if (!Theme.REDUCE_MOTION) {
            sheet.setTranslationY(Ui.dp(30));
            sheet.animate().translationY(0).setDuration(200).start();
        }
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
            }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
