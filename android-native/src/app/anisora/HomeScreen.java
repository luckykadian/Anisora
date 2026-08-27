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
