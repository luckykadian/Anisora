package app.anisora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Search — live AniList search, sectioned catalog browse, catalog sort popup, 18+ toggle. */
public class SearchScreen {

    /** Sort options for the catalog popup (AniList MediaSort values). */
    static final String[][] CATALOG_SORTS = {
            {"sections", "Default (sections)", "grid", ""},
            {"trending", "Trending", "flame", "TRENDING_DESC"},
            {"popularity", "Popularity", "trending", "POPULARITY_DESC"},
            {"score", "Top score", "star", "SCORE_DESC"},
            {"favourites", "Most favourited", "heart", "FAVOURITES_DESC"},
            {"newest", "Newest first", "calendar", "START_DATE_DESC"},
            {"oldest", "Oldest first", "clock", "START_DATE"},
            {"title", "Title A-Z", "type", "TITLE_ROMAJI"},
            {"added", "Recently added", "plus", "ID_DESC"},
    };

    public static View build(final Context c, final MainActivity app, final String q) {
        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout col = Ui.col(c);
        final int pad = 16;
        col.setPadding(Ui.dp(pad), Ui.dp(20), Ui.dp(pad), Ui.dp(110));
        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final boolean hasQuery = q != null && q.trim().length() > 0;
        final boolean adult = app.store.getB("adultOnly", false);
        final String sortId = app.store.getS("catalogSort", "sections");
        final String sortScope = app.store.getS("catalogScope", "BOTH");

        /* ---------- header ---------- */
        LinearLayout headTop = Ui.row(c);
        TextView kicker = Ui.mono(c, hasQuery ? "Search AniList" : "AniList catalog", 10, Theme.MUT);
        LinearLayout.LayoutParams kp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.weight = 1;
        headTop.addView(kicker, kp);

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
        } else if (!"sections".equals(sortId)) {
            h1.addView(Ui.text(c, "Sorted by ", 24, Theme.TXT, Theme.DISP_BOLD));
            h1.addView(Ui.text(c, sortName(sortId).toLowerCase(), 24, Theme.ACC, Theme.DISP_BOLD));
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

        if (hasQuery) {
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

        if (!"sections".equals(sortId)) {
            /* ---------- sorted catalog (sort popup applied) ---------- */
            String aniSort = sortValue(sortId);
            final boolean both = "BOTH".equals(sortScope);
            final String first = both ? "ANIME" : sortScope;
            Api.browseSorted(first, aniSort, adult, new Api.Cb() {
                public void ok(JSONObject d) {
                    if (!sc.isAttachedToWindow()) return;
                    results.removeAllViews();
                    addSortedGrid(c, app, results, "ANIME".equals(first) ? "film" : "book",
                            ("ANIME".equals(first) ? "Anime" : "Manga") + " · " + sortName(sortId), d, pad, open);
                    if (both) {
                        Api.browseSorted("MANGA", sortValue(sortId), adult, new Api.Cb() {
                            public void ok(JSONObject d2) {
                                if (!sc.isAttachedToWindow()) return;
                                results.addView(Ui.space(c, 28));
                                addSortedGrid(c, app, results, "book", "Manga · " + sortName(sortId), d2, pad, open);
                            }

                            public void fail(Exception e) {
                            }
                        });
                    }
                }

                public void fail(Exception e) {
                    offline(c, results, sc);
                }
            });
            return sc;
        }

        /* ---------- default: sectioned catalog (like Home, minus the library) ---------- */
        if ("ALL".equals(tab) || "ANIME".equals(tab)) {
            Api.fetchHome("ANIME", new Api.Cb() {
                public void ok(JSONObject d) {
                    if (!sc.isAttachedToWindow()) return;
                    results.removeAllViews();
                    addSections(c, app, results, d, true, open);
                    if ("ALL".equals(tab)) {
                        Api.fetchHome("MANGA", new Api.Cb() {
                            public void ok(JSONObject d2) {
                                if (!sc.isAttachedToWindow()) return;
                                addSections(c, app, results, d2, false, open);
                            }

                            public void fail(Exception e) {
                            }
                        });
                    }
                }

                public void fail(Exception e) {
                    offline(c, results, sc);
                }
            });
        } else {
            Api.fetchHome("MANGA", new Api.Cb() {
                public void ok(JSONObject d) {
                    if (!sc.isAttachedToWindow()) return;
                    results.removeAllViews();
                    addSections(c, app, results, d, false, open);
                }

                public void fail(Exception e) {
                    offline(c, results, sc);
                }
            });
        }
        return sc;
    }

    /* ------------------------- catalog sort popup (header) ------------------------- */

    static String sortName(String id) {
        for (int i = 0; i < CATALOG_SORTS.length; i++)
            if (CATALOG_SORTS[i][0].equals(id)) return CATALOG_SORTS[i][1];
        return "Trending";
    }

    static String sortValue(String id) {
        for (int i = 0; i < CATALOG_SORTS.length; i++)
            if (CATALOG_SORTS[i][0].equals(id)) return CATALOG_SORTS[i][3];
        return "TRENDING_DESC";
    }

    /** Popup from the header sort button: scope (anime/manga/both) + all sort options. */
    public static void showCatalogSortSheet(final MainActivity app) {
        final Context c = app;
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        ScrollView scr = new ScrollView(c);
        scr.setVerticalScrollBarEnabled(false);
        LinearLayout sheet = Ui.col(c);
        sheet.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        sheet.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(12));

        sheet.addView(Ui.text(c, "Sort catalog", 16, Theme.TXT, Theme.DISP_BOLD));
        TextView sub = Ui.text(c, "Applies to the Search page browse view", 11, Theme.MUT, Theme.SANS);
        sub.setPadding(0, Ui.dp(3), 0, Ui.dp(12));
        sheet.addView(sub);

        sheet.addView(Ui.text(c, "Apply to", 12, Theme.MUT, Theme.SANS_SB));
        sheet.addView(Ui.space(c, 7));
        sheet.addView(Widgets.seg(c, new String[][]{{"BOTH", "Both"}, {"ANIME", "Anime"}, {"MANGA", "Manga"}},
                app.store.getS("catalogScope", "BOTH"), new Widgets.OnSeg() {
                    public void pick(String id) {
                        app.store.put("catalogScope", id);
                        dismiss(overlay);
                        showCatalogSortSheet(app); // re-open with new scope highlighted
                    }
                }));
        sheet.addView(Ui.space(c, 12));
        sheet.addView(Ui.text(c, "Sort by", 12, Theme.MUT, Theme.SANS_SB));
        sheet.addView(Ui.space(c, 4));

        String cur = app.store.getS("catalogSort", "sections");
        for (int i = 0; i < CATALOG_SORTS.length; i++) {
            final String id = CATALOG_SORTS[i][0];
            boolean active = id.equals(cur);
            LinearLayout item = Ui.row(c);
            item.setPadding(Ui.dp(12), Ui.dp(11), Ui.dp(12), Ui.dp(11));
            item.setBackground(active ? Ui.rounded(Theme.ACC_SOFT, 12, 0, 0) : Ui.rounded(0x00000000, 12, 0, 0));
            item.addView(new Icons(c, CATALOG_SORTS[i][2], 15, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(15), Ui.dp(15)));
            item.addView(Ui.hspace(c, 11));
            item.addView(Ui.text(c, CATALOG_SORTS[i][1], 13.5f, active ? Theme.ACC : Theme.TXT, Theme.SANS_SB));
            if (active) {
                View spr = new View(c);
                LinearLayout.LayoutParams wp = Ui.lp(0, 1);
                wp.weight = 1;
                item.addView(spr, wp);
                item.addView(new Icons(c, "check", 14, Theme.ACC), Ui.lp(Ui.dp(14), Ui.dp(14)));
            }
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    app.store.put("catalogSort", id);
                    dismiss(overlay);
                    if (!"search".equals(app.route)) app.route = "search";
                    app.rebuildContent();
                }
            });
            sheet.addView(item, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        scr.addView(sheet);
        FrameLayout.LayoutParams shp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shp.gravity = Gravity.BOTTOM;
        shp.setMargins(Ui.dp(12), Ui.dp(60), Ui.dp(12), Ui.dp(20));
        overlay.addView(scr, shp);
        if (!Theme.REDUCE_MOTION) {
            scr.setTranslationY(Ui.dp(30));
            scr.animate().translationY(0).setDuration(200).start();
        }
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss(overlay);
            }
        });
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void dismiss(FrameLayout overlay) {
        ViewGroup p = (ViewGroup) overlay.getParent();
        if (p != null) p.removeView(overlay);
    }

    /* --------------------------------- helpers --------------------------------- */

    private static void addSections(Context c, MainActivity app, LinearLayout box, JSONObject d,
                                    boolean isAnime, Cards.OnMedia open) {
        int cardW = "compact".equals(app.store.getS("density", "cozy")) ? 118 : 136;
        if (box.getChildCount() > 0) box.addView(Ui.space(c, 8));
        addRail(box, c, app, "flame", isAnime ? "Trending anime" : "Trending manga",
                isAnime ? "What everyone is watching" : "Hot on the charts", d, "trending", cardW, open);
        addRail(box, c, app, "trending",
                isAnime ? "Popular this " + Api.seasonLabel(Api.seasonNow()).toLowerCase() : "All-time favourites",
                isAnime ? Api.seasonLabel(Api.seasonNow()) + " " + Api.yearNow() + " season" : "Most loved by the community",
                d, "seasonal", cardW, open);
        addRail(box, c, app, "trophy", "Top rated", "Critics' darlings", d, "top", cardW, open);
        addRail(box, c, app, "heart", "Community favourites", null, d, "loved", cardW, open);
    }

    private static void addRail(LinearLayout box, Context c, MainActivity app, String icon, String title,
                                String sub, JSONObject d, String key, int cardW, Cards.OnMedia open) {
        JSONObject page = d.optJSONObject(key);
        if (page == null) return;
        JSONArray media = page.optJSONArray("media");
        if (media == null || media.length() == 0) return;
        LinearLayout rail = Cards.rail(c, app, icon, title, sub, media, cardW, open);
        box.addView(rail);
        box.addView(Ui.space(c, 28));
        Ui.appear(rail, 30);
    }

    private static void addSortedGrid(Context c, MainActivity app, LinearLayout box, String icon,
                                      String title, JSONObject d, int pad, Cards.OnMedia open) {
        JSONObject page = d.optJSONObject("Page");
        JSONArray media = page != null ? page.optJSONArray("media") : null;
        if (media == null || media.length() == 0) return;
        box.addView(Widgets.sectionHead(c, icon, title, media.length() + " titles"));
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
