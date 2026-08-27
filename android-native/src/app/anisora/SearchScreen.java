package app.anisora;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Search — live AniList search + default catalog browse + adult-only toggle. */
public class SearchScreen {

    public static View build(final Context c, final MainActivity app, final String q) {
        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout col = Ui.col(c);
        final int pad = 16;
        col.setPadding(Ui.dp(pad), Ui.dp(20), Ui.dp(pad), Ui.dp(110));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final boolean hasQuery = q != null && q.trim().length() > 0;
        final boolean adult = app.store.getB("adultOnly", false);

        /* ---------- header ---------- */
        LinearLayout headTop = Ui.row(c);
        TextView kicker = Ui.mono(c, hasQuery ? "Search AniList" : "AniList catalog", 10, Theme.MUT);
        LinearLayout.LayoutParams kp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.weight = 1;
        headTop.addView(kicker, kp);

        // adult-only toggle chip (annotated request)
        LinearLayout adultChip = Ui.row(c);
        adultChip.setBackground(adult
                ? Ui.ripple(Ui.rounded(0x26FB7185, 999, 0x66FB7185, 1), 0x33FB7185)
                : Ui.ripple(Ui.rounded(Theme.BG1, 999, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        adultChip.setPadding(Ui.dp(11), Ui.dp(6), Ui.dp(11), Ui.dp(6));
        adultChip.addView(Ui.text(c, "18+", 10.5f, adult ? Theme.ROSE : Theme.MUT, Theme.MONO_BOLD));
        adultChip.addView(Ui.hspace(c, 5));
        adultChip.addView(Ui.text(c, adult ? "Adult only" : "Adult", 11, adult ? Theme.ROSE : Theme.MUT, Theme.SANS_SB));
        adultChip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean on = !app.store.getB("adultOnly", false);
                app.store.put("adultOnly", on);
                app.toast(on ? "Showing only adult content" : "Adult-only filter off", on ? "info" : "check");
                app.rebuildContent();
            }
        });
        headTop.addView(adultChip);
        col.addView(headTop);
        col.addView(Ui.space(c, 7));

        LinearLayout h1 = Ui.row(c);
        if (hasQuery) {
            h1.addView(Ui.text(c, "Results for ", 24, Theme.TXT, Theme.DISP_BOLD));
            h1.addView(Ui.oneLine(Ui.text(c, "\u201C" + q.trim() + "\u201D", 24, Theme.ACC, Theme.DISP_BOLD)));
        } else {
            h1.addView(Ui.text(c, "Browse the ", 24, Theme.TXT, Theme.DISP_BOLD));
            h1.addView(Ui.text(c, "catalog", 24, Theme.ACC, Theme.DISP_BOLD));
        }
        col.addView(h1);
        col.addView(Ui.space(c, 14));

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

        final LinearLayout results = Ui.col(c);
        col.addView(results);
        addSkeletonGrid(c, app, results, pad);

        if (!hasQuery) {
            /* ---------- default: AniList catalog (annotated request) ---------- */
            if ("ALL".equals(tab)) {
                Api.browse("ANIME", adult, new Api.Cb() {
                    public void ok(JSONObject d) {
                        if (!sc.isAttachedToWindow()) return;
                        results.removeAllViews();
                        addCatalogSection(c, app, results, "film", "Anime catalog", d, pad, open);
                        Api.browse("MANGA", adult, new Api.Cb() {
                            public void ok(JSONObject d2) {
                                if (!sc.isAttachedToWindow()) return;
                                results.addView(Ui.space(c, 28));
                                addCatalogSection(c, app, results, "book", "Manga catalog", d2, pad, open);
                            }

                            public void fail(Exception e) {
                            }
                        });
                    }

                    public void fail(Exception e) {
                        offline(c, results, sc);
                    }
                });
            } else {
                Api.browse(tab, adult, new Api.Cb() {
                    public void ok(JSONObject d) {
                        if (!sc.isAttachedToWindow()) return;
                        results.removeAllViews();
                        addCatalogSection(c, app, results, "ANIME".equals(tab) ? "film" : "book",
                                ("ANIME".equals(tab) ? "Anime" : "Manga") + " catalog", d, pad, open);
                    }

                    public void fail(Exception e) {
                        offline(c, results, sc);
                    }
                });
            }
            return sc;
        }

        /* ---------- live search ---------- */
        String type = "ALL".equals(tab) ? null : tab;
        Api.search(q.trim(), type, app.store.getB("nsfw", false), adult, new Api.Cb() {
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
                offline(c, results, sc);
            }
        });

        return sc;
    }

    private static void addCatalogSection(Context c, MainActivity app, LinearLayout box, String icon,
                                          String title, JSONObject d, int pad, Cards.OnMedia open) {
        JSONObject page = d.optJSONObject("Page");
        JSONArray media = page != null ? page.optJSONArray("media") : null;
        if (media == null || media.length() == 0) return;
        box.addView(Widgets.sectionHead(c, icon, title, "Trending on AniList right now"));
        View grid = Cards.grid(c, app, media, null, HomeScreen.gridColumns(c, app), pad, open);
        box.addView(grid);
        Ui.appear(grid, 40);
    }

    private static void addSkeletonGrid(Context c, MainActivity app, LinearLayout box, int pad) {
        int cols = HomeScreen.gridColumns(c, app);
        int gap = 14;
        int screenW = (int) (c.getResources().getDisplayMetrics().widthPixels / Ui.density);
        int cardW = (screenW - pad * 2 - gap * (cols - 1)) / cols;
        LinearLayout skRow = null;
        for (int i = 0; i < 6; i++) {
            if (i % cols == 0) {
                skRow = Ui.row(c);
                box.addView(skRow, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 16, 0, 0));
            }
            skRow.addView(Widgets.skel(c, Theme.RADIUS), Ui.lpm(Ui.dp(cardW), Ui.dp((int) (cardW * 1.5f)), i % cols == 0 ? 0 : gap, 0, 0, 0));
        }
    }

    private static void offline(Context c, LinearLayout results, ScrollView sc) {
        if (!sc.isAttachedToWindow()) return;
        results.removeAllViews();
        results.addView(Widgets.emptyState(c, "cloud-off", "AniList unavailable",
                "Couldn't reach AniList — check your connection."));
    }
}
