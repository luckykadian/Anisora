package app.anisora;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Search — mirrors SearchView in src/components/Home.tsx. */
public class SearchScreen {

    public static View build(final Context c, final MainActivity app, final String q) {
        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout col = Ui.col(c);
        final int pad = 16;
        col.setPadding(Ui.dp(pad), Ui.dp(20), Ui.dp(pad), Ui.dp(110));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(Ui.mono(c, "Search AniList", 10, Theme.MUT));
        col.addView(Ui.space(c, 7));

        LinearLayout h1 = Ui.row(c);
        if (q != null && q.trim().length() > 0) {
            h1.addView(Ui.text(c, "Results for ", 24, Theme.TXT, Theme.DISP_BOLD));
            TextView qt = Ui.oneLine(Ui.text(c, "\u201C" + q.trim() + "\u201D", 24, Theme.ACC, Theme.DISP_BOLD));
            h1.addView(qt);
        } else {
            h1.addView(Ui.text(c, "Type to search", 24, Theme.TXT, Theme.DISP_BOLD));
        }
        col.addView(h1);
        col.addView(Ui.space(c, 14));

        // tab seg
        final String tab = app.searchTab;
        col.addView(Widgets.seg(c, new String[][]{{"ALL", "All"}, {"ANIME", "Anime"}, {"MANGA", "Manga"}}, tab,
                new Widgets.OnSeg() {
                    public void pick(String id) {
                        app.searchTab = id;
                        app.rebuildContent();
                    }
                }));
        col.addView(Ui.space(c, 20));

        final Cards.OnMedia open = new Cards.OnMedia() {
            public void open(JSONObject m) {
                app.openDetail(m.optInt("id"), m);
            }
        };

        if (q == null || q.trim().length() == 0) {
            col.addView(Widgets.emptyState(c, "search", "Search the whole AniList database",
                    "Try \u201CFrieren\u201D, \u201CJujutsu Kaisen\u201D, \u201CBerserk\u201D…"));
            return sc;
        }

        // skeleton grid while loading
        final LinearLayout results = Ui.col(c);
        col.addView(results);
        int cols = HomeScreen.gridColumns(c, app);
        int gap = 14;
        int screenW = (int) (c.getResources().getDisplayMetrics().widthPixels / Ui.density);
        int cardW = (screenW - pad * 2 - gap * (cols - 1)) / cols;
        LinearLayout skRow = null;
        for (int i = 0; i < 6; i++) {
            if (i % cols == 0) {
                skRow = Ui.row(c);
                results.addView(skRow, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 16, 0, 0));
            }
            View s = Widgets.skel(c, Theme.RADIUS);
            skRow.addView(s, Ui.lpm(Ui.dp(cardW), Ui.dp((int) (cardW * 1.5f)), i % cols == 0 ? 0 : gap, 0, 0, 0));
        }

        String type = "ALL".equals(tab) ? null : tab;
        Api.search(q.trim(), type, app.store.getB("nsfw", false), new Api.Cb() {
            public void ok(JSONObject d) {
                if (!sc.isAttachedToWindow()) return;
                results.removeAllViews();
                JSONObject page = d.optJSONObject("Page");
                JSONArray media = page != null ? page.optJSONArray("media") : null;
                if (media == null || media.length() == 0) {
                    results.addView(Widgets.emptyState(c, "search", "No results",
                            "Try a different spelling, or check the Anime/Manga filter."));
                    return;
                }
                results.addView(Cards.grid(c, app, media, null, HomeScreen.gridColumns(c, app), pad, open));
            }

            public void fail(Exception e) {
                if (!sc.isAttachedToWindow()) return;
                results.removeAllViews();
                results.addView(Widgets.emptyState(c, "cloud-off", "Search unavailable",
                        "Couldn't reach AniList — check your connection."));
            }
        });

        return sc;
    }
}
