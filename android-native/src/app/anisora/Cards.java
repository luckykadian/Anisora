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
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** MediaCard + Rail, mirroring src/components/MediaCard.tsx and Home Rail. */
public class Cards {

    public interface OnMedia {
        void open(JSONObject media);
    }

    /** 2:3 poster card with format badge, score pill, progress bar + title/meta. */
    public static LinearLayout mediaCard(final Context c, final MainActivity app,
                                         final JSONObject m, JSONObject entry, int widthDp,
                                         final OnMedia onOpen) {
        final String titleLang = app.store.getS("titleLang", "romaji");
        final String title = Api.titleOf(m, titleLang);
        JSONObject e = entry;
        if (e == null) e = app.store.entry(m.optInt("id"));

        LinearLayout root = Ui.col(c);
        root.setLayoutParams(new ViewGroup.LayoutParams(Ui.dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT));

        /* ------- poster ------- */
        FrameLayout poster = new FrameLayout(c);
        poster.setBackground(Ui.rounded(Theme.BG2, Theme.RADIUS, Theme.LINE, 1));
        Widgets.clipRounded(poster, Theme.RADIUS);

        JSONObject cover = m.optJSONObject("coverImage");
        String color = cover != null ? cover.optString("color", null) : null;
        String url = cover != null ? cover.optString("large", null) : null;
        if (url == null || "null".equals(url)) url = cover != null ? cover.optString("medium", null) : null;

        ImageView img = new ImageView(c);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackground(Widgets.posterFallback("null".equals(color) ? null : color, title));
        poster.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (url != null && !"null".equals(url)) Images.load(url, img, Ui.dp(widthDp));

        // bottom scrim
        View scrim = new View(c);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xBF000000, 0x33000000, 0x00000000});
        scrim.setBackground(g);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp((int) (widthDp * 1.5f * 0.4f)));
        sp.gravity = Gravity.BOTTOM;
        poster.addView(scrim, sp);

        // format badge
        String format = m.optString("format", null);
        if (format != null && !"null".equals(format)) {
            TextView badge = Ui.text(c, Api.formatLabel(format).toUpperCase(), 8.5f, 0xD9FFFFFF, Theme.MONO_BOLD);
            badge.setLetterSpacing(0.08f);
            badge.setBackground(Ui.rounded(0x8C000000, 7, 0x1AFFFFFF, 1));
            badge.setPadding(Ui.dp(6), Ui.dp(2), Ui.dp(6), Ui.dp(2));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bp.gravity = Gravity.TOP | Gravity.LEFT;
            bp.setMargins(Ui.dp(8), Ui.dp(8), 0, 0);
            poster.addView(badge, bp);
        }

        // score pill
        int score = m.optInt("averageScore", 0);
        if (score > 0) {
            LinearLayout pill = Widgets.scorePill(c, score);
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            pp.setMargins(0, 0, Ui.dp(8), Ui.dp(8));
            poster.addView(pill, pp);
        }

        // +1 button for tracked, non-completed entries
        final JSONObject fe = e;
        if (fe != null && !"COMPLETED".equals(fe.optString("status"))) {
            FrameLayout plus = new FrameLayout(c);
            plus.setBackground(Ui.rounded(0x99000000, 999, 0x26FFFFFF, 1));
            Icons pi = new Icons(c, "plus", 14, 0xFFFFFFFF, 2.4f);
            FrameLayout.LayoutParams pip = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
            pip.gravity = Gravity.CENTER;
            plus.addView(pi, pip);
            FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.dp(28), Ui.dp(28));
            plp.gravity = Gravity.TOP | Gravity.RIGHT;
            plp.setMargins(0, Ui.dp(8), Ui.dp(8), 0);
            poster.addView(plus, plp);
            plus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    JSONObject after = app.store.bump(m.optInt("id"));
                    if (after != null) {
                        Anilist.push(app, after);
                        String unit = "MANGA".equals(after.optString("type")) ? "Ch." : "Ep.";
                        String tt = title.length() > 22 ? title.substring(0, 22) + "…" : title;
                        app.toast(unit + " " + after.optInt("progress") + " logged — " + tt, "check");
                    }
                }
            });
        }

        // progress bar
        if (fe != null) {
            int total = fe.optInt("total", -1);
            int aired = 0;
            JSONObject nae = m.optJSONObject("nextAiringEpisode");
            if (nae != null) aired = nae.optInt("episode", 1) - 1;
            int progTotal = total > 0 ? total
                    : m.optInt("episodes", 0) > 0 ? m.optInt("episodes")
                    : m.optInt("chapters", 0) > 0 ? m.optInt("chapters")
                    : aired > 0 ? Math.max(aired, fe.optInt("progress")) : fe.optInt("progress");
            if (progTotal > 0) {
                float pct = Math.min(1f, fe.optInt("progress") / (float) progTotal);
                FrameLayout barBg = new FrameLayout(c);
                barBg.setBackgroundColor(0x80000000);
                View bar = new View(c);
                bar.setBackground(Ui.rounded(Theme.ACC, 2, 0, 0));
                FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams((int) (Ui.dp(widthDp) * pct), Ui.dp(4));
                barBg.addView(bar, fp);
                FrameLayout.LayoutParams bp2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(4));
                bp2.gravity = Gravity.BOTTOM;
                poster.addView(barBg, bp2);
            }
        }

        root.addView(poster, Ui.lp(Ui.dp(widthDp), Ui.dp((int) (widthDp * 1.5f))));

        /* ------- text ------- */
        TextView t = Ui.oneLine(Ui.text(c, title, 12.5f, Theme.TXT, Theme.SANS_SB));
        t.setPadding(Ui.dp(2), Ui.dp(8), Ui.dp(2), 0);
        root.addView(t);

        LinearLayout meta = Ui.row(c);
        meta.setPadding(Ui.dp(2), Ui.dp(4), Ui.dp(2), 0);
        String status = m.optString("status", null);
        if (status != null && !"null".equals(status)) {
            View dot = new View(c);
            dot.setBackground(Ui.circle(Api.statusDot(status)));
            meta.addView(dot, Ui.lpm(Ui.dp(6), Ui.dp(6), 0, 0, 6, 0));
        }
        StringBuilder ms = new StringBuilder();
        String fl = Api.formatLabel(m.optString("format", null));
        if (fl != null && !"null".equals(fl)) ms.append(fl);
        int year = m.optInt("seasonYear", 0);
        if (year > 0) {
            if (ms.length() > 0) ms.append(" · ");
            ms.append(year);
        }
        if (ms.length() == 0 && fe != null) {
            int tt = fe.optInt("total", -1);
            ms.append(fe.optInt("progress")).append("/").append(tt > 0 ? String.valueOf(tt) : "?");
        }
        if (ms.length() == 0 && status != null && !"null".equals(status)) ms.append(Api.statusLabel(status));
        if (ms.length() == 0) ms.append("—");
        TextView mv = Ui.oneLine(Ui.text(c, ms.toString(), 10.5f, Theme.MUT, Theme.SANS_MED));
        meta.addView(mv);
        root.addView(meta);

        root.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onOpen.open(m);
            }
        });
        Ui.press(root);
        return root;
    }

    /** Horizontal rail with section head. */
    public static LinearLayout rail(Context c, MainActivity app, String iconName, String title,
                                    String sub, JSONArray items, int cardW, OnMedia onOpen) {
        LinearLayout section = Ui.col(c);
        if (items == null || items.length() == 0) return section;
        section.addView(Widgets.sectionHead(c, iconName, title, sub));
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < items.length(); i++) {
            JSONObject m = items.optJSONObject(i);
            if (m == null) continue;
            LinearLayout card = mediaCard(c, app, m, null, cardW, onOpen);
            row.addView(card, Ui.lpm(Ui.dp(cardW), ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 14, 0));
        }
        hs.addView(row);
        section.addView(hs);
        return section;
    }

    /** Grid of cards (library / search results). */
    public static LinearLayout grid(Context c, MainActivity app, JSONArray items,
                                    java.util.List<JSONObject> entries, int columns, int screenPadDp, OnMedia onOpen) {
        LinearLayout col = Ui.col(c);
        int gap = 14;
        int screenW = (int) (c.getResources().getDisplayMetrics().widthPixels / Ui.density);
        int cardW = (screenW - screenPadDp * 2 - gap * (columns - 1)) / columns;
        LinearLayout row = null;
        int inRow = 0;
        int n = items != null ? items.length() : entries.size();
        for (int i = 0; i < n; i++) {
            JSONObject m;
            JSONObject e = null;
            if (items != null) {
                m = items.optJSONObject(i);
            } else {
                e = entries.get(i);
                m = entryToMedia(e);
            }
            if (m == null) continue;
            if (row == null || inRow >= columns) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                col.addView(row, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, inRow == 0 && col.getChildCount() == 0 ? 0 : 18, 0, 0));
                inRow = 0;
            }
            LinearLayout card = mediaCard(c, app, m, e, cardW, onOpen);
            row.addView(card, Ui.lpm(Ui.dp(cardW), ViewGroup.LayoutParams.WRAP_CONTENT, inRow == 0 ? 0 : gap, 0, 0, 0));
            inRow++;
        }
        return col;
    }

    /** Convert a library entry into a media-shaped object (entryToMedia in Home.tsx). */
    public static JSONObject entryToMedia(JSONObject e) {
        try {
            JSONObject m = new JSONObject();
            m.put("id", e.optInt("id"));
            m.put("type", e.optString("type"));
            JSONObject t = new JSONObject();
            t.put("romaji", e.optString("title"));
            m.put("title", t);
            JSONObject cov = new JSONObject();
            if (e.has("cover")) cov.put("large", e.optString("cover"));
            if (e.has("color")) cov.put("color", e.optString("color"));
            m.put("coverImage", cov);
            if ("MANGA".equals(e.optString("type"))) m.put("format", "MANGA");
            return m;
        } catch (Exception ex) {
            return e;
        }
    }
}
