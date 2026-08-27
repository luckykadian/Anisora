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

/** Character / Staff page — replica of src/components/PersonOverlay.tsx. */
public class PersonScreen {

    public static void open(final Context c, final MainActivity app, final String kind,
                            final int id, final String seedName, final String seedImage, final String role) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0xA6000000);
        overlay.setClickable(true);

        final ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        final LinearLayout card = Ui.col(c);
        card.setBackground(Ui.rounded(Theme.BG1, 24, Theme.LINE, 1));
        Widgets.clipRounded(card, 24);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(Ui.dp(12), Ui.dp(46), Ui.dp(12), Ui.dp(24));
        LinearLayout wrap = Ui.col(c);
        wrap.addView(card);
        sc.addView(wrap, cp);
        overlay.addView(sc, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss(overlay);
            }
        });
        card.setClickable(true);

        renderSkeleton(c, card, seedName, seedImage, kind, role, overlay);

        Api.Cb cb = new Api.Cb() {
            public void ok(JSONObject d) {
                JSONObject p = d.optJSONObject("character".equals(kind) ? "Character" : "Staff");
                if (p == null || !overlay.isAttachedToWindow()) return;
                card.removeAllViews();
                render(c, app, card, p, kind, role, overlay);
            }

            public void fail(Exception e) {
                if (!overlay.isAttachedToWindow()) return;
                card.removeAllViews();
                LinearLayout pad = Ui.col(c);
                pad.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
                pad.addView(Widgets.emptyState(c, "cloud-off", "Couldn't load this person",
                        "Check your connection and try again."));
                card.addView(pad);
            }
        };
        if ("character".equals(kind)) Api.fetchCharacter(id, cb);
        else Api.fetchStaff(id, cb);

        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!Theme.REDUCE_MOTION) {
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(180).start();
            card.setTranslationY(Ui.dp(24));
            card.animate().translationY(0).setDuration(240).start();
        }
    }

    private static void dismiss(final FrameLayout overlay) {
        ViewGroup p = (ViewGroup) overlay.getParent();
        if (p != null) p.removeView(overlay);
    }

    private static void renderSkeleton(Context c, LinearLayout card, String name, String image,
                                       String kind, String role, FrameLayout overlay) {
        LinearLayout pad = Ui.col(c);
        pad.setPadding(Ui.dp(18), Ui.dp(46), Ui.dp(18), Ui.dp(24));
        View s1 = Widgets.skel(c, 20);
        pad.addView(s1, Ui.lp(Ui.dp(110), Ui.dp(110)));
        View s2 = Widgets.skel(c, 6);
        pad.addView(s2, Ui.lpm(Ui.dp(190), Ui.dp(22), 0, 14, 0, 0));
        View s3 = Widgets.skel(c, 6);
        pad.addView(s3, Ui.lpm(Ui.dp(130), Ui.dp(13), 0, 8, 0, 0));
        card.addView(pad);
    }

    private static void render(final Context c, final MainActivity app, LinearLayout card,
                               JSONObject p, String kind, String role, final FrameLayout overlay) {
        boolean isChar = "character".equals(kind);
        JSONObject nameObj = p.optJSONObject("name");
        String name = nameObj != null ? nameObj.optString("full", "—") : "—";
        String nativeName = nameObj != null ? nameObj.optString("native", null) : null;
        int hue = Ui.hashHue(name);

        /* ---------- header band + close ---------- */
        FrameLayout headBand = new FrameLayout(c);
        GradientDrawable band = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.alpha(Ui.hsl(hue, 0.55f, 0.35f), 217), Theme.alpha(Ui.hsl((hue + 60) % 360, 0.45f, 0.2f), 90), 0x00000000});
        headBand.setBackground(band);

        FrameLayout close = new FrameLayout(c);
        close.setBackground(Ui.rounded(0x66000000, 999, 0x26FFFFFF, 1));
        Icons xi = new Icons(c, "x", 14, 0xFFFFFFFF);
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
        xp.gravity = Gravity.CENTER;
        close.addView(xi, xp);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.dp(34), Ui.dp(34));
        clp.gravity = Gravity.TOP | Gravity.RIGHT;
        clp.setMargins(0, Ui.dp(12), Ui.dp(12), 0);
        headBand.addView(close, clp);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss(overlay);
            }
        });

        LinearLayout headContent = Ui.col(c);
        headContent.setPadding(Ui.dp(18), Ui.dp(40), Ui.dp(18), 0);

        FrameLayout imgBox = new FrameLayout(c);
        imgBox.setBackground(Ui.rounded(Theme.BG2, 22, Theme.BG1, 4));
        Widgets.clipRounded(imgBox, 22);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(Widgets.posterFallback(null, name));
        imgBox.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        JSONObject img = p.optJSONObject("image");
        if (img != null) Images.load(img.optString("large", null), iv, Ui.dp(120));
        headContent.addView(imgBox, Ui.lp(Ui.dp(112), Ui.dp(112)));

        LinearLayout kindRow = Ui.row(c);
        kindRow.setPadding(0, Ui.dp(14), 0, 0);
        kindRow.addView(new Icons(c, isChar ? "user" : "users", 11, Theme.ACC), Ui.lp(Ui.dp(11), Ui.dp(11)));
        kindRow.addView(Ui.hspace(c, 6));
        TextView kt = Ui.mono(c, isChar ? "Character" : "Staff", 9, Theme.ACC);
        kindRow.addView(kt);
        if (role != null && role.length() > 0) {
            kindRow.addView(Ui.hspace(c, 6));
            String rl = "MAIN".equals(role) ? "Main role" : "SUPPORTING".equals(role) ? "Supporting role" : role;
            kindRow.addView(Ui.text(c, "· " + rl, 10.5f, Theme.MUT, Theme.SANS_MED));
        }
        headContent.addView(kindRow);

        TextView nameT = Ui.text(c, name, 25, Theme.TXT, Theme.DISP_BOLD);
        nameT.setPadding(0, Ui.dp(6), 0, 0);
        headContent.addView(nameT);
        if (nativeName != null && !"null".equals(nativeName) && nativeName.length() > 0) {
            TextView natT = Ui.text(c, nativeName, 13, Theme.MUT, Theme.SANS);
            natT.setPadding(0, Ui.dp(4), 0, 0);
            headContent.addView(natT);
        }

        headBand.addView(headContent, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(headBand);

        /* ---------- facts ---------- */
        LinearLayout facts = Ui.row(c);
        facts.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), 0);
        addFact(c, facts, "heart", Api.fmt(p.optLong("favourites", 0)) + " favourites");
        String gender = p.optString("gender", null);
        if (gender != null && !"null".equals(gender)) addFact(c, facts, "user", gender);
        String age = p.optString("age", null);
        if (age != null && !"null".equals(age)) addFact(c, facts, "calendar", "Age " + age);
        if (!isChar) {
            String lang = p.optString("languageV2", null);
            if (lang != null && !"null".equals(lang)) addFact(c, facts, "mic", lang);
        }
        HorizontalScrollView fh = new HorizontalScrollView(c);
        fh.setHorizontalScrollBarEnabled(false);
        LinearLayout fwrap = Ui.col(c);
        fwrap.addView(fh);
        fh.addView(facts);
        card.addView(fwrap);

        JSONObject dob = p.optJSONObject("dateOfBirth");
        if (dob != null && !dob.isNull("month")) {
            LinearLayout b = Ui.row(c);
            b.setPadding(Ui.dp(18), Ui.dp(10), Ui.dp(18), 0);
            b.addView(new Icons(c, "calendar", 12, Theme.MUT), Ui.lp(Ui.dp(12), Ui.dp(12)));
            b.addView(Ui.hspace(c, 7));
            b.addView(Ui.text(c, "Birthday: " + Api.fmtDate(dob), 12, Theme.MUT, Theme.SANS_MED));
            card.addView(b);
        }

        /* ---------- description ---------- */
        String desc = p.optString("description", null);
        if (desc != null && !"null".equals(desc) && desc.length() > 0) {
            LinearLayout dw = Ui.col(c);
            dw.setPadding(Ui.dp(18), Ui.dp(16), Ui.dp(18), 0);
            final TextView dt = Ui.text(c, desc.replaceAll("~!|!~", "").replaceAll("__|\\*\\*", "")
                    .replaceAll("<[^>]+>", "").trim(), 12.5f, Theme.MUT, Theme.SANS);
            dt.setLineSpacing(Ui.dp(4), 1f);
            dt.setMaxLines(7);
            dt.setEllipsize(android.text.TextUtils.TruncateAt.END);
            dw.addView(dt);
            final TextView more = Ui.text(c, "Read more", 12, Theme.ACC, Theme.SANS_SB);
            more.setPadding(0, Ui.dp(7), 0, 0);
            more.setOnClickListener(new View.OnClickListener() {
                boolean open = false;

                public void onClick(View v) {
                    open = !open;
                    dt.setMaxLines(open ? 9999 : 7);
                    more.setText(open ? "Show less" : "Read more");
                }
            });
            dw.addView(more);
            card.addView(dw);
        }

        /* ---------- appearances rail ---------- */
        JSONArray media = new JSONArray();
        if (isChar) {
            JSONObject mroot = p.optJSONObject("media");
            JSONArray nodes = mroot != null ? mroot.optJSONArray("nodes") : null;
            if (nodes != null) for (int i = 0; i < nodes.length(); i++) media.put(nodes.optJSONObject(i));
        } else {
            JSONObject mroot = p.optJSONObject("staffMedia");
            JSONArray edges = mroot != null ? mroot.optJSONArray("edges") : null;
            if (edges != null) for (int i = 0; i < edges.length(); i++) {
                JSONObject e = edges.optJSONObject(i);
                JSONObject n = e != null ? e.optJSONObject("node") : null;
                if (n != null) media.put(n);
            }
        }
        if (media.length() > 0) {
            LinearLayout rw = Ui.col(c);
            rw.setPadding(Ui.dp(18), Ui.dp(20), Ui.dp(18), Ui.dp(4));
            rw.addView(Cards.rail(c, app, "film", isChar ? "Appears in" : "Known for",
                    media.length() + " titles", media, 108, new Cards.OnMedia() {
                        public void open(JSONObject m) {
                            dismiss(overlay);
                            app.openDetail(m.optInt("id"), m);
                        }
                    }));
            card.addView(rw);
        }

        card.addView(Ui.space(c, 16));
    }

    private static void addFact(Context c, LinearLayout row, String icon, String text) {
        LinearLayout f = Ui.row(c);
        f.setBackground(Ui.rounded(Theme.BG2, 999, Theme.LINE, 1));
        f.setPadding(Ui.dp(10), Ui.dp(5), Ui.dp(11), Ui.dp(5));
        f.addView(new Icons(c, icon, 11, Theme.ACC), Ui.lp(Ui.dp(11), Ui.dp(11)));
        f.addView(Ui.hspace(c, 6));
        f.addView(Ui.text(c, text, 11, Theme.MUT, Theme.SANS_SB));
        row.addView(f, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                row.getChildCount() == 0 ? 0 : 8, 0, 0, 0));
    }
}
