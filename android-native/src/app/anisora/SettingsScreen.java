package app.anisora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Settings — mirrors src/components/Settings.tsx (sections + panels). */
public class SettingsScreen {

    private static final String[][] SECTIONS = {
            {"appearance", "Appearance", "palette"},
            {"content", "Content", "type"},
            {"extensions", "Extensions", "layers"},
            {"playback", "Playback", "play"},
            {"sync", "Sync", "refresh"},
            {"account", "Account", "user"},
    };

    public static View build(final Context c, final MainActivity app) {
        ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        LinearLayout col = Ui.col(c);
        col.setPadding(Ui.dp(16), Ui.dp(20), Ui.dp(16), Ui.dp(110));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(Ui.mono(c, "Control room", 10, Theme.MUT));
        col.addView(Ui.space(c, 7));
        LinearLayout h1 = Ui.row(c);
        h1.addView(Ui.text(c, "Make it ", 27, Theme.TXT, Theme.DISP_BOLD));
        h1.addView(Ui.text(c, "yours.", 27, Theme.ACC, Theme.DISP_BOLD));
        col.addView(h1);
        col.addView(Ui.space(c, 5));
        col.addView(Ui.text(c, "Every change applies instantly and persists on this device.", 12.5f, Theme.MUT, Theme.SANS));
        col.addView(Ui.space(c, 22));

        // section nav
        final String sec = app.settingsSection;
        HorizontalScrollView nav = new HorizontalScrollView(c);
        nav.setHorizontalScrollBarEnabled(false);
        LinearLayout navRow = Ui.row(c);
        navRow.setBackground(Ui.rounded(Theme.BG1, 16, Theme.LINE, 1));
        navRow.setPadding(Ui.dp(5), Ui.dp(5), Ui.dp(5), Ui.dp(5));
        for (int i = 0; i < SECTIONS.length; i++) {
            final String id = SECTIONS[i][0];
            boolean active = id.equals(sec);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(13), Ui.dp(9), Ui.dp(13), Ui.dp(9));
            item.setBackground(active ? Ui.rounded(Theme.ACC, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, SECTIONS[i][2], 14, active ? Theme.ACC_INK : Theme.MUT), Ui.lp(Ui.dp(14), Ui.dp(14)));
            item.addView(Ui.hspace(c, 8));
            item.addView(Ui.text(c, SECTIONS[i][1], 12.5f, active ? Theme.ACC_INK : Theme.MUT, Theme.SANS_SB));
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.settingsSection = id;
                    app.rebuildContent();
                }
            });
            navRow.addView(item);
        }
        nav.addView(navRow);
        col.addView(nav);
        col.addView(Ui.space(c, 18));

        if ("appearance".equals(sec)) buildAppearance(c, app, col);
        else if ("content".equals(sec)) buildContent(c, app, col);
        else if ("extensions".equals(sec)) buildExtensions(c, app, col);
        else if ("playback".equals(sec)) buildPlayback(c, app, col);
        else if ("sync".equals(sec)) buildSync(c, app, col);
        else buildAccount(c, app, col);

        return sc;
    }

    /* -------------------------------- panels -------------------------------- */

    private static LinearLayout panel(Context c, String icon, String title, String sub) {
        LinearLayout p = Ui.col(c);
        p.setBackground(Ui.rounded(Theme.BG1, 18, Theme.LINE, 1));
        p.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(8));
        LinearLayout head = Ui.row(c);
        FrameLayout ic = new FrameLayout(c);
        ic.setBackground(Ui.rounded(Theme.ACC_SOFT, 10, Theme.ACC_LINE, 1));
        Icons i = new Icons(c, icon, 15, Theme.ACC);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(15), Ui.dp(15));
        ip.gravity = Gravity.CENTER;
        ic.addView(i, ip);
        head.addView(ic, Ui.lpm(Ui.dp(30), Ui.dp(30), 0, 0, 10, 0));
        LinearLayout tcol = Ui.col(c);
        tcol.addView(Ui.text(c, title, 15.5f, Theme.TXT, Theme.DISP_BOLD));
        if (sub != null) {
            TextView s = Ui.text(c, sub, 11, Theme.MUT, Theme.SANS_MED);
            s.setPadding(0, Ui.dp(2), 0, 0);
            tcol.addView(s);
        }
        head.addView(tcol);
        p.addView(head);
        p.addView(Ui.space(c, 8));
        return p;
    }

    private static LinearLayout rowOf(Context c, String label, String sub, View control, boolean vertical) {
        LinearLayout r = vertical ? Ui.col(c) : Ui.row(c);
        r.setPadding(0, Ui.dp(12), 0, Ui.dp(12));
        LinearLayout tcol = Ui.col(c);
        tcol.addView(Ui.text(c, label, 13.5f, Theme.TXT, Theme.SANS_SB));
        if (sub != null) {
            TextView s = Ui.text(c, sub, 11, Theme.MUT, Theme.SANS);
            s.setPadding(0, Ui.dp(3), 0, 0);
            tcol.addView(s);
        }
        if (vertical) {
            r.addView(tcol);
            r.addView(Ui.space(c, 10));
            r.addView(control);
        } else {
            LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.weight = 1;
            r.addView(tcol, tp);
            r.addView(control);
        }
        return r;
    }

    private static View toggleFor(final Context c, final MainActivity app, final String key, boolean def) {
        return Widgets.toggle(c, app.store.getB(key, def), new Widgets.OnToggle() {
            public void toggled(boolean on) {
                app.store.put(key, on);
            }
        });
    }

    /* ------------------------------- appearance ------------------------------ */

    private static void buildAppearance(final Context c, final MainActivity app, LinearLayout col) {
        LinearLayout p = panel(c, "palette", "Appearance", "Skin the entire interface");

        p.addView(rowOf(c, "Theme", "Dark, pure-black AMOLED, or light",
                Widgets.seg(c, new String[][]{{"dark", "Dark"}, {"amoled", "AMOLED"}, {"light", "Light"}},
                        app.store.getS("theme", "dark"), new Widgets.OnSeg() {
                            public void pick(String id) {
                                app.store.put("theme", id);
                                app.applyTheme();
                            }
                        }), true));
        p.addView(Ui.divider(c));

        // accent swatches
        LinearLayout swatches = Ui.row(c);
        String current = app.store.getS("accent", "61 180 242");
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            final String val = Theme.ACCENTS[i][1];
            int color = Theme.parseAccent(val);
            FrameLayout sw = new FrameLayout(c);
            sw.setBackground(Ui.circle(color));
            if (val.equals(current)) {
                Icons ck = new Icons(c, "check", 14, 0xB3000000, 3.2f);
                FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
                cp.gravity = Gravity.CENTER;
                sw.addView(ck, cp);
            }
            sw.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.store.put("accent", val);
                    app.applyTheme();
                }
            });
            swatches.addView(sw, Ui.lpm(Ui.dp(34), Ui.dp(34), i == 0 ? 0 : 9, 0, 0, 0));
        }
        p.addView(rowOf(c, "Accent color", "Used for highlights, rings and sync states", swatches, true));
        p.addView(Ui.divider(c));

        p.addView(rowOf(c, "Card density", "Compact packs more posters per row",
                Widgets.seg(c, new String[][]{{"cozy", "Cozy"}, {"compact", "Compact"}},
                        app.store.getS("density", "cozy"), new Widgets.OnSeg() {
                            public void pick(String id) {
                                app.store.put("density", id);
                                app.rebuildContent();
                            }
                        }), true));
        p.addView(Ui.divider(c));

        // poster radius slider
        LinearLayout sl = Ui.row(c);
        final TextView valT = Ui.text(c, app.store.getI("posterRadius", 16) + "px", 12, Theme.TXT, Theme.MONO_BOLD);
        SeekBar sb = new SeekBar(c);
        sb.setMax(20);
        sb.setProgress(app.store.getI("posterRadius", 16) - 6);
        sb.getProgressDrawable().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.getThumb().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) {
                valT.setText((v + 6) + "px");
            }

            public void onStartTrackingTouch(SeekBar s) {
            }

            public void onStopTrackingTouch(SeekBar s) {
                app.store.put("posterRadius", s.getProgress() + 6);
                app.applyTheme();
            }
        });
        LinearLayout.LayoutParams sp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.weight = 1;
        sl.addView(sb, sp);
        sl.addView(Ui.hspace(c, 10));
        sl.addView(valT);
        p.addView(rowOf(c, "Poster corner radius", "Roundness of every cover", sl, true));
        p.addView(Ui.divider(c));

        p.addView(rowOf(c, "Reduce motion", "Minimise animations", toggleFor(c, app, "reduceMotion", false), false));

        col.addView(p);
    }

    /* -------------------------------- content -------------------------------- */

    private static void buildContent(final Context c, final MainActivity app, LinearLayout col) {
        LinearLayout p = panel(c, "type", "Content", "How titles and results behave");
        p.addView(rowOf(c, "Title language", "Renaming applies to every card instantly",
                Widgets.seg(c, new String[][]{{"romaji", "Romaji"}, {"english", "English"}, {"native", "Native"}},
                        app.store.getS("titleLang", "romaji"), new Widgets.OnSeg() {
                            public void pick(String id) {
                                app.store.put("titleLang", id);
                                app.rebuildContent();
                            }
                        }), true));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Adult content", "Include 18+ entries in search results", toggleFor(c, app, "nsfw", false), false));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Auto-update progress", "Advance your AniList entry after finishing an episode",
                toggleFor(c, app, "autoProgress", true), false));
        col.addView(p);
    }

    /* ------------------------------- extensions ------------------------------- */

    static final String[][] EXTS = {
            {"AniWatch", "EN", "1.4.2", "ANIME", "#6C5CE7", "128k"},
            {"Senpai Stream", "EN", "0.9.8", "ANIME", "#00B4D8", "86k"},
            {"Jellyfin Local", "Multi", "2.1.0", "ANIME", "#5bc0de", "12k"},
            {"MangaDex", "Multi", "3.0.1", "MANGA", "#FF6740", "215k"},
            {"Asura Scans", "EN", "1.1.6", "MANGA", "#F4A261", "74k"},
    };

    private static void buildExtensions(final Context c, final MainActivity app, LinearLayout col) {
        LinearLayout p = panel(c, "layers", "Extensions", "Streaming & reading sources for the Watch tab");
        for (int i = 0; i < EXTS.length; i++) {
            final String name = EXTS[i][0];
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(11), 0, Ui.dp(11));

            TextView av = Ui.text(c, name.substring(0, 1), 15, 0xFFFFFFFF, Theme.DISP_BOLD);
            av.setGravity(Gravity.CENTER);
            int color = 0xFF6C5CE7;
            try {
                color = (int) Long.parseLong(EXTS[i][4].substring(1), 16) | 0xFF000000;
            } catch (Exception ignored) {
            }
            av.setBackground(Ui.rounded(color, 12, 0, 0));
            row.addView(av, Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 0, 12, 0));

            LinearLayout mid = Ui.col(c);
            mid.addView(Ui.text(c, name, 13.5f, Theme.TXT, Theme.SANS_SB));
            TextView subT = Ui.text(c, EXTS[i][1] + " · v" + EXTS[i][2] + " · " + EXTS[i][5] + " installs · "
                    + ("ANIME".equals(EXTS[i][3]) ? "Anime" : "Manga"), 11, Theme.MUT, Theme.SANS);
            subT.setPadding(0, Ui.dp(3), 0, 0);
            mid.addView(subT);
            LinearLayout.LayoutParams mp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            mp.weight = 1;
            row.addView(mid, mp);

            final String key = "ext." + name;
            boolean def = !"Jellyfin Local".equals(name) && !"Asura Scans".equals(name);
            row.addView(Widgets.toggle(c, app.store.getB(key, def), new Widgets.OnToggle() {
                public void toggled(boolean on) {
                    app.store.put(key, on);
                    app.toast(name + (on ? " enabled" : " disabled"), on ? "check" : "info");
                }
            }));
            p.addView(row);
            if (i < EXTS.length - 1) p.addView(Ui.divider(c));
        }
        col.addView(p);
    }

    /* -------------------------------- playback -------------------------------- */

    private static void buildPlayback(final Context c, final MainActivity app, LinearLayout col) {
        LinearLayout p = panel(c, "play", "Playback", "Player defaults for every extension");
        p.addView(rowOf(c, "Auto-play next episode", "Continue the binge automatically",
                toggleFor(c, app, "autoNext", true), false));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Skip intro", "Jump past openings when markers exist",
                toggleFor(c, app, "skipIntro", true), false));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Preferred quality", null,
                Widgets.seg(c, new String[][]{{"1080p", "1080p"}, {"720p", "720p"}, {"480p", "480p"}},
                        app.store.getS("quality", "1080p"), new Widgets.OnSeg() {
                            public void pick(String id) {
                                app.store.put("quality", id);
                                app.rebuildContent();
                            }
                        }), true));
        col.addView(p);
    }

    /* ---------------------------------- sync ---------------------------------- */

    private static void buildSync(final Context c, final MainActivity app, LinearLayout col) {
        LinearLayout p = panel(c, "refresh", "Sync", "How Anisora talks to AniList");
        p.addView(rowOf(c, "Auto-sync changes", "Push list updates in the background",
                toggleFor(c, app, "syncAuto", true), false));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Sync on launch", "Refresh lists when the app starts",
                toggleFor(c, app, "syncOnStart", true), false));
        p.addView(Ui.divider(c));
        p.addView(rowOf(c, "Score format", null,
                Widgets.seg(c, new String[][]{{"100", "100 pt"}, {"10", "10 pt"}, {"5", "5 ★"}},
                        app.store.getS("scoreFormat", "100"), new Widgets.OnSeg() {
                            public void pick(String id) {
                                app.store.put("scoreFormat", id);
                                app.rebuildContent();
                            }
                        }), true));

        // sync now button (real re-sync when signed in)
        LinearLayout btn = Ui.row(c);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(Ui.ripple(Ui.rounded(Theme.ACC_SOFT, 12, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 60)));
        btn.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));
        btn.addView(new Icons(c, "refresh", 14, Theme.ACC), Ui.lp(Ui.dp(14), Ui.dp(14)));
        btn.addView(Ui.hspace(c, 8));
        btn.addView(Ui.text(c, Anilist.authed() ? "Sync now" : "Sync now (sign in first)", 13, Theme.ACC, Theme.SANS_SB));
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Anilist.authed()) {
                    Api.clearCache();
                    app.toast("Pulling your AniList library…", "sync");
                    Anilist.syncLibrary(app, app.store.getI("anilist.userId", 0));
                } else {
                    app.startAniListLogin();
                }
            }
        });
        p.addView(btn, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 12));
        col.addView(p);
    }

    /* --------------------------------- account -------------------------------- */

    private static void buildAccount(final Context c, final MainActivity app, LinearLayout col) {
        boolean connected = Anilist.authed();
        LinearLayout p = panel(c, "user", "Account", connected ? "anilist.co · connected" : "Guest profile");

        LinearLayout row = Ui.row(c);
        row.setPadding(0, Ui.dp(10), 0, Ui.dp(14));
        TextView av = Ui.text(c, app.store.userName().substring(0, 1).toUpperCase(), 20, Theme.ACC, Theme.DISP_BOLD);
        av.setGravity(Gravity.CENTER);
        av.setBackground(Ui.rounded(Theme.BG2, 16, Theme.LINE, 1));
        row.addView(av, Ui.lpm(Ui.dp(52), Ui.dp(52), 0, 0, 12, 0));
        LinearLayout info = Ui.col(c);
        info.addView(Ui.text(c, app.store.userName(), 15.5f, Theme.TXT, Theme.SANS_BOLD));
        TextView st = Ui.text(c, (connected ? "AniList account" : "Guest profile") + " · "
                + app.store.countInProgress() + " in progress", 11.5f, Theme.MUT, Theme.SANS);
        st.setPadding(0, Ui.dp(3), 0, 0);
        info.addView(st);
        row.addView(info);
        p.addView(row);
        p.addView(Ui.divider(c));

        if (!connected) {
            LinearLayout connect = Ui.row(c);
            connect.setGravity(Gravity.CENTER);
            connect.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 12, 0, 0), 0x33000000));
            connect.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));
            TextView al = Ui.text(c, "AL", 10, Theme.ACC_INK, Theme.SANS_BOLD);
            al.setBackground(Ui.rounded(0x40FFFFFF, 6, 0, 0));
            al.setPadding(Ui.dp(4), Ui.dp(1), Ui.dp(4), Ui.dp(1));
            connect.addView(al);
            connect.addView(Ui.hspace(c, 8));
            connect.addView(Ui.text(c, "Connect AniList account", 13, Theme.ACC_INK, Theme.SANS_BOLD));
            connect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.startAniListLogin();
                }
            });
            p.addView(connect, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 4));
        }

        LinearLayout out = Ui.row(c);
        out.setGravity(Gravity.CENTER);
        out.setBackground(Ui.ripple(Ui.rounded(0x1AFB7185, 12, 0x4DFB7185, 1), 0x33FB7185));
        out.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));
        out.addView(new Icons(c, "logout", 14, Theme.ROSE), Ui.lp(Ui.dp(14), Ui.dp(14)));
        out.addView(Ui.hspace(c, 8));
        out.addView(Ui.text(c, "Log out", 13, Theme.ROSE, Theme.SANS_SB));
        out.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Anilist.logout(app);
                app.store.logout();
                app.toast("Signed out — see you soon", "info");
                app.rebuild();
            }
        });
        p.addView(out, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 12));
        col.addView(p);
    }
}
