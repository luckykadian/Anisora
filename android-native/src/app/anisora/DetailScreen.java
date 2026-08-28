package app.anisora;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Media detail overlay — mirrors src/components/Detail.tsx + InfoTab + WatchTab. */
public class DetailScreen {

    static final String[][] STATUS_META = {
            {"CURRENT", "Set as watching", "play"},
            {"COMPLETED", "Completed", "check"},
            {"PLANNING", "Plan to watch", "bookmark"},
            {"PAUSED", "Paused", "pause"},
            {"DROPPED", "Dropped", "x"},
            {"REPEATING", "Rewatching", "rotate"},
    };

    public static View build(final Context c, final MainActivity app, final int id, final JSONObject seed) {
        final FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Theme.BG0);
        root.setClickable(true);

        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout col = Ui.col(c);
        col.setPadding(0, 0, 0, Ui.dp(60));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sc, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // floating back button
        final FrameLayout back = new FrameLayout(c);
        back.setBackground(Ui.ripple(Ui.rounded(0xB3000000, 12, 0x26FFFFFF, 1), 0x33FFFFFF));
        Icons bi = new Icons(c, "arrow-left", 17, 0xFFFFFFFF);
        FrameLayout.LayoutParams bip = new FrameLayout.LayoutParams(Ui.dp(17), Ui.dp(17));
        bip.gravity = Gravity.CENTER;
        back.addView(bi, bip);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(Ui.dp(40), Ui.dp(40));
        bp.setMargins(Ui.dp(14), Ui.dp(14), 0, 0);
        root.addView(back, bp);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.closeDetail();
            }
        });

        // skeleton header while loading
        buildSkeleton(c, col, seed);

        Api.fetchDetail(id, new Api.Cb() {
            public void ok(JSONObject data) {
                JSONObject d = data.optJSONObject("Media");
                if (d == null || !root.isAttachedToWindow()) return;
                col.removeAllViews();
                buildLoaded(c, app, col, d);
                root.removeView(back);
                root.addView(back);
            }

            public void fail(Exception e) {
                if (!root.isAttachedToWindow()) return;
                col.removeAllViews();
                LinearLayout wrap = Ui.col(c);
                wrap.setPadding(Ui.dp(16), Ui.dp(90), Ui.dp(16), 0);
                wrap.addView(Widgets.emptyState(c, "cloud-off", "Couldn't load this title",
                        "Check your connection and try again."));
                col.addView(wrap);
            }
        });

        return root;
    }

    private static void buildSkeleton(Context c, LinearLayout col, JSONObject seed) {
        View banner = Widgets.skel(c, 0);
        col.addView(banner, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(170)));
        LinearLayout pad = Ui.col(c);
        pad.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), 0);
        View t1 = Widgets.skel(c, 6);
        pad.addView(t1, Ui.lp(Ui.dp(220), Ui.dp(22)));
        View t2 = Widgets.skel(c, 6);
        pad.addView(t2, Ui.lpm(Ui.dp(150), Ui.dp(13), 0, 10, 0, 0));
        col.addView(pad);
    }

    private static void buildLoaded(final Context c, final MainActivity app, LinearLayout col, final JSONObject d) {
        final String titleLang = app.store.getS("titleLang", "romaji");
        final String title = Api.titleOf(d, titleLang);
        final boolean isAnime = "ANIME".equals(d.optString("type"));
        JSONObject cover = d.optJSONObject("coverImage");
        String color = cover != null ? cover.optString("color", null) : null;

        /* ---------- banner ---------- */
        FrameLayout bannerBox = new FrameLayout(c);
        ImageView banner = new ImageView(c);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.setBackground(Widgets.posterFallback("null".equals(color) ? null : color, title));
        bannerBox.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String bUrl = d.optString("bannerImage", null);
        if (bUrl != null && !"null".equals(bUrl)) Images.load(bUrl, banner, 900);
        View scrim = new View(c);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Theme.BG0, Theme.alpha(Theme.BG0, 140), 0x33000000}));
        bannerBox.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        col.addView(bannerBox, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(190)));

        /* ---------- header: cover + title ---------- */
        LinearLayout head = Ui.row(c);
        head.setGravity(Gravity.BOTTOM);
        head.setPadding(Ui.dp(16), 0, Ui.dp(16), 0);
        ((ViewGroup.MarginLayoutParams) newLp(head, col)).topMargin = Ui.dp(-56);

        FrameLayout coverBox = new FrameLayout(c);
        coverBox.setBackground(Ui.rounded(Theme.BG2, Theme.RADIUS, Theme.LINE, 1));
        Widgets.clipRounded(coverBox, Theme.RADIUS);
        ImageView cv = new ImageView(c);
        cv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cv.setBackground(Widgets.posterFallback("null".equals(color) ? null : color, title));
        coverBox.addView(cv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String cUrl = cover != null ? cover.optString("large", null) : null;
        if (cUrl != null && !"null".equals(cUrl)) Images.load(cUrl, cv, Ui.dp(110));
        head.addView(coverBox, Ui.lp(Ui.dp(104), Ui.dp(156)));

        LinearLayout tcol = Ui.col(c);
        tcol.setPadding(Ui.dp(14), 0, 0, Ui.dp(4));
        TextView tt = Ui.text(c, title, 19, Theme.TXT, Theme.DISP_BOLD);
        tt.setMaxLines(3);
        tcol.addView(tt);
        // meta chips row
        StringBuilder meta = new StringBuilder();
        String fl = Api.formatLabel(d.optString("format", null));
        if (fl != null && !"null".equals(fl)) meta.append(fl);
        if (!d.isNull("season") && d.optInt("seasonYear", 0) > 0) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(Api.seasonLabel(d.optString("season"))).append(" ").append(d.optInt("seasonYear"));
        }
        String sl = Api.statusLabel(d.optString("status", null));
        if (sl != null && !"null".equals(sl)) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(sl);
        }
        TextView mt = Ui.text(c, meta.toString(), 11.5f, Theme.MUT, Theme.SANS_MED);
        mt.setPadding(0, Ui.dp(6), 0, 0);
        tcol.addView(mt);
        // score + popularity pills
        LinearLayout pills = Ui.row(c);
        pills.setPadding(0, Ui.dp(8), 0, 0);
        int score = d.optInt("averageScore", 0);
        if (score > 0) {
            LinearLayout sp = Ui.row(c);
            sp.setBackground(Ui.rounded(Theme.ACC_SOFT, 999, Theme.ACC_LINE, 1));
            sp.setPadding(Ui.dp(9), Ui.dp(4), Ui.dp(10), Ui.dp(4));
            sp.addView(new Icons(c, "star", 10, Theme.STAR), Ui.lp(Ui.dp(10), Ui.dp(10)));
            sp.addView(Ui.hspace(c, 5));
            sp.addView(Ui.text(c, score + "%", 11, Theme.TXT, Theme.MONO_BOLD));
            pills.addView(sp);
            pills.addView(Ui.hspace(c, 8));
        }
        long popu = d.optLong("popularity", 0);
        if (popu > 0) {
            LinearLayout pp = Ui.row(c);
            pp.setBackground(Ui.rounded(Theme.BG1, 999, Theme.LINE, 1));
            pp.setPadding(Ui.dp(9), Ui.dp(4), Ui.dp(10), Ui.dp(4));
            pp.addView(new Icons(c, "heart", 10, Theme.ROSE), Ui.lp(Ui.dp(10), Ui.dp(10)));
            pp.addView(Ui.hspace(c, 5));
            pp.addView(Ui.text(c, Api.fmt(popu), 11, Theme.MUT, Theme.MONO_BOLD));
            pills.addView(pp);
        }
        tcol.addView(pills);
        head.addView(tcol, weight1());
        col.addView(head);

        /* ---------- airing countdown ---------- */
        JSONObject nae = d.optJSONObject("nextAiringEpisode");
        if (nae != null) {
            LinearLayout air = Ui.row(c);
            air.setBackground(Ui.rounded(Theme.ACC_SOFT, 12, Theme.ACC_LINE, 1));
            air.setPadding(Ui.dp(13), Ui.dp(9), Ui.dp(13), Ui.dp(9));
            air.addView(new Icons(c, "clock", 13, Theme.ACC), Ui.lp(Ui.dp(13), Ui.dp(13)));
            air.addView(Ui.hspace(c, 8));
            air.addView(Ui.text(c, "Ep " + nae.optInt("episode") + " airs in "
                    + Api.fmtCountdown(nae.optLong("timeUntilAiring")), 12, Theme.ACC, Theme.SANS_SB));
            LinearLayout wrap = Ui.col(c);
            wrap.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), 0);
            wrap.addView(air);
            col.addView(wrap);
        }

        /* ---------- tracking actions ---------- */
        final LinearLayout actions = Ui.col(c);
        actions.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), 0);
        col.addView(actions);
        renderActions(c, app, actions, d, title);

        /* ---------- tabs: info | watch/read (seg re-rendered so the pill follows) ---------- */
        final LinearLayout tabBody = Ui.col(c);
        final LinearLayout tabsWrap = Ui.col(c);
        tabsWrap.setPadding(Ui.dp(16), Ui.dp(18), Ui.dp(16), 0);
        final String[] tab = {"info"};
        final Runnable[] render = new Runnable[1];
        final String watchLabel = isAnime ? "Watch" : "Read";
        render[0] = new Runnable() {
            public void run() {
                tabsWrap.removeAllViews();
                tabsWrap.addView(Widgets.seg(c, new String[][]{{"info", "Info"}, {"play", watchLabel}}, tab[0],
                        new Widgets.OnSeg() {
                            public void pick(String idd) {
                                tab[0] = idd;
                                render[0].run();
                            }
                        }));
                tabBody.removeAllViews();
                if ("info".equals(tab[0])) buildInfo(c, app, tabBody, d);
                else buildWatch(c, app, tabBody, d);
            }
        };
        col.addView(tabsWrap);
        col.addView(tabBody);
        render[0].run();
    }

    /* ------------------------------ actions row ------------------------------ */

    private static void renderActions(final Context c, final MainActivity app, final LinearLayout box,
                                      final JSONObject d, final String title) {
        box.removeAllViews();
        final JSONObject e = app.store.entry(d.optInt("id"));
        final boolean isAnime = "ANIME".equals(d.optString("type"));

        LinearLayout row = Ui.row(c);

        // status button
        LinearLayout statusBtn = Ui.row(c);
        statusBtn.setGravity(Gravity.CENTER);
        boolean tracked = e != null;
        statusBtn.setBackground(Ui.ripple(
                tracked ? Ui.rounded(Theme.ACC_SOFT, 14, Theme.ACC_LINE, 1) : Ui.rounded(Theme.ACC, 14, 0, 0),
                0x33000000));
        statusBtn.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
        int fg = tracked ? Theme.ACC : Theme.ACC_INK;
        String icon = tracked ? iconFor(e.optString("status")) : "plus";
        String label = tracked ? Api.statusShort(e.optString("status")) : (isAnime ? "Add to list" : "Add to list");
        statusBtn.addView(new Icons(c, icon, 15, fg), Ui.lp(Ui.dp(15), Ui.dp(15)));
        statusBtn.addView(Ui.hspace(c, 8));
        statusBtn.addView(Ui.text(c, label, 13.5f, fg, Theme.SANS_BOLD));
        statusBtn.addView(Ui.hspace(c, 6));
        statusBtn.addView(new Icons(c, "chev-down", 13, fg), Ui.lp(Ui.dp(13), Ui.dp(13)));
        statusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showStatusSheet(c, app, d, title, box);
            }
        });
        LinearLayout.LayoutParams sp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.weight = 1;
        row.addView(statusBtn, sp);

        // +1 button
        if (e != null && !"COMPLETED".equals(e.optString("status"))) {
            LinearLayout plus = Ui.row(c);
            plus.setGravity(Gravity.CENTER);
            plus.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            plus.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            plus.addView(new Icons(c, "plus", 14, Theme.TXT, 2.4f), Ui.lp(Ui.dp(14), Ui.dp(14)));
            plus.addView(Ui.hspace(c, 6));
            int total = e.optInt("total", -1);
            plus.addView(Ui.text(c, e.optInt("progress") + " / " + (total > 0 ? String.valueOf(total) : "?"),
                    12.5f, Theme.TXT, Theme.MONO_BOLD));
            Ui.press(plus);
            plus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // annotated request: open a popup to manage watched episodes (AniList-synced)
                    showProgressSheet(c, app, d, title, box);
                }
            });
            row.addView(plus, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 10, 0, 0, 0));
        }

        // rate button
        if (e != null) {
            LinearLayout rate = Ui.row(c);
            rate.setGravity(Gravity.CENTER);
            rate.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            rate.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            int myScore = e.optInt("score", 0);
            rate.addView(new Icons(c, "star", 14, myScore > 0 ? Theme.STAR : Theme.MUT), Ui.lp(Ui.dp(14), Ui.dp(14)));
            rate.addView(Ui.hspace(c, 6));
            rate.addView(Ui.text(c, myScore > 0 ? scoreLabel(app, myScore) : "Rate",
                    12.5f, myScore > 0 ? Theme.TXT : Theme.MUT, Theme.MONO_BOLD));
            Ui.press(rate);
            rate.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showScoreSheet(c, app, d, title, box);
                }
            });
            row.addView(rate, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 10, 0, 0, 0));
        }
        box.addView(row);
    }

    /** Watched-episodes manager popup (annotated request) — slider + steppers, AniList-synced. */
    private static void showProgressSheet(final Context c, final MainActivity app, final JSONObject d,
                                          final String title, final LinearLayout actionsBox) {
        final JSONObject e = app.store.entry(d.optInt("id"));
        if (e == null) return;
        final boolean isAnime = "ANIME".equals(d.optString("type"));
        int total0 = e.optInt("total", -1);
        if (total0 <= 0) total0 = isAnime ? d.optInt("episodes", -1) : d.optInt("chapters", -1);
        JSONObject nae = d.optJSONObject("nextAiringEpisode");
        int aired = nae != null ? nae.optInt("episode", 1) - 1 : 0;
        final int max = total0 > 0 ? total0 : Math.max(Math.max(aired, e.optInt("progress") + 24), 24);
        final boolean capped = total0 > 0;

        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);

        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(18), Ui.dp(18), Ui.dp(18), Ui.dp(18));

        sheet.addView(Ui.text(c, isAnime ? "Watched episodes" : "Read chapters", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, Anilist.authed() ? "Saves straight to your AniList entry" : "Stored on this device (guest)",
                11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(3), 0, 0);
        sheet.addView(sub);
        sheet.addView(Ui.space(c, 18));

        final int[] val = {e.optInt("progress", 0)};
        final TextView big = Ui.text(c, val[0] + " / " + (capped ? String.valueOf(max) : "?"), 26, Theme.ACC, Theme.MONO_BOLD);

        LinearLayout stepper = Ui.row(c);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        final android.widget.SeekBar sb = new android.widget.SeekBar(c);
        sb.setMax(max);
        sb.setProgress(val[0]);
        sb.getProgressDrawable().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.getThumb().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int v, boolean u) {
                val[0] = v;
                big.setText(v + " / " + (capped ? String.valueOf(max) : "?"));
            }

            public void onStartTrackingTouch(android.widget.SeekBar s) {
            }

            public void onStopTrackingTouch(android.widget.SeekBar s) {
            }
        });

        FrameLayout minus = stepBtn(c, "x", false);
        ((Icons) ((FrameLayout) minus).getChildAt(0)).setIcon("chev-left");
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (val[0] > 0) sb.setProgress(val[0] - 1);
            }
        });
        FrameLayout plus1 = stepBtn(c, "plus", true);
        plus1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (val[0] < max) sb.setProgress(val[0] + 1);
            }
        });

        stepper.addView(minus, Ui.lp(Ui.dp(38), Ui.dp(38)));
        LinearLayout.LayoutParams sp2 = Ui.lpm(0, ViewGroup.LayoutParams.WRAP_CONTENT, 10, 0, 10, 0);
        sp2.weight = 1;
        stepper.addView(sb, sp2);
        stepper.addView(plus1, Ui.lp(Ui.dp(38), Ui.dp(38)));
        sheet.addView(stepper);

        LinearLayout bigRow = Ui.row(c);
        bigRow.setGravity(Gravity.CENTER);
        bigRow.addView(big);
        sheet.addView(bigRow, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 10, 0, 0));
        sheet.addView(Ui.space(c, 16));

        LinearLayout save = Ui.row(c);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 12, 0, 0), 0x33000000));
        save.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
        save.addView(new Icons(c, "check", 14, Theme.ACC_INK, 2.6f), Ui.lp(Ui.dp(14), Ui.dp(14)));
        save.addView(Ui.hspace(c, 8));
        save.addView(Ui.text(c, "Save progress", 13.5f, Theme.ACC_INK, Theme.SANS_BOLD));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    e.put("progress", val[0]);
                    String st = e.optString("status");
                    if (capped && val[0] >= max) e.put("status", "COMPLETED");
                    else if (val[0] > 0 && ("PLANNING".equals(st) || "PAUSED".equals(st) || "COMPLETED".equals(st)))
                        e.put("status", "CURRENT");
                } catch (Exception ignored) {
                }
                app.store.upsert(e);
                Anilist.push(app, e);
                app.toast((isAnime ? "Progress set to Ep. " : "Progress set to Ch. ") + val[0]
                        + (Anilist.authed() ? " · synced to AniList" : ""), "check");
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
                renderActions(c, app, actionsBox, d, title);
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

    private static FrameLayout stepBtn(Context c, String icon, boolean accent) {
        FrameLayout b = new FrameLayout(c);
        b.setBackground(Ui.ripple(Ui.rounded(accent ? Theme.ACC_SOFT : Theme.BG2, 12,
                accent ? Theme.ACC_LINE : Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        Icons i = new Icons(c, icon, 15, accent ? Theme.ACC : Theme.MUT, 2.4f);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(15), Ui.dp(15));
        ip.gravity = Gravity.CENTER;
        b.addView(i, ip);
        return b;
    }

    /** Format a 0-100 raw score in the user's chosen score format. */
    static String scoreLabel(MainActivity app, int raw) {
        String f = app.store.getS("scoreFormat", "100");
        if ("10".equals(f)) {
            String s = String.valueOf(Math.round(raw / 10.0 * 10) / 10.0);
            return (s.endsWith(".0") ? s.substring(0, s.length() - 2) : s) + "/10";
        }
        if ("5".equals(f)) return Math.max(1, Math.round(raw / 20f)) + "★";
        return raw + "/100";
    }

    /** Score picker sheet — writes locally and pushes to AniList when signed in. */
    private static void showScoreSheet(final Context c, final MainActivity app, final JSONObject d,
                                       final String title, final LinearLayout actionsBox) {
        final JSONObject e = app.store.entry(d.optInt("id"));
        if (e == null) return;
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);

        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(18), Ui.dp(18), Ui.dp(18), Ui.dp(18));

        sheet.addView(Ui.text(c, "Your score", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, Anilist.authed() ? "Saves straight to your AniList profile" : "Stored on this device (guest)", 11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(3), 0, 0);
        sheet.addView(sub);
        sheet.addView(Ui.space(c, 16));

        final int[] val = {e.optInt("score", 75)};
        if (val[0] <= 0) val[0] = 75;
        LinearLayout srow = Ui.row(c);
        final TextView big = Ui.text(c, scoreLabel(app, val[0]), 22, Theme.ACC, Theme.MONO_BOLD);
        final android.widget.SeekBar sb = new android.widget.SeekBar(c);
        sb.setMax(100);
        sb.setProgress(val[0]);
        sb.getProgressDrawable().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.getThumb().setColorFilter(Theme.ACC, android.graphics.PorterDuff.Mode.SRC_IN);
        sb.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(android.widget.SeekBar s, int v, boolean u) {
                val[0] = v;
                big.setText(scoreLabel(app, v));
            }

            public void onStartTrackingTouch(android.widget.SeekBar s) {
            }

            public void onStopTrackingTouch(android.widget.SeekBar s) {
            }
        });
        LinearLayout.LayoutParams sp2 = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp2.weight = 1;
        srow.addView(sb, sp2);
        srow.addView(Ui.hspace(c, 12));
        srow.addView(big);
        sheet.addView(srow);
        sheet.addView(Ui.space(c, 16));

        LinearLayout btns = Ui.row(c);
        LinearLayout save = Ui.row(c);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 12, 0, 0), 0x33000000));
        save.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));
        save.addView(Ui.text(c, "Save score", 13, Theme.ACC_INK, Theme.SANS_BOLD));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    e.put("score", val[0]);
                } catch (Exception ignored) {
                }
                app.store.upsert(e);
                Anilist.push(app, e);
                app.toast("Scored " + scoreLabel(app, val[0]) + (Anilist.authed() ? " · synced to AniList" : ""), "check");
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
                renderActions(c, app, actionsBox, d, title);
            }
        });
        LinearLayout.LayoutParams svp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        svp.weight = 1;
        btns.addView(save, svp);

        if (e.optInt("score", 0) > 0) {
            LinearLayout clear = Ui.row(c);
            clear.setGravity(Gravity.CENTER);
            clear.setBackground(Ui.ripple(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            clear.setPadding(Ui.dp(16), Ui.dp(11), Ui.dp(16), Ui.dp(11));
            clear.addView(Ui.text(c, "Clear", 13, Theme.MUT, Theme.SANS_SB));
            clear.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    e.remove("score");
                    app.store.upsert(e);
                    Anilist.push(app, e);
                    app.toast("Score cleared", "info");
                    ViewGroup p = (ViewGroup) overlay.getParent();
                    if (p != null) p.removeView(overlay);
                    renderActions(c, app, actionsBox, d, title);
                }
            });
            btns.addView(clear, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 10, 0, 0, 0));
        }
        sheet.addView(btns);

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

    static String iconFor(String status) {
        for (int i = 0; i < STATUS_META.length; i++)
            if (STATUS_META[i][0].equals(status)) return STATUS_META[i][2];
        return "bookmark";
    }

    /** Bottom-sheet-style status picker (Pop in Detail.tsx). */
    private static void showStatusSheet(final Context c, final MainActivity app, final JSONObject d,
                                        final String title, final LinearLayout actionsBox) {
        final JSONObject e = app.store.entry(d.optInt("id"));
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);

        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(10));

        for (int i = 0; i < STATUS_META.length; i++) {
            final String sid = STATUS_META[i][0];
            boolean active = e != null && sid.equals(e.optString("status"));
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            item.setBackground(active ? Ui.rounded(Theme.ACC_SOFT, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, STATUS_META[i][2], 15, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            item.addView(Ui.text(c, STATUS_META[i][1], 13.5f, active ? Theme.ACC : Theme.TXT, Theme.SANS_SB));
            if (active) {
                View sprint = new View(c);
                LinearLayout.LayoutParams wp = Ui.lp(0, 1);
                wp.weight = 1;
                item.addView(sprint, wp);
                item.addView(new Icons(c, "check", 14, Theme.ACC), Ui.lp(Ui.dp(14), Ui.dp(14)));
            }
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setStatus(app, d, title, sid);
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                    renderActions(c, app, actionsBox, d, title);
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (e != null) {
            sheet.addView(Ui.divider(c));
            LinearLayout rm = Ui.row(c);
            rm.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            rm.addView(new Icons(c, "x", 15, Theme.ROSE), Ui.lp(Ui.dp(15), Ui.dp(15)));
            rm.addView(Ui.hspace(c, 11));
            rm.addView(Ui.text(c, "Remove from list", 13.5f, Theme.ROSE, Theme.SANS_SB));
            rm.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Anilist.delete(app, e);
                    app.store.remove(d.optInt("id"));
                    app.toast("Removed from your list" + (Anilist.authed() ? " · AniList updated" : ""), "trash");
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                    renderActions(c, app, actionsBox, d, title);
                }
            });
            sheet.addView(rm, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(20));
        overlay.addView(sheet, shp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void setStatus(MainActivity app, JSONObject d, String title, String status) {
        try {
            JSONObject e = app.store.entry(d.optInt("id"));
            if (e == null) {
                e = new JSONObject();
                e.put("id", d.optInt("id"));
                e.put("type", d.optString("type"));
                e.put("title", title);
                JSONObject mt = d.optJSONObject("title");
                if (mt != null) {
                    if (!mt.isNull("romaji")) e.put("titleR", mt.optString("romaji"));
                    if (!mt.isNull("english")) e.put("titleE", mt.optString("english"));
                    if (!mt.isNull("native")) e.put("titleN", mt.optString("native"));
                }
                JSONObject cov = d.optJSONObject("coverImage");
                if (cov != null) {
                    e.put("cover", cov.optString("large", null));
                    e.put("color", cov.optString("color", null));
                }
                String fmt2 = d.optString("format", null);
                if (fmt2 != null && !"null".equals(fmt2)) e.put("format", fmt2);
                if (d.optInt("seasonYear", 0) > 0) e.put("year", d.optInt("seasonYear"));
                String mstat = d.optString("status", null);
                if (mstat != null && !"null".equals(mstat)) e.put("mstatus", mstat);
                if (d.optInt("averageScore", 0) > 0) e.put("avg", d.optInt("averageScore"));
                e.put("progress", 0);
                int total = "MANGA".equals(d.optString("type")) ? d.optInt("chapters", -1) : d.optInt("episodes", -1);
                if (total > 0) e.put("total", total);
            }
            e.put("status", status);
            if ("COMPLETED".equals(status) && e.optInt("total", -1) > 0) e.put("progress", e.optInt("total"));
            app.store.upsert(e);
            Anilist.push(app, e);
            app.toast(Api.statusShort(status) + " — " + (title.length() > 26 ? title.substring(0, 26) + "…" : title)
                    + (Anilist.authed() ? " · synced" : ""), "check");
        } catch (Exception ignored) {
        }
    }

    /* -------------------------------- info tab -------------------------------- */

    private static void buildInfo(final Context c, final MainActivity app, LinearLayout col, final JSONObject d) {
        final boolean isAnime = "ANIME".equals(d.optString("type"));

        // description
        String desc = d.optString("description", null);
        if (desc != null && !"null".equals(desc) && desc.length() > 0) {
            LinearLayout wrap = Ui.col(c);
            wrap.setPadding(Ui.dp(16), Ui.dp(20), Ui.dp(16), 0);
            wrap.addView(Widgets.sectionHead(c, "info", "Synopsis", null));
            final TextView dt = Ui.text(c, desc.replaceAll("<br>", "\n").replaceAll("<[^>]+>", ""), 13, Theme.MUT, Theme.SANS);
            dt.setLineSpacing(Ui.dp(4), 1f);
            dt.setMaxLines(6);
            dt.setEllipsize(android.text.TextUtils.TruncateAt.END);
            wrap.addView(dt);
            final TextView more = Ui.text(c, "Read more", 12, Theme.ACC, Theme.SANS_SB);
            more.setPadding(0, Ui.dp(8), 0, 0);
            more.setOnClickListener(new View.OnClickListener() {
                boolean open = false;

                public void onClick(View v) {
                    open = !open;
                    dt.setMaxLines(open ? 9999 : 6);
                    more.setText(open ? "Show less" : "Read more");
                }
            });
            wrap.addView(more);
            col.addView(wrap);
        }

        // info grid
        LinearLayout gwrap = Ui.col(c);
        gwrap.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
        gwrap.addView(Widgets.sectionHead(c, "grid", "Information", null));
        LinearLayout card = Ui.col(c);
        card.setBackground(Ui.rounded(Theme.BG1, 16, Theme.LINE, 1));
        card.setPadding(Ui.dp(14), Ui.dp(6), Ui.dp(14), Ui.dp(6));
        addInfo(c, card, "Format", Api.formatLabel(d.optString("format", null)));
        addInfo(c, card, "Status", Api.statusLabel(d.optString("status", null)));
        if (isAnime) {
            if (d.optInt("episodes", 0) > 0) addInfo(c, card, "Episodes", String.valueOf(d.optInt("episodes")));
            if (d.optInt("duration", 0) > 0) addInfo(c, card, "Duration", d.optInt("duration") + " min");
        } else {
            if (d.optInt("chapters", 0) > 0) addInfo(c, card, "Chapters", String.valueOf(d.optInt("chapters")));
            if (d.optInt("volumes", 0) > 0) addInfo(c, card, "Volumes", String.valueOf(d.optInt("volumes")));
        }
        addInfo(c, card, "Start date", Api.fmtDate(d.optJSONObject("startDate")));
        addInfo(c, card, "End date", Api.fmtDate(d.optJSONObject("endDate")));
        if (!d.isNull("source")) addInfo(c, card, "Source", Api.sourceLabel(d.optString("source")));
        int mean = d.optInt("meanScore", 0);
        if (mean > 0) addInfo(c, card, "Mean score", mean + "%");
        long fav = d.optLong("favourites", 0);
        if (fav > 0) addInfo(c, card, "Favourites", Api.fmt(fav));
        JSONObject studios = d.optJSONObject("studios");
        if (studios != null) {
            JSONArray nodes = studios.optJSONArray("nodes");
            if (nodes != null && nodes.length() > 0) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < nodes.length(); i++) {
                    if (i > 0) s.append(", ");
                    s.append(nodes.optJSONObject(i).optString("name"));
                }
                addInfo(c, card, "Studio", s.toString());
            }
        }
        gwrap.addView(card);
        col.addView(gwrap);

        // genres
        JSONArray genres = d.optJSONArray("genres");
        if (genres != null && genres.length() > 0) {
            LinearLayout gw = Ui.col(c);
            gw.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
            gw.addView(Widgets.sectionHead(c, "sparkles", "Genres", null));
            List<String> gl = new ArrayList<String>();
            for (int i = 0; i < genres.length(); i++) gl.add(genres.optString(i));
            gw.addView(Widgets.wrapChips(c, gl, 3));
            col.addView(gw);
        }

        // characters rail
        JSONObject chars = d.optJSONObject("characters");
        if (chars != null) {
            JSONArray edges = chars.optJSONArray("edges");
            if (edges != null && edges.length() > 0) {
                LinearLayout cw = Ui.col(c);
                cw.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
                cw.addView(Widgets.sectionHead(c, "user", "Characters", "Tap a card on AniList for more"));
                HorizontalScrollView hs = new HorizontalScrollView(c);
                hs.setHorizontalScrollBarEnabled(false);
                LinearLayout row = Ui.row(c);
                row.setGravity(Gravity.TOP);
                for (int i = 0; i < edges.length(); i++) {
                    JSONObject edge = edges.optJSONObject(i);
                    if (edge == null) continue;
                    JSONObject node = edge.optJSONObject("node");
                    if (node == null) continue;
                    LinearLayout cc = Ui.col(c);
                    FrameLayout ib = new FrameLayout(c);
                    ib.setBackground(Ui.rounded(Theme.BG2, 14, Theme.LINE, 1));
                    Widgets.clipRounded(ib, 14);
                    ImageView iv = new ImageView(c);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ib.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    JSONObject img = node.optJSONObject("image");
                    if (img != null) Images.load(img.optString("large", null), iv, Ui.dp(84));
                    cc.addView(ib, Ui.lp(Ui.dp(84), Ui.dp(112)));
                    TextView nm = Ui.oneLine(Ui.text(c, node.optJSONObject("name") != null
                            ? node.optJSONObject("name").optString("full") : "", 11.5f, Theme.TXT, Theme.SANS_SB));
                    nm.setPadding(Ui.dp(2), Ui.dp(6), Ui.dp(2), 0);
                    nm.setMaxWidth(Ui.dp(84));
                    cc.addView(nm);
                    String role = edge.optString("role", "");
                    TextView rl = Ui.oneLine(Ui.text(c, role.length() > 0
                            ? role.substring(0, 1) + role.substring(1).toLowerCase() : "", 10, Theme.MUT, Theme.SANS_MED));
                    rl.setPadding(Ui.dp(2), Ui.dp(2), Ui.dp(2), 0);
                    rl.setMaxWidth(Ui.dp(84));
                    cc.addView(rl);
                    Ui.press(cc);
                    final int pid = node.optInt("id");
                    final String pname = node.optJSONObject("name") != null ? node.optJSONObject("name").optString("full") : "";
                    final String pimg = img != null ? img.optString("large", null) : null;
                    final String prole = role;
                    cc.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            PersonScreen.open(c, app, "character", pid, pname, pimg, prole);
                        }
                    });
                    row.addView(cc, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, i == 0 ? 0 : 12, 0, 0, 0));
                }
                hs.addView(row);
                cw.addView(hs);
                col.addView(cw);
            }
        }

        // staff rail (StaffCard section in InfoTab.tsx)
        JSONObject staff = d.optJSONObject("staff");
        if (staff != null) {
            JSONArray sedges = staff.optJSONArray("edges");
            if (sedges != null && sedges.length() > 0) {
                LinearLayout sw = Ui.col(c);
                sw.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
                sw.addView(Widgets.sectionHead(c, "users", "Staff", "Direction, music, character design"));
                HorizontalScrollView shs = new HorizontalScrollView(c);
                shs.setHorizontalScrollBarEnabled(false);
                LinearLayout srow = Ui.row(c);
                srow.setGravity(Gravity.TOP);
                for (int i = 0; i < sedges.length(); i++) {
                    JSONObject edge = sedges.optJSONObject(i);
                    if (edge == null) continue;
                    JSONObject node = edge.optJSONObject("node");
                    if (node == null) continue;
                    LinearLayout scCard = Ui.col(c);
                    FrameLayout ib = new FrameLayout(c);
                    ib.setBackground(Ui.rounded(Theme.BG2, 14, Theme.LINE, 1));
                    Widgets.clipRounded(ib, 14);
                    ImageView iv = new ImageView(c);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ib.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    JSONObject img = node.optJSONObject("image");
                    if (img != null) Images.load(img.optString("large", null), iv, Ui.dp(84));
                    scCard.addView(ib, Ui.lp(Ui.dp(84), Ui.dp(112)));
                    TextView nm = Ui.oneLine(Ui.text(c, node.optJSONObject("name") != null
                            ? node.optJSONObject("name").optString("full") : "", 11.5f, Theme.TXT, Theme.SANS_SB));
                    nm.setPadding(Ui.dp(2), Ui.dp(6), Ui.dp(2), 0);
                    nm.setMaxWidth(Ui.dp(84));
                    scCard.addView(nm);
                    TextView rl = Ui.oneLine(Ui.text(c, edge.optString("role", ""), 10, Theme.MUT, Theme.SANS_MED));
                    rl.setPadding(Ui.dp(2), Ui.dp(2), Ui.dp(2), 0);
                    rl.setMaxWidth(Ui.dp(84));
                    scCard.addView(rl);
                    Ui.press(scCard);
                    final int pid = node.optInt("id");
                    final String pname = node.optJSONObject("name") != null ? node.optJSONObject("name").optString("full") : "";
                    final String pimg = img != null ? img.optString("large", null) : null;
                    final String prole = edge.optString("role", null);
                    scCard.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            PersonScreen.open(c, app, "staff", pid, pname, pimg, prole);
                        }
                    });
                    srow.addView(scCard, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, i == 0 ? 0 : 12, 0, 0, 0));
                }
                shs.addView(srow);
                sw.addView(shs);
                col.addView(sw);
            }
        }

        // relations rail
        JSONObject relations = d.optJSONObject("relations");
        if (relations != null) {
            JSONArray redges = relations.optJSONArray("edges");
            if (redges != null && redges.length() > 0) {
                JSONArray media = new JSONArray();
                for (int i = 0; i < redges.length(); i++) {
                    JSONObject edge = redges.optJSONObject(i);
                    JSONObject node = edge != null ? edge.optJSONObject("node") : null;
                    if (node != null) {
                        try {
                            // show Prequel / Sequel / Adaptation under the card (annotated request)
                            node.put("_rel", Api.relationLabel(edge.optString("relationType", "")));
                        } catch (Exception ignored) {
                        }
                        media.put(node);
                    }
                }
                if (media.length() > 0) {
                    LinearLayout rw = Ui.col(c);
                    rw.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
                    rw.addView(Cards.rail(c, app, "layers", "Relations", "Sequels, prequels & spin-offs", media, 118,
                            new Cards.OnMedia() {
                                public void open(JSONObject m) {
                                    app.openDetail(m.optInt("id"), m);
                                }
                            }));
                    col.addView(rw);
                }
            }
        }

        // recommendations rail
        JSONObject recs = d.optJSONObject("recommendations");
        if (recs != null) {
            JSONArray nodes = recs.optJSONArray("nodes");
            if (nodes != null && nodes.length() > 0) {
                JSONArray media = new JSONArray();
                for (int i = 0; i < nodes.length(); i++) {
                    JSONObject n = nodes.optJSONObject(i);
                    JSONObject m = n != null ? n.optJSONObject("mediaRecommendation") : null;
                    if (m != null) media.put(m);
                }
                if (media.length() > 0) {
                    LinearLayout rw = Ui.col(c);
                    rw.setPadding(Ui.dp(16), Ui.dp(22), Ui.dp(16), 0);
                    rw.addView(Cards.rail(c, app, "heart", "You might also like", "Rated by the community", media, 118,
                            new Cards.OnMedia() {
                                public void open(JSONObject m) {
                                    app.openDetail(m.optInt("id"), m);
                                }
                            }));
                    col.addView(rw);
                }
            }
        }
    }

    private static void addInfo(Context c, LinearLayout card, String label, String value) {
        if (value == null || "null".equals(value) || value.length() == 0) return;
        LinearLayout r = Ui.row(c);
        r.setPadding(0, Ui.dp(9), 0, Ui.dp(9));
        TextView l = Ui.text(c, label, 12, Theme.MUT, Theme.SANS_MED);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(110), ViewGroup.LayoutParams.WRAP_CONTENT);
        r.addView(l, lp);
        TextView v = Ui.text(c, value, 12.5f, Theme.TXT, Theme.SANS_SB);
        LinearLayout.LayoutParams vp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.weight = 1;
        r.addView(v, vp);
        card.addView(r);
    }

    /* -------------------------------- watch tab -------------------------------- */

    private static void buildWatch(final Context c, final MainActivity app, LinearLayout col, final JSONObject d) {
        final LinearLayout box = Ui.col(c);
        col.addView(box);
        WatchPane.render(c, app, box, d);
    }

    /** Installed Aniyomi extensions of a kind (demo names when none installed). */
    private static java.util.List<String> enabledExts(MainActivity app, String kind) {
        java.util.List<String> out = app.store.extNames(kind);
        if (out.isEmpty()) out.add("ANIME".equals(kind) ? "AniWatch (demo)" : "MangaDex (demo)");
        return out;
    }

    private static void renderWatch(final Context c, final MainActivity app, final LinearLayout box,
                                    final JSONObject d, boolean searched) {
        box.removeAllViews();
        final boolean isAnime = "ANIME".equals(d.optString("type"));
        final String head = Api.titleOf(d, app.store.getS("titleLang", "romaji"));
        final String extKey = isAnime ? "watchExt" : "readExt";
        final String ext = app.store.getS(extKey, isAnime ? "AniWatch" : "MangaDex");

        // items: streamingEpisodes for anime when present, otherwise generated
        JSONArray raw = isAnime ? d.optJSONArray("streamingEpisodes") : null;
        final java.util.List<String[]> eps = new java.util.ArrayList<String[]>(); // {num, title, thumb}
        if (raw != null && raw.length() > 0) {
            for (int i = 0; i < raw.length(); i++) {
                JSONObject ep = raw.optJSONObject(i);
                if (ep == null) continue;
                String et = ep.optString("title", "Episode " + (i + 1));
                int n = i + 1;
                java.util.regex.Matcher mm = java.util.regex.Pattern.compile("(\\d+)").matcher(et);
                if (mm.find()) {
                    try {
                        n = Integer.parseInt(mm.group(1));
                    } catch (Exception ignored) {
                    }
                }
                eps.add(new String[]{String.valueOf(n), et, ep.optString("thumbnail", null)});
            }
        } else if (isAnime) {
            int n = d.optInt("episodes", 0);
            if (n <= 0) {
                JSONObject nae = d.optJSONObject("nextAiringEpisode");
                if (nae != null) n = Math.max(1, nae.optInt("episode", 1) - 1);
            }
            if (n <= 0) n = 12;
            n = Math.min(n, 100);
            for (int i = 1; i <= n; i++) eps.add(new String[]{String.valueOf(i), "Episode " + i, null});
        } else {
            // manga: newest chapter first
            int n = d.optInt("chapters", 0);
            JSONObject e0 = app.store.entry(d.optInt("id"));
            if (n <= 0 && e0 != null) n = Math.max(e0.optInt("progress", 0) + 12, 24);
            if (n <= 0) n = 40;
            int from = Math.max(1, n - 99);
            for (int i = n; i >= from; i--) eps.add(new String[]{String.valueOf(i), "Chapter " + i, null});
        }

        LinearLayout wrap = Ui.col(c);
        wrap.setPadding(Ui.dp(16), Ui.dp(20), Ui.dp(16), 0);

        LinearLayout headRow = Widgets.sectionHead(c, isAnime ? "play" : "book",
                isAnime ? "Episodes" : "Chapters",
                eps.size() + " available" + (isAnime ? " · " + app.store.getS("quality", "1080p") + " · sub" : " · EN"));
        LinearLayout srcBtn = Ui.row(c);
        srcBtn.setBackground(Ui.ripple(Ui.rounded(Theme.ACC_SOFT, 12, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 60)));
        srcBtn.setPadding(Ui.dp(11), Ui.dp(7), Ui.dp(9), Ui.dp(7));
        srcBtn.addView(Ui.text(c, ext, 11.5f, Theme.ACC, Theme.SANS_SB));
        srcBtn.addView(Ui.hspace(c, 4));
        srcBtn.addView(new Icons(c, "chev-down", 12, Theme.ACC), Ui.lp(Ui.dp(12), Ui.dp(12)));
        srcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showExtensionSheet(c, app, box, d, head);
            }
        });
        headRow.addView(srcBtn);
        wrap.addView(headRow);

        if (searched) {
            LinearLayout found = Ui.row(c);
            found.setBackground(Ui.rounded(Theme.ACC_SOFT, 10, Theme.ACC_LINE, 1));
            found.setPadding(Ui.dp(11), Ui.dp(8), Ui.dp(11), Ui.dp(8));
            found.addView(new Icons(c, "check", 12, Theme.ACC), Ui.lp(Ui.dp(12), Ui.dp(12)));
            found.addView(Ui.hspace(c, 7));
            TextView ft = Ui.oneLine(Ui.text(c, "Found \u201C" + head + "\u201D on " + ext + " — " + eps.size()
                    + (isAnime ? " episodes" : " chapters"), 11.5f, Theme.ACC, Theme.SANS_SB));
            found.addView(ft);
            wrap.addView(found, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));
        }

        boolean thumbs = isAnime && app.store.getB("showThumbs", true);
        for (int i = 0; i < eps.size(); i++) {
            final int epn = Integer.parseInt(eps.get(i)[0]);
            final String et = eps.get(i)[1];
            final String thumb = eps.get(i)[2];
            LinearLayout row = Ui.row(c);
            row.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            row.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(12), Ui.dp(10));

            if (thumbs && thumb != null && !"null".equals(thumb)) {
                FrameLayout tb = new FrameLayout(c);
                tb.setBackground(Ui.rounded(Theme.BG2, 10, Theme.LINE, 1));
                Widgets.clipRounded(tb, 10);
                ImageView iv = new ImageView(c);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                tb.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                Images.load(thumb, iv, Ui.dp(104));
                FrameLayout pb = new FrameLayout(c);
                pb.setBackground(Ui.circle(0x99000000));
                Icons pi = new Icons(c, "play", 11, 0xFFFFFFFF);
                FrameLayout.LayoutParams pip = new FrameLayout.LayoutParams(Ui.dp(11), Ui.dp(11));
                pip.gravity = Gravity.CENTER;
                pip.leftMargin = Ui.dp(2);
                pb.addView(pi, pip);
                FrameLayout.LayoutParams pbp = new FrameLayout.LayoutParams(Ui.dp(26), Ui.dp(26));
                pbp.gravity = Gravity.CENTER;
                tb.addView(pb, pbp);
                row.addView(tb, Ui.lpm(Ui.dp(104), Ui.dp(58), 0, 0, 12, 0));
            } else {
                FrameLayout pb = new FrameLayout(c);
                pb.setBackground(Ui.rounded(Theme.ACC_SOFT, 10, Theme.ACC_LINE, 1));
                Icons pi = new Icons(c, isAnime ? "play" : "book", 13, Theme.ACC);
                FrameLayout.LayoutParams pip = new FrameLayout.LayoutParams(Ui.dp(13), Ui.dp(13));
                pip.gravity = Gravity.CENTER;
                pb.addView(pi, pip);
                row.addView(pb, Ui.lpm(Ui.dp(38), Ui.dp(38), 0, 0, 12, 0));
            }

            LinearLayout tc = Ui.col(c);
            TextView t1 = Ui.text(c, et, 12.5f, Theme.TXT, Theme.SANS_SB);
            t1.setMaxLines(2);
            tc.addView(t1);
            TextView t2 = Ui.text(c, isAnime
                    ? app.store.getS("watchServer", "HD-1") + " · " + app.store.getS("quality", "1080p") + " · sub"
                    : ext + " · EN", 10.5f, Theme.MUT, Theme.MONO_MED);
            t2.setPadding(0, Ui.dp(3), 0, 0);
            tc.addView(t2);
            LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.weight = 1;
            row.addView(tc, tp);

            // read/watched tick for items at or below current progress
            JSONObject ecur = app.store.entry(d.optInt("id"));
            if (ecur != null && epn <= ecur.optInt("progress", 0)) {
                row.addView(new Icons(c, "check", 13, Theme.GREEN), Ui.lp(Ui.dp(13), Ui.dp(13)));
            }

            final PlayerScreen.OnDone onDone = new PlayerScreen.OnDone() {
                public void done(int num) {
                    if (app.store.getB("autoProgress", true)) {
                        JSONObject e = app.store.entry(d.optInt("id"));
                        if (e != null && num > e.optInt("progress", 0)) {
                            try {
                                e.put("progress", num);
                                String st = e.optString("status");
                                int total = e.optInt("total", -1);
                                if (total > 0 && num >= total) e.put("status", "COMPLETED");
                                else if ("PLANNING".equals(st) || "PAUSED".equals(st)) e.put("status", "CURRENT");
                            } catch (Exception ignored) {
                            }
                            app.store.upsert(e);
                            Anilist.push(app, e);
                            app.toast((isAnime ? "Ep. " : "Ch. ") + num + " finished — progress updated"
                                    + (Anilist.authed() ? " on AniList" : ""), "check");
                            return;
                        }
                    }
                    app.toast("Finished " + (isAnime ? "Ep. " : "Ch. ") + num, isAnime ? "play" : "check");
                }
            };
            String coverUrl = null;
            JSONObject cov2 = d.optJSONObject("coverImage");
            if (cov2 != null) coverUrl = cov2.optString("large", null);
            final String fCover = coverUrl;
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (isAnime) PlayerScreen.open(c, app, head, epn, et, thumb, onDone);
                    else ReaderScreen.open(c, app, head, epn, et, fCover, onDone);
                }
            });
            wrap.addView(row, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 10, 0, 0));
        }
        box.addView(wrap);
    }

    /** Extension picker: choose a source, then simulate searching this title on it. */
    private static void showExtensionSheet(final Context c, final MainActivity app, final LinearLayout box,
                                           final JSONObject d, final String title) {
        final boolean isAnime = "ANIME".equals(d.optString("type"));
        final String extKey = isAnime ? "watchExt" : "readExt";
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(8), Ui.dp(12), Ui.dp(8), Ui.dp(10));
        TextView tt = Ui.text(c, (isAnime ? "Watch" : "Read") + " from extension", 13, Theme.MUT, Theme.SANS_SB);
        tt.setPadding(Ui.dp(14), 0, Ui.dp(14), Ui.dp(8));
        sheet.addView(tt);

        String cur = app.store.getS(extKey, isAnime ? "AniWatch" : "MangaDex");
        java.util.List<String> exts = enabledExts(app, isAnime ? "ANIME" : "MANGA");
        for (int i = 0; i < exts.size(); i++) {
            final String name = exts.get(i);
            boolean active = name.equals(cur);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            item.setBackground(active ? Ui.rounded(Theme.ACC_SOFT, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, "layers", 15, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            item.addView(Ui.text(c, name, 13.5f, active ? Theme.ACC : Theme.TXT, Theme.SANS_SB));
            if (active) {
                View spr = new View(c);
                LinearLayout.LayoutParams wp = Ui.lp(0, 1);
                wp.weight = 1;
                item.addView(spr, wp);
                item.addView(new Icons(c, "check", 14, Theme.ACC), Ui.lp(Ui.dp(14), Ui.dp(14)));
            }
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.store.put(extKey, name);
                    ViewGroup p = (ViewGroup) overlay.getParent();
                    if (p != null) p.removeView(overlay);
                    box.removeAllViews();
                    LinearLayout sk = Ui.col(c);
                    sk.setPadding(Ui.dp(16), Ui.dp(20), Ui.dp(16), 0);
                    LinearLayout srow = Ui.row(c);
                    srow.addView(new Icons(c, "search", 13, Theme.ACC), Ui.lp(Ui.dp(13), Ui.dp(13)));
                    srow.addView(Ui.hspace(c, 8));
                    srow.addView(Ui.oneLine(Ui.text(c, "Searching \u201C" + title + "\u201D on " + name + "…",
                            12.5f, Theme.MUT, Theme.SANS_SB)));
                    sk.addView(srow);
                    sk.addView(Ui.space(c, 14));
                    for (int j = 0; j < 4; j++)
                        sk.addView(Widgets.skel(c, 14), Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(64), 0, j == 0 ? 0 : 10, 0, 0));
                    box.addView(sk);
                    new android.os.Handler().postDelayed(new Runnable() {
                        public void run() {
                            if (box.isAttachedToWindow()) renderWatch(c, app, box, d, true);
                        }
                    }, 900);
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

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

    /* --------------------------------- helpers --------------------------------- */

    private static ViewGroup.MarginLayoutParams newLp(View v, LinearLayout parent) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        v.setLayoutParams(p);
        return p;
    }

    private static LinearLayout.LayoutParams weight1() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.weight = 1;
        return p;
    }
}
