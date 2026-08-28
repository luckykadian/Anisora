package app.anisora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Watch / Read tab: Continue pill, 50-item range chips, Wrong title search,
 * and real extension episode/chapter lists.
 */
public class WatchPane {

    static final int RANGE = 50;

    static class Row {
        String url, name, preview;
        float number;
        int displayNum;
    }

    public static void render(final Context c, final MainActivity app, final LinearLayout box, final JSONObject d) {
        box.removeAllViews();
        final boolean isAnime = "ANIME".equals(d.optString("type"));
        final String head = Api.titleOf(d, app.store.getS("titleLang", "romaji"));
        final int mediaId = d.optInt("id");
        final String extKey = isAnime ? "watchExt" : "readExt";
        final String kind = isAnime ? "ANIME" : "MANGA";

        ExtBridge.SourceRef src = isAnime
                ? ExtBridge.findAnimeByName(app.store.getS(extKey, "AniWatch"))
                : ExtBridge.findMangaByName(app.store.getS(extKey, "MangaDex"));
        final long[] sourceId = {src != null ? src.id : 0};
        final String[] sourceName = {src != null ? src.name : app.store.getS(extKey, isAnime ? "AniWatch (demo)" : "MangaDex (demo)")};

        LinearLayout wrap = Ui.col(c);
        wrap.setPadding(Ui.dp(16), Ui.dp(20), Ui.dp(16), 0);

        /* ---------- toolbar: source + wrong title + settings ---------- */
        LinearLayout tools = Ui.row(c);
        LinearLayout srcBtn = Ui.row(c);
        srcBtn.setBackground(Ui.ripple(Ui.rounded(Theme.ACC_SOFT, 12, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 60)));
        srcBtn.setPadding(Ui.dp(11), Ui.dp(7), Ui.dp(9), Ui.dp(7));
        srcBtn.addView(Ui.text(c, sourceName[0], 11.5f, Theme.ACC, Theme.SANS_SB));
        srcBtn.addView(Ui.hspace(c, 4));
        srcBtn.addView(new Icons(c, "chev-down", 12, Theme.ACC), Ui.lp(Ui.dp(12), Ui.dp(12)));
        tools.addView(srcBtn);

        tools.addView(Ui.hspace(c, 8));
        LinearLayout wrong = Ui.row(c);
        wrong.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        wrong.setPadding(Ui.dp(11), Ui.dp(7), Ui.dp(11), Ui.dp(7));
        wrong.addView(new Icons(c, "search", 12, Theme.MUT), Ui.lp(Ui.dp(12), Ui.dp(12)));
        wrong.addView(Ui.hspace(c, 6));
        wrong.addView(Ui.text(c, "Wrong title", 11.5f, Theme.TXT, Theme.SANS_SB));
        tools.addView(wrong);

        if (src != null && src.configurable) {
            tools.addView(Ui.hspace(c, 8));
            FrameLayout gear = new FrameLayout(c);
            gear.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            Icons gi = new Icons(c, "settings", 14, Theme.MUT);
            FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
            gp.gravity = Gravity.CENTER;
            gear.addView(gi, gp);
            tools.addView(gear, Ui.lp(Ui.dp(34), Ui.dp(34)));
            gear.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showSourceSettings(c, app, sourceId[0], isAnime, sourceName[0]);
                }
            });
        }
        wrap.addView(tools);
        wrap.addView(Ui.space(c, 14));

        final LinearLayout body = Ui.col(c);
        wrap.addView(body);
        box.addView(wrap);

        final Runnable[] load = new Runnable[1];
        srcBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSourceSheet(c, app, d, box, isAnime, extKey, new Runnable() {
                    public void run() { render(c, app, box, d); }
                });
            }
        });
        wrong.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (sourceId[0] == 0) {
                    app.toast("Install an extension first (Settings → Extensions)", "info");
                    return;
                }
                showWrongTitle(c, app, d, head, sourceId[0], isAnime, sourceName[0], new Runnable() {
                    public void run() { load[0].run(); }
                });
            }
        });

        load[0] = new Runnable() {
            public void run() {
                fillBody(c, app, body, d, head, mediaId, isAnime, sourceId[0], sourceName[0], load[0]);
            }
        };
        load[0].run();
    }

    private static void fillBody(final Context c, final MainActivity app, final LinearLayout body,
                                 final JSONObject d, final String head, final int mediaId,
                                 final boolean isAnime, final long sourceId, final String sourceName,
                                 final Runnable reload) {
        body.removeAllViews();
        if (sourceId == 0) {
            demoList(c, app, body, d, head, isAnime);
            return;
        }
        JSONObject bind = app.store.getBind(isAnime ? "ANIME" : "MANGA", mediaId, sourceId);
        if (bind == null) {
            body.addView(searching(c, "Searching “" + head + "” on " + sourceName + "…"));
            ExtBridge.ListCb cb = new ExtBridge.ListCb() {
                public void ok(ArrayList items) {
                    if (!body.isAttachedToWindow()) return;
                    if (items == null || items.isEmpty()) {
                        body.removeAllViews();
                        body.addView(Widgets.emptyState(c, "search", "No match on " + sourceName,
                                "Tap Wrong title to search a different name."));
                        return;
                    }
                    ExtBridge.Hit best = pickHit(items, head);
                    app.store.setBind(isAnime ? "ANIME" : "MANGA", mediaId, sourceId, best.url, best.title, best.thumbnail);
                    app.toast("Matched “" + best.title + "” on " + sourceName, "check");
                    reload.run();
                }
                public void fail(String msg) {
                    if (!body.isAttachedToWindow()) return;
                    body.removeAllViews();
                    body.addView(Widgets.emptyState(c, "cloud-off", "Couldn't search " + sourceName, msg));
                }
            };
            if (isAnime) ExtBridge.searchAnime(sourceId, head, cb);
            else ExtBridge.searchManga(sourceId, head, cb);
            return;
        }

        body.addView(searching(c, "Loading " + (isAnime ? "episodes" : "chapters") + "…"));
        final String bUrl = bind.optString("url");
        final String bTitle = bind.optString("title", head);
        final String bThumb = bind.optString("thumb", null);
        ExtBridge.ListCb cb = new ExtBridge.ListCb() {
            public void ok(ArrayList items) {
                if (!body.isAttachedToWindow()) return;
                List<Row> rows = new ArrayList<Row>();
                for (int i = 0; i < items.size(); i++) {
                    ExtBridge.Item it = (ExtBridge.Item) items.get(i);
                    Row r = new Row();
                    r.url = it.url;
                    r.name = it.name;
                    r.preview = it.preview;
                    r.number = it.number;
                    r.displayNum = it.number > 0 ? Math.round(it.number) : i + 1;
                    rows.add(r);
                }
                paintList(c, app, body, d, head, isAnime, sourceId, sourceName, bTitle, rows);
            }
            public void fail(String msg) {
                if (!body.isAttachedToWindow()) return;
                body.removeAllViews();
                body.addView(Widgets.emptyState(c, "cloud-off", "Couldn't load list", msg));
            }
        };
        if (isAnime) ExtBridge.episodes(sourceId, bUrl, bTitle, bThumb, cb);
        else ExtBridge.chapters(sourceId, bUrl, bTitle, bThumb, cb);
    }

    private static ExtBridge.Hit pickHit(ArrayList items, String want) {
        String w = want.toLowerCase();
        ExtBridge.Hit first = (ExtBridge.Hit) items.get(0);
        for (int i = 0; i < items.size(); i++) {
            ExtBridge.Hit h = (ExtBridge.Hit) items.get(i);
            String t = h.title == null ? "" : h.title.toLowerCase();
            if (t.equals(w)) return h;
        }
        for (int i = 0; i < items.size(); i++) {
            ExtBridge.Hit h = (ExtBridge.Hit) items.get(i);
            String t = h.title == null ? "" : h.title.toLowerCase();
            if (t.contains(w) || w.contains(t)) return h;
        }
        return first;
    }

    private static View searching(Context c, String msg) {
        LinearLayout sk = Ui.col(c);
        LinearLayout srow = Ui.row(c);
        srow.addView(new Icons(c, "search", 13, Theme.ACC), Ui.lp(Ui.dp(13), Ui.dp(13)));
        srow.addView(Ui.hspace(c, 8));
        srow.addView(Ui.oneLine(Ui.text(c, msg, 12.5f, Theme.MUT, Theme.SANS_SB)));
        sk.addView(srow);
        sk.addView(Ui.space(c, 14));
        for (int j = 0; j < 4; j++)
            sk.addView(Widgets.skel(c, 14), Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(64), 0, j == 0 ? 0 : 10, 0, 0));
        return sk;
    }

    /* ------------------------------ list + continue + ranges ------------------------------ */

    private static void paintList(final Context c, final MainActivity app, final LinearLayout body,
                                  final JSONObject d, final String head, final boolean isAnime,
                                  final long sourceId, final String sourceName, final String boundTitle,
                                  final List<Row> rows) {
        body.removeAllViews();
        JSONObject ecur = app.store.entry(d.optInt("id"));
        final int progress = ecur == null ? 0 : ecur.optInt("progress", 0);

        LinearLayout headRow = Widgets.sectionHead(c, isAnime ? "play" : "book",
                isAnime ? "Episodes" : "Chapters",
                rows.size() + " on " + sourceName);
        body.addView(headRow);

        TextView matched = Ui.text(c, "Playing as “" + boundTitle + "”", 11, Theme.MUT, Theme.SANS);
        matched.setPadding(0, 0, 0, Ui.dp(12));
        body.addView(matched);

        Row cont = continueRow(rows, progress);
        if (cont != null) {
            body.addView(continueBanner(c, app, d, head, isAnime, sourceId, cont, ecur),
                    Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 14));
        }

        final int n = rows.size();
        final int[] rangeStart = {rangeContaining(rows, progress)};
        if (n > RANGE) {
            HorizontalScrollView hs = new HorizontalScrollView(c);
            hs.setHorizontalScrollBarEnabled(false);
            final LinearLayout chips = Ui.row(c);
            hs.addView(chips);
            body.addView(hs);
            body.addView(Ui.space(c, 12));
            rebuildRangeChips(c, chips, n, rangeStart[0], new Widgets.OnSeg() {
                public void pick(String id) {
                    rangeStart[0] = Integer.parseInt(id);
                    paintRows(c, app, listBox(body), d, head, isAnime, sourceId, rows, rangeStart[0], progress);
                    rebuildRangeChips(c, chips, n, rangeStart[0], this);
                }
            });
        }

        final LinearLayout list = Ui.col(c);
        list.setTag("list");
        body.addView(list);
        paintRows(c, app, list, d, head, isAnime, sourceId, rows, rangeStart[0], progress);
    }

    private static LinearLayout listBox(LinearLayout body) {
        for (int i = 0; i < body.getChildCount(); i++) {
            View v = body.getChildAt(i);
            if ("list".equals(v.getTag())) return (LinearLayout) v;
        }
        return body;
    }

    private static int rangeContaining(List<Row> rows, int progress) {
        int target = Math.max(1, progress + 1);
        int best = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).displayNum >= target) {
                return (i / RANGE) * RANGE;
            }
        }
        int last = Math.max(0, rows.size() - 1);
        return (last / RANGE) * RANGE;
    }

    private static void rebuildRangeChips(Context c, LinearLayout chips, int n, int selected,
                                          final Widgets.OnSeg cb) {
        chips.removeAllViews();
        int g = 0;
        while (g < n) {
            final int start = g;
            int end = Math.min(n, g + RANGE);
            boolean on = start == selected;
            LinearLayout chip = Ui.row(c);
            chip.setBackground(on
                    ? Ui.ripple(Ui.rounded(Theme.ACC, 999, 0, 0), 0x33000000)
                    : Ui.ripple(Ui.rounded(Theme.BG1, 999, Theme.ACC_LINE, 1), Theme.alpha(Theme.ACC, 40)));
            chip.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(8));
            chip.addView(Ui.text(c, (start + 1) + " – " + end, 12, on ? Theme.ACC_INK : Theme.ACC, Theme.SANS_SB));
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { cb.pick(String.valueOf(start)); }
            });
            chips.addView(chip, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, g == 0 ? 0 : 8, 0, 0, 0));
            g += RANGE;
        }
    }

    private static Row continueRow(List<Row> rows, int progress) {
        int want = progress + 1;
        Row fallback = null;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.displayNum == want) return r;
            if (r.displayNum > progress && fallback == null) fallback = r;
        }
        if (fallback != null) return fallback;
        return rows.isEmpty() ? null : rows.get(Math.min(progress, rows.size() - 1));
    }

    private static View continueBanner(final Context c, final MainActivity app, final JSONObject d,
                                       final String head, final boolean isAnime, final long sourceId,
                                       final Row row, JSONObject ecur) {
        FrameLayout card = new FrameLayout(c);
        card.setBackground(Ui.rounded(Theme.BG2, 18, Theme.LINE, 1));
        Widgets.clipRounded(card, 18);

        ImageView bg = new ImageView(c);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.55f);
        card.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String cover = row.preview;
        if (cover == null || cover.length() == 0) {
            JSONObject cov = d.optJSONObject("coverImage");
            if (cov != null) cover = cov.optString("large", null);
        }
        if (cover != null && !"null".equals(cover)) Images.load(cover, bg, 900);

        View scrim = new View(c);
        scrim.setBackgroundColor(0x66000000);
        card.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout rowL = Ui.row(c);
        rowL.setPadding(Ui.dp(16), Ui.dp(18), Ui.dp(14), Ui.dp(18));
        LinearLayout tcol = Ui.col(c);
        String unit = isAnime ? "Episode" : "Chapter";
        tcol.addView(Ui.text(c, "Continue : " + unit + " " + row.displayNum, 15, 0xFFFFFFFF, Theme.SANS_BOLD));
        TextView sub = Ui.oneLine(Ui.text(c, row.name, 12, 0xCCFFFFFF, Theme.SANS_MED));
        sub.setPadding(0, Ui.dp(4), 0, 0);
        tcol.addView(sub);
        LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.weight = 1;
        rowL.addView(tcol, tp);

        FrameLayout play = new FrameLayout(c);
        play.setBackground(Ui.circle(0xE6FFFFFF));
        Icons pi = new Icons(c, "play", 16, 0xFF111111);
        FrameLayout.LayoutParams pip = new FrameLayout.LayoutParams(Ui.dp(16), Ui.dp(16));
        pip.gravity = Gravity.CENTER;
        pip.leftMargin = Ui.dp(2);
        play.addView(pi, pip);
        rowL.addView(play, Ui.lp(Ui.dp(44), Ui.dp(44)));
        card.addView(rowL, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View.OnClickListener go = new View.OnClickListener() {
            public void onClick(View v) { openItem(c, app, d, head, isAnime, sourceId, row); }
        };
        card.setOnClickListener(go);
        play.setOnClickListener(go);
        return card;
    }

    private static void paintRows(final Context c, final MainActivity app, LinearLayout list,
                                  final JSONObject d, final String head, final boolean isAnime,
                                  final long sourceId, List<Row> rows, int start, int progress) {
        list.removeAllViews();
        int end = Math.min(rows.size(), start + RANGE);
        for (int i = start; i < end; i++) {
            final Row row = rows.get(i);
            LinearLayout r = Ui.row(c);
            r.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            r.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(12), Ui.dp(10));

            FrameLayout pb = new FrameLayout(c);
            pb.setBackground(Ui.rounded(Theme.ACC_SOFT, 10, Theme.ACC_LINE, 1));
            Icons pi = new Icons(c, isAnime ? "play" : "book", 13, Theme.ACC);
            FrameLayout.LayoutParams pip = new FrameLayout.LayoutParams(Ui.dp(13), Ui.dp(13));
            pip.gravity = Gravity.CENTER;
            pb.addView(pi, pip);
            r.addView(pb, Ui.lpm(Ui.dp(38), Ui.dp(38), 0, 0, 12, 0));

            LinearLayout tc = Ui.col(c);
            tc.addView(Ui.text(c, row.name, 12.5f, Theme.TXT, Theme.SANS_SB));
            TextView t2 = Ui.text(c, (isAnime ? "Ep. " : "Ch. ") + row.displayNum + " · " + (isAnime
                    ? app.store.getS("watchServer", "HD-1") + " · " + app.store.getS("quality", "1080p")
                    : "EN"), 10.5f, Theme.MUT, Theme.MONO_MED);
            t2.setPadding(0, Ui.dp(3), 0, 0);
            tc.addView(t2);
            LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.weight = 1;
            r.addView(tc, tp);

            if (row.displayNum <= progress) {
                r.addView(new Icons(c, "check", 13, Theme.GREEN), Ui.lp(Ui.dp(13), Ui.dp(13)));
            }
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openItem(c, app, d, head, isAnime, sourceId, row); }
            });
            list.addView(r, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == start ? 0 : 10, 0, 0));
        }
    }

    private static void openItem(final Context c, final MainActivity app, final JSONObject d,
                                 final String head, final boolean isAnime, final long sourceId, final Row row) {
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
                        } catch (Exception ignored) {}
                        app.store.upsert(e);
                        Anilist.push(app, e);
                        app.toast((isAnime ? "Ep. " : "Ch. ") + num + " finished — progress updated"
                                + (Anilist.authed() ? " on AniList" : ""), "check");
                    }
                }
            }
        };
        if (sourceId == 0) {
            JSONObject cov = d.optJSONObject("coverImage");
            String cover = cov != null ? cov.optString("large", null) : null;
            if (isAnime) PlayerScreen.open(c, app, head, row.displayNum, row.name, row.preview, onDone);
            else ReaderScreen.open(c, app, head, row.displayNum, row.name, cover, onDone);
            return;
        }
        if (!isAnime) {
            app.toast("Loading pages…", "sync");
            ExtBridge.pages(sourceId, row.url, row.name, row.number, new ExtBridge.ListCb() {
                public void ok(ArrayList items) {
                    ArrayList<String> urls = new ArrayList<String>();
                    for (int i = 0; i < items.size(); i++) urls.add(String.valueOf(items.get(i)));
                    ReaderScreen.openPages(c, app, head, row.displayNum, row.name, urls, onDone);
                }
                public void fail(String msg) {
                    app.toast("Couldn't load pages — " + msg, "info");
                    JSONObject cov = d.optJSONObject("coverImage");
                    String cover = cov != null ? cov.optString("large", null) : null;
                    ReaderScreen.open(c, app, head, row.displayNum, row.name, cover, onDone);
                }
            });
            return;
        }
        app.toast("Fetching streams…", "sync");
        ExtBridge.videos(sourceId, row.url, row.name, row.number, new ExtBridge.ListCb() {
            public void ok(ArrayList items) {
                if (items == null || items.isEmpty()) {
                    app.toast("No streams from this extension", "info");
                    PlayerScreen.open(c, app, head, row.displayNum, row.name, row.preview, onDone);
                    return;
                }
                ExtBridge.Stream pick = pickStream(app, items);
                if (items.size() > 1) showQualityThenPlay(c, app, head, row, items, onDone);
                else playStream(app, head, row, pick, onDone);
            }
            public void fail(String msg) {
                app.toast("Couldn't extract video — " + msg, "info");
                PlayerScreen.open(c, app, head, row.displayNum, row.name, row.preview, onDone);
            }
        });
    }

    private static ExtBridge.Stream pickStream(MainActivity app, ArrayList items) {
        String want = app.store.getS("quality", "1080p");
        ExtBridge.Stream first = (ExtBridge.Stream) items.get(0);
        for (int i = 0; i < items.size(); i++) {
            ExtBridge.Stream s = (ExtBridge.Stream) items.get(i);
            if (s.quality != null && s.quality.toLowerCase().contains(want.toLowerCase().replace("p", ""))) return s;
        }
        return first;
    }

    private static void showQualityThenPlay(final Context c, final MainActivity app, final String head,
                                            final Row row, final ArrayList items, final PlayerScreen.OnDone onDone) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(8), Ui.dp(12), Ui.dp(8), Ui.dp(10));
        TextView tt = Ui.text(c, "Quality / server", 13, Theme.MUT, Theme.SANS_SB);
        tt.setPadding(Ui.dp(14), 0, Ui.dp(14), Ui.dp(8));
        sheet.addView(tt);
        for (int i = 0; i < items.size(); i++) {
            final ExtBridge.Stream s = (ExtBridge.Stream) items.get(i);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            item.addView(new Icons(c, "play", 15, Theme.ACC), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            item.addView(Ui.text(c, s.quality == null || s.quality.length() == 0 ? ("Stream " + (i + 1)) : s.quality,
                    13.5f, Theme.TXT, Theme.SANS_SB));
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ((ViewGroup) overlay.getParent()).removeView(overlay);
                    playStream(app, head, row, s, onDone);
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(20));
        overlay.addView(sheet, shp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { ((ViewGroup) overlay.getParent()).removeView(overlay); }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void playStream(final MainActivity app, String head, final Row row,
                                   ExtBridge.Stream s, final PlayerScreen.OnDone onDone) {
        Map<String, String> headers = s.headers == null ? new HashMap<String, String>() : s.headers;
        ExtPlayer.play(app, "E" + row.displayNum + " · " + row.name, s.url, headers, new Runnable() {
            public void run() { onDone.done(row.displayNum); }
        });
    }

    /* ------------------------------ wrong title ------------------------------ */

    static void showWrongTitle(final Context c, final MainActivity app, final JSONObject d,
                               final String seed, final long sourceId, final boolean isAnime,
                               final String sourceName, final Runnable onBound) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));

        sheet.addView(Ui.text(c, "Wrong title", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, "Search " + sourceName + " and pick the matching entry", 11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(3), 0, Ui.dp(12));
        sheet.addView(sub);

        LinearLayout qrow = Ui.row(c);
        final EditText input = new EditText(c);
        input.setText(seed);
        input.setHint("Title on this extension");
        input.setHintTextColor(Theme.alpha(Theme.MUT, 150));
        input.setTextColor(Theme.TXT);
        input.setTextSize(13f);
        input.setTypeface(Theme.SANS_MED);
        input.setSingleLine(true);
        input.setBackground(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1));
        input.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        LinearLayout.LayoutParams ip = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        ip.weight = 1;
        qrow.addView(input, ip);
        LinearLayout go = Ui.row(c);
        go.setGravity(Gravity.CENTER);
        go.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 12, 0, 0), 0x33000000));
        go.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        go.addView(Ui.text(c, "Search", 12.5f, Theme.ACC_INK, Theme.SANS_BOLD));
        qrow.addView(go, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8, 0, 0, 0));
        sheet.addView(qrow);

        final LinearLayout results = Ui.col(c);
        results.setPadding(0, Ui.dp(12), 0, 0);
        ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        sc.addView(results);
        sheet.addView(sc, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(280)));

        final Runnable runSearch = new Runnable() {
            public void run() {
                String q = input.getText().toString().trim();
                if (q.length() < 2) { app.toast("Type a bit more", "info"); return; }
                results.removeAllViews();
                results.addView(Ui.text(c, "Searching…", 12, Theme.MUT, Theme.SANS_SB));
                ExtBridge.ListCb cb = new ExtBridge.ListCb() {
                    public void ok(ArrayList items) {
                        results.removeAllViews();
                        if (items == null || items.isEmpty()) {
                            results.addView(Ui.text(c, "No results", 12, Theme.MUT, Theme.SANS));
                            return;
                        }
                        for (int i = 0; i < items.size(); i++) {
                            final ExtBridge.Hit h = (ExtBridge.Hit) items.get(i);
                            LinearLayout row = Ui.row(c);
                            row.setBackground(Ui.ripple(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
                            row.setPadding(Ui.dp(10), Ui.dp(10), Ui.dp(10), Ui.dp(10));
                            FrameLayout ic = new FrameLayout(c);
                            ic.setBackground(Ui.rounded(Theme.BG1, 8, Theme.LINE, 1));
                            Widgets.clipRounded(ic, 8);
                            ImageView iv = new ImageView(c);
                            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ic.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            if (h.thumbnail != null) Images.load(h.thumbnail, iv, Ui.dp(40));
                            row.addView(ic, Ui.lpm(Ui.dp(40), Ui.dp(56), 0, 0, 10, 0));
                            row.addView(Ui.text(c, h.title, 13, Theme.TXT, Theme.SANS_SB));
                            row.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    app.store.setBind(isAnime ? "ANIME" : "MANGA", d.optInt("id"), sourceId, h.url, h.title, h.thumbnail);
                                    app.toast("Using “" + h.title + "”", "check");
                                    ViewGroup p = (ViewGroup) overlay.getParent();
                                    if (p != null) p.removeView(overlay);
                                    onBound.run();
                                }
                            });
                            results.addView(row, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 8, 0, 0));
                        }
                    }
                    public void fail(String msg) {
                        results.removeAllViews();
                        results.addView(Ui.text(c, msg, 12, Theme.ROSE, Theme.SANS));
                    }
                };
                if (isAnime) ExtBridge.searchAnime(sourceId, q, cb);
                else ExtBridge.searchManga(sourceId, q, cb);
            }
        };
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { runSearch.run(); }
        });
        runSearch.run();

        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(20));
        overlay.addView(sheet, shp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { ViewGroup p = (ViewGroup) overlay.getParent(); if (p != null) p.removeView(overlay); }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /* ------------------------------ source picker / settings ------------------------------ */

    static void showSourceSheet(final Context c, final MainActivity app, final JSONObject d,
                                final LinearLayout box, final boolean isAnime, final String extKey,
                                final Runnable onPicked) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(8), Ui.dp(12), Ui.dp(8), Ui.dp(10));
        TextView tt = Ui.text(c, (isAnime ? "Watch" : "Read") + " from extension", 13, Theme.MUT, Theme.SANS_SB);
        tt.setPadding(Ui.dp(14), 0, Ui.dp(14), Ui.dp(8));
        sheet.addView(tt);

        ArrayList<ExtBridge.SourceRef> list = isAnime ? ExtBridge.animeSources() : ExtBridge.mangaSources();
        String cur = app.store.getS(extKey, isAnime ? "AniWatch" : "MangaDex");
        if (list.isEmpty()) {
            TextView empty = Ui.text(c, "No extensions loaded. Install one in Settings → Extensions.", 12, Theme.MUT, Theme.SANS);
            empty.setPadding(Ui.dp(14), Ui.dp(8), Ui.dp(14), Ui.dp(12));
            sheet.addView(empty);
        }
        for (int i = 0; i < list.size(); i++) {
            final ExtBridge.SourceRef ref = list.get(i);
            boolean active = ref.name.equals(cur);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            item.setBackground(active ? Ui.rounded(Theme.ACC_SOFT, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, "layers", 15, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            LinearLayout col = Ui.col(c);
            col.addView(Ui.text(c, ref.name, 13.5f, active ? Theme.ACC : Theme.TXT, Theme.SANS_SB));
            if (ref.lang != null && ref.lang.length() > 0)
                col.addView(Ui.text(c, ref.lang.toUpperCase(), 10, Theme.MUT, Theme.MONO_MED));
            LinearLayout.LayoutParams cp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.weight = 1;
            item.addView(col, cp);
            if (ref.configurable) {
                FrameLayout gear = new FrameLayout(c);
                Icons gi = new Icons(c, "settings", 14, Theme.MUT);
                gear.addView(gi, Ui.lp(Ui.dp(14), Ui.dp(14)));
                gear.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showSourceSettings(c, app, ref.id, isAnime, ref.name); }
                });
                item.addView(gear, Ui.lp(Ui.dp(28), Ui.dp(28)));
            }
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.store.put(extKey, ref.name);
                    ViewGroup p = (ViewGroup) overlay.getParent();
                    if (p != null) p.removeView(overlay);
                    onPicked.run();
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), 0, Ui.dp(12), Ui.dp(20));
        overlay.addView(sheet, shp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { ViewGroup p = (ViewGroup) overlay.getParent(); if (p != null) p.removeView(overlay); }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    static void showSourceSettings(final Context c, final MainActivity app, long sourceId, boolean isAnime, String name) {
        JSONArray prefs = ExtBridge.settingsJson(sourceId, isAnime);
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        ScrollView sc = new ScrollView(c);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        sheet.addView(Ui.text(c, name + " settings", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, "Same per-source preferences Aniyomi exposes via ConfigurableSource", 11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(4), 0, Ui.dp(12));
        sheet.addView(sub);
        if (prefs.length() == 0) {
            sheet.addView(Ui.text(c, "This extension has no extra settings.", 12.5f, Theme.MUT, Theme.SANS));
        }
        android.content.SharedPreferences sp = c.getSharedPreferences("source_" + sourceId, Context.MODE_PRIVATE);
        for (int i = 0; i < prefs.length(); i++) {
            JSONObject p = prefs.optJSONObject(i);
            if (p == null) continue;
            String type = p.optString("type");
            if ("category".equals(type)) {
                TextView cat = Ui.text(c, p.optString("title"), 11, Theme.MUT, Theme.SANS_SB);
                cat.setPadding(0, Ui.dp(12), 0, Ui.dp(6));
                sheet.addView(cat);
                continue;
            }
            final String key = p.optString("key");
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(10), 0, Ui.dp(10));
            LinearLayout tcol = Ui.col(c);
            tcol.addView(Ui.text(c, p.optString("title", key), 13.5f, Theme.TXT, Theme.SANS_SB));
            if (p.optString("summary").length() > 0) {
                TextView sm = Ui.text(c, p.optString("summary"), 11, Theme.MUT, Theme.SANS);
                sm.setPadding(0, Ui.dp(3), 0, 0);
                tcol.addView(sm);
            }
            LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            tp.weight = 1;
            row.addView(tcol, tp);
            if ("toggle".equals(type)) {
                boolean on = sp.getBoolean(key, p.optBoolean("value", false));
                row.addView(Widgets.toggle(c, on, new Widgets.OnToggle() {
                    public void toggled(boolean v) { sp.edit().putBoolean(key, v).apply(); }
                }));
            } else if ("list".equals(type)) {
                JSONArray entries = p.optJSONArray("entries");
                JSONArray values = p.optJSONArray("entryValues");
                String cur = sp.getString(key, p.optString("value", ""));
                final TextView val = Ui.text(c, cur, 12, Theme.ACC, Theme.SANS_SB);
                row.addView(val);
                final JSONArray fEntries = entries;
                final JSONArray fValues = values;
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (fValues == null) return;
                        // cycle
                        int idx = 0;
                        for (int k = 0; k < fValues.length(); k++) if (fValues.optString(k).equals(sp.getString(key, ""))) idx = k;
                        idx = (idx + 1) % fValues.length();
                        String nv = fValues.optString(idx);
                        sp.edit().putString(key, nv).apply();
                        val.setText(fEntries != null && idx < fEntries.length() ? fEntries.optString(idx) : nv);
                    }
                });
            } else if ("text".equals(type)) {
                TextView val = Ui.text(c, sp.getString(key, p.optString("value", "")), 11, Theme.MUT, Theme.MONO_MED);
                row.addView(val);
            }
            sheet.addView(row);
            sheet.addView(Ui.divider(c));
        }
        sc.addView(sheet);
        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), Ui.dp(40), Ui.dp(12), Ui.dp(20));
        overlay.addView(sc, shp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { ViewGroup p = (ViewGroup) overlay.getParent(); if (p != null) p.removeView(overlay); }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /* ------------------------------ demo fallback ------------------------------ */

    private static void demoList(final Context c, final MainActivity app, LinearLayout body,
                                 final JSONObject d, final String head, final boolean isAnime) {
        JSONArray raw = isAnime ? d.optJSONArray("streamingEpisodes") : null;
        List<Row> rows = new ArrayList<Row>();
        if (raw != null && raw.length() > 0) {
            for (int i = 0; i < raw.length(); i++) {
                JSONObject ep = raw.optJSONObject(i);
                if (ep == null) continue;
                Row r = new Row();
                r.name = ep.optString("title", "Episode " + (i + 1));
                r.displayNum = i + 1;
                r.preview = ep.optString("thumbnail", null);
                r.url = "";
                rows.add(r);
            }
        } else if (isAnime) {
            int n = d.optInt("episodes", 12);
            if (n <= 0) n = 12;
            n = Math.min(n, 200);
            for (int i = 1; i <= n; i++) {
                Row r = new Row();
                r.name = "Episode " + i;
                r.displayNum = i;
                r.url = "";
                rows.add(r);
            }
        } else {
            int n = d.optInt("chapters", 40);
            if (n <= 0) n = 40;
            n = Math.min(n, 200);
            for (int i = 1; i <= n; i++) {
                Row r = new Row();
                r.name = "Chapter " + i;
                r.displayNum = i;
                r.url = "";
                rows.add(r);
            }
        }
        paintList(c, app, body, d, head, isAnime, 0, "demo", head, rows);
    }
}
