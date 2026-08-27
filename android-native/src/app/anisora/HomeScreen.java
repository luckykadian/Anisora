package app.anisora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home (Anime / Manga) — mirrors src/components/Home.tsx. */
public class HomeScreen {

    public static View build(final Context c, final MainActivity app, final String type) {
        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout col = Ui.col(c);
        int pad = 16;
        col.setPadding(Ui.dp(pad), Ui.dp(20), Ui.dp(pad), Ui.dp(110));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final boolean isAnime = "ANIME".equals(type);
        final Cards.OnMedia open = new Cards.OnMedia() {
            public void open(JSONObject m) {
                app.openDetail(m.optInt("id"), m);
            }
        };

        /* ---------- greeting ---------- */
        String dateStr = new SimpleDateFormat("EEEE, MMMM d", Locale.US).format(new Date());
        col.addView(Ui.mono(c, dateStr + " · " + Api.seasonLabel(Api.seasonNow()) + " " + Api.yearNow(), 10, Theme.MUT));
        col.addView(Ui.space(c, 7));

        int hour = new Date().getHours();
        String greet = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        LinearLayout h1 = Ui.row(c);
        TextView g1 = Ui.text(c, greet + ", ", 27, Theme.TXT, Theme.DISP_BOLD);
        TextView g2 = Ui.text(c, app.store.userName(), 27, Theme.ACC, Theme.DISP_BOLD);
        h1.addView(g1);
        h1.addView(g2);
        col.addView(h1);
        col.addView(Ui.space(c, 6));

        List<JSONObject> entries = app.store.entriesOf(type);
        sortEntries(entries, app.store.getS("librarySort", "updated"));
        int inProgress = 0;
        for (int i = 0; i < entries.size(); i++) {
            String st = entries.get(i).optString("status");
            if ("CURRENT".equals(st) || "PAUSED".equals(st) || "REPEATING".equals(st)) inProgress++;
        }
        String subMsg = inProgress > 0
                ? "You have " + inProgress + " " + (isAnime ? "series" : "titles") + " in progress — pick up where you left off."
                : "Your " + (isAnime ? "anime" : "manga") + " universe awaits.";
        col.addView(Ui.text(c, subMsg, 12.5f, Theme.MUT, Theme.SANS));
        col.addView(Ui.space(c, 10));

        final LinearLayout statusChip = Ui.row(c);
        statusChip.addView(Widgets.chip(c, "AniList connected", new Icons(c, "wifi", 12, Theme.GREEN), false));
        col.addView(statusChip);
        col.addView(Ui.space(c, 28));

        /* ---------- library ---------- */
        if (entries.size() > 0) {
            LinearLayout head = Widgets.sectionHead(c, "grid",
                    "My " + (isAnime ? "Anime" : "Manga") + " List",
                    entries.size() + " tracked on this device");
            // sort button (annotated request: sort by last updated / date added / …)
            LinearLayout sortBtn = Ui.row(c);
            sortBtn.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 12, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
            sortBtn.setPadding(Ui.dp(11), Ui.dp(7), Ui.dp(11), Ui.dp(7));
            sortBtn.addView(new Icons(c, "sort", 13, Theme.MUT), Ui.lp(Ui.dp(13), Ui.dp(13)));
            sortBtn.addView(Ui.hspace(c, 6));
            sortBtn.addView(Ui.text(c, sortLabel(app.store.getS("librarySort", "updated")), 11.5f, Theme.MUT, Theme.SANS_SB));
            sortBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showSortSheet(c, app);
                }
            });
            head.addView(sortBtn);
            col.addView(head);

            // status filter seg (horizontally scrollable)
            final String[] cur = {app.getFilter(type)};
            HorizontalScrollView fh = new HorizontalScrollView(c);
            fh.setHorizontalScrollBarEnabled(false);
            String[][] all = {{"ALL", "All"}, {"CURRENT", "In Progress"}, {"COMPLETED", "Completed"},
                    {"PLANNING", "Planning"}, {"PAUSED", "Paused"}, {"DROPPED", "Dropped"}};
            java.util.ArrayList<String[]> avail = new java.util.ArrayList<String[]>();
            for (int i = 0; i < all.length; i++) {
                if ("ALL".equals(all[i][0])) {
                    avail.add(all[i]);
                    continue;
                }
                for (int j = 0; j < entries.size(); j++) {
                    if (all[i][0].equals(entries.get(j).optString("status"))) {
                        avail.add(all[i]);
                        break;
                    }
                }
            }
            fh.addView(Widgets.seg(c, avail.toArray(new String[0][]), cur[0], new Widgets.OnSeg() {
                public void pick(String id) {
                    app.setFilter(type, id);
                    app.rebuildContent();
                }
            }));
            col.addView(fh);
            col.addView(Ui.space(c, 14));

            java.util.ArrayList<JSONObject> filtered = new java.util.ArrayList<JSONObject>();
            for (int i = 0; i < entries.size(); i++) {
                if ("ALL".equals(cur[0]) || cur[0].equals(entries.get(i).optString("status"))) filtered.add(entries.get(i));
            }
            if (filtered.size() > 0) {
                col.addView(Cards.grid(c, app, null, filtered, gridColumns(c, app), pad, open));
            } else {
                col.addView(Widgets.emptyState(c, "search", "Nothing here",
                        "No " + (isAnime ? "anime" : "manga") + " matches this filter yet."));
            }
            col.addView(Ui.space(c, 34));
        }

        /* ---------- rails (loading skeleton first) ---------- */
        final LinearLayout railsBox = Ui.col(c);
        col.addView(railsBox);
        int cardW = "compact".equals(app.store.getS("density", "cozy")) ? 118 : 136;
        railsBox.addView(Widgets.skeletonRail(c, "flame", "Trending now", cardW));
        railsBox.addView(Ui.space(c, 30));
        railsBox.addView(Widgets.skeletonRail(c, "trophy", "All-time top rated", cardW));
        final int fCardW = cardW;

        Api.fetchHome(type, new Api.Cb() {
            public void ok(JSONObject d) {
                if (!sc.isAttachedToWindow()) return;
                railsBox.removeAllViews();
                statusChip.removeAllViews();
                statusChip.addView(Widgets.chip(c, "AniList connected", new Icons(c, "wifi", 12, Theme.GREEN), false));
                addRail(railsBox, c, app, "flame", "Trending now",
                        isAnime ? "What everyone is watching" : "Hot on the charts", d, "trending", fCardW, open);
                addRail(railsBox, c, app, "trending",
                        isAnime ? "Popular this " + Api.seasonLabel(Api.seasonNow()).toLowerCase() : "All-time favourites",
                        isAnime ? Api.seasonLabel(Api.seasonNow()) + " " + Api.yearNow() + " season" : "Most loved by the community",
                        d, "seasonal", fCardW, open);
                addRail(railsBox, c, app, "trophy", "Top rated", "Critics' darlings", d, "top", fCardW, open);
                addRail(railsBox, c, app, "heart", "Community favourites", null, d, "loved", fCardW, open);
                addFooter(railsBox, c);
                for (int i = 0; i < railsBox.getChildCount(); i++) Ui.appear(railsBox.getChildAt(i), i * 55);
            }

            public void fail(Exception e) {
                if (!sc.isAttachedToWindow()) return;
                railsBox.removeAllViews();
                statusChip.removeAllViews();
                LinearLayout warn = Ui.row(c);
                Icons ci = new Icons(c, "cloud-off", 12, Theme.AMBER);
                LinearLayout chip = Ui.row(c);
                chip.setBackground(Ui.rounded(0x1AFBBF24, 999, 0x4DFBBF24, 1));
                chip.setPadding(Ui.dp(11), Ui.dp(6), Ui.dp(11), Ui.dp(6));
                chip.addView(ci, Ui.lp(Ui.dp(12), Ui.dp(12)));
                chip.addView(Ui.hspace(c, 6));
                chip.addView(Ui.text(c, "Offline mode", 11, Theme.AMBER, Theme.SANS_SB));
                warn.addView(chip);
                statusChip.addView(warn);
                railsBox.addView(Widgets.emptyState(c, "cloud-off", "You're offline",
                        "Couldn't reach AniList — check your connection and try again."));
                addFooter(railsBox, c);
            }
        });

        return sc;
    }

    /* ------------------------------ library sort ------------------------------ */

    static String sortLabel(String s) {
        if ("added".equals(s)) return "Date added";
        if ("title".equals(s)) return "Title";
        if ("score".equals(s)) return "Score";
        if ("progress".equals(s)) return "Progress";
        return "Last updated";
    }

    static void sortEntries(List<JSONObject> list, final String mode) {
        java.util.Collections.sort(list, new java.util.Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                if ("title".equals(mode))
                    return a.optString("title", "").compareToIgnoreCase(b.optString("title", ""));
                if ("score".equals(mode)) return b.optInt("score", 0) - a.optInt("score", 0);
                if ("progress".equals(mode)) return b.optInt("progress", 0) - a.optInt("progress", 0);
                long av, bv;
                if ("added".equals(mode)) {
                    av = a.optLong("addedAt", a.optLong("updatedAt"));
                    bv = b.optLong("addedAt", b.optLong("updatedAt"));
                } else {
                    av = a.optLong("updatedAt");
                    bv = b.optLong("updatedAt");
                }
                return bv > av ? 1 : bv < av ? -1 : 0;
            }
        });
    }

    private static void showSortSheet(Context c, final MainActivity app) {
        final String[][] OPTS = {{"updated", "Last updated", "clock"}, {"added", "Date added", "calendar"},
                {"title", "Title A-Z", "type"}, {"score", "Your score", "star"}, {"progress", "Progress", "trending"}};
        final android.widget.FrameLayout overlay = new android.widget.FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(8), Ui.dp(12), Ui.dp(8), Ui.dp(10));
        TextView tt = Ui.text(c, "Sort library by", 13, Theme.MUT, Theme.SANS_SB);
        tt.setPadding(Ui.dp(14), 0, Ui.dp(14), Ui.dp(8));
        sheet.addView(tt);
        String cur = app.store.getS("librarySort", "updated");
        for (int i = 0; i < OPTS.length; i++) {
            final String id = OPTS[i][0];
            boolean active = id.equals(cur);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
            item.setBackground(active ? Ui.rounded(Theme.ACC_SOFT, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, OPTS[i][2], 15, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            item.addView(Ui.text(c, OPTS[i][1], 13.5f, active ? Theme.ACC : Theme.TXT, Theme.SANS_SB));
            if (active) {
                View spr = new View(c);
                LinearLayout.LayoutParams wp = Ui.lp(0, 1);
                wp.weight = 1;
                item.addView(spr, wp);
                item.addView(new Icons(c, "check", 14, Theme.ACC), Ui.lp(Ui.dp(14), Ui.dp(14)));
            }
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.store.put("librarySort", id);
                    ViewGroup p = (ViewGroup) overlay.getParent();
                    if (p != null) p.removeView(overlay);
                    app.rebuildContent();
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        android.widget.FrameLayout.LayoutParams shp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    static int gridColumns(Context c, MainActivity app) {
        boolean compact = "compact".equals(app.store.getS("density", "cozy"));
        int screenW = (int) (c.getResources().getDisplayMetrics().widthPixels / Ui.density);
        int target = compact ? 112 : 142;
        return Math.max(2, (screenW - 32) / (target + 14));
    }

    private static void addRail(LinearLayout box, Context c, MainActivity app, String icon,
                                String title, String sub, JSONObject d, String key, int cardW, Cards.OnMedia open) {
        JSONObject page = d.optJSONObject(key);
        if (page == null) return;
        JSONArray media = page.optJSONArray("media");
        if (media == null || media.length() == 0) return;
        box.addView(Cards.rail(c, app, icon, title, sub, media, cardW, open));
        box.addView(Ui.space(c, 30));
    }

    private static void addFooter(LinearLayout box, Context c) {
        LinearLayout foot = Ui.row(c);
        foot.setGravity(Gravity.CENTER);
        foot.addView(new Icons(c, "clock", 12, Theme.MUT), Ui.lp(Ui.dp(12), Ui.dp(12)));
        foot.addView(Ui.hspace(c, 7));
        foot.addView(Ui.text(c, "Data by the AniList API · Anisora is a demo client", 11, Theme.MUT, Theme.SANS));
        box.addView(foot, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 8, 0, 0));
    }
}
