package app.anisora;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;

/** Onboarding — mirrors src/components/Onboarding.tsx (mobile layout). */
public class OnboardingScreen {

    public static View build(final Context c, final MainActivity app) {
        FrameLayout root = new FrameLayout(c);
        root.setBackgroundColor(Theme.BG0);

        // backdrop art + gradients
        ImageView art = new ImageView(c);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setAlpha(0.6f);
        try {
            InputStream in = c.getAssets().open("onboarding.jpg");
            art.setImageBitmap(android.graphics.BitmapFactory.decodeStream(in));
            in.close();
        } catch (Exception ignored) {
        }
        root.addView(art, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View grad1 = new View(c);
        grad1.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Theme.BG0, Theme.alpha(Theme.BG0, 204), Theme.alpha(Theme.BG0, 77)}));
        root.addView(grad1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View grad2 = new View(c);
        grad2.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Theme.BG0, 0x00000000, Theme.alpha(Theme.BG0, 179)}));
        root.addView(grad2, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView sc = new ScrollView(c);
        sc.setVerticalScrollBarEnabled(false);
        sc.setFillViewport(true);
        LinearLayout col = Ui.col(c);
        col.setPadding(Ui.dp(24), Ui.dp(28), Ui.dp(24), Ui.dp(24));

        // header: logo + shield chip
        LinearLayout header = Ui.row(c);
        header.addView(logo(c, 30, true));
        View spring = new View(c);
        LinearLayout.LayoutParams sp = Ui.lp(0, 1);
        sp.weight = 1;
        header.addView(spring, sp);
        col.addView(header);

        col.addView(Ui.space(c, 48));

        // badge
        LinearLayout badge = Ui.row(c);
        badge.setBackground(Ui.rounded(Theme.ACC_SOFT, 999, Theme.ACC_LINE, 1));
        badge.setPadding(Ui.dp(13), Ui.dp(6), Ui.dp(13), Ui.dp(6));
        badge.addView(new Icons(c, "sparkles", 12, Theme.ACC), Ui.lp(Ui.dp(12), Ui.dp(12)));
        badge.addView(Ui.hspace(c, 7));
        TextView bt = Ui.text(c, "THE ANILIST TRACKER, REIMAGINED", 10.5f, Theme.ACC, Theme.SANS_SB);
        bt.setLetterSpacing(0.05f);
        badge.addView(bt);
        LinearLayout badgeWrap = Ui.row(c);
        badgeWrap.addView(badge);
        col.addView(badgeWrap);

        col.addView(Ui.space(c, 22));

        // headline
        TextView h1a = Ui.text(c, "Log the stories", 38, Theme.TXT, Theme.DISP_BOLD);
        h1a.setLineSpacing(0, 1.02f);
        col.addView(h1a);
        final TextView h1b = Ui.text(c, "that move you.", 38, Theme.ACC, Theme.DISP_BOLD);
        h1b.setLineSpacing(0, 1.02f);
        // gradient text like the web (acc -> white/60)
        h1b.post(new Runnable() {
            public void run() {
                int w = h1b.getWidth();
                if (w > 0) {
                    h1b.getPaint().setShader(new LinearGradient(0, 0, w, 0,
                            new int[]{Theme.ACC, Theme.ACC, 0x99FFFFFF}, new float[]{0, 0.55f, 1}, Shader.TileMode.CLAMP));
                    h1b.invalidate();
                }
            }
        });
        col.addView(h1b);

        col.addView(Ui.space(c, 18));

        TextView sub = Ui.text(c,
                "Track every episode and chapter, discover what's next, and bend the interface to your taste — powered by the AniList universe.",
                14, Theme.MUT, Theme.SANS);
        sub.setLineSpacing(Ui.dp(4), 1f);
        col.addView(sub);

        col.addView(Ui.space(c, 30));

        // CTA: continue with AniList
        LinearLayout cta = Ui.row(c);
        cta.setGravity(Gravity.CENTER);
        cta.setBackground(Ui.ripple(Ui.rounded(Theme.ACC, 16, 0, 0), 0x33000000));
        cta.setPadding(Ui.dp(20), Ui.dp(14), Ui.dp(20), Ui.dp(14));
        TextView al = Ui.text(c, "AL", 11, Theme.ACC_INK, Theme.SANS_BOLD);
        al.setBackground(Ui.rounded(0x40FFFFFF, 7, 0, 0));
        al.setPadding(Ui.dp(5), Ui.dp(2), Ui.dp(5), Ui.dp(2));
        cta.addView(al);
        cta.addView(Ui.hspace(c, 10));
        cta.addView(Ui.text(c, "Continue with AniList", 14.5f, Theme.ACC_INK, Theme.SANS_BOLD));
        cta.addView(Ui.hspace(c, 8));
        cta.addView(new Icons(c, "chev-right", 15, Theme.ACC_INK), Ui.lp(Ui.dp(15), Ui.dp(15)));
        cta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.showOauthDialog();
            }
        });
        col.addView(cta, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(Ui.space(c, 12));

        // guest button
        LinearLayout guest = Ui.row(c);
        guest.setGravity(Gravity.CENTER);
        guest.setBackground(Ui.ripple(Ui.rounded(Theme.alpha(Theme.BG1, 179), 16, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        guest.setPadding(Ui.dp(20), Ui.dp(14), Ui.dp(20), Ui.dp(14));
        guest.addView(new Icons(c, "user", 15, Theme.TXT), Ui.lp(Ui.dp(15), Ui.dp(15)));
        guest.addView(Ui.hspace(c, 9));
        guest.addView(Ui.text(c, "Browse as guest", 14.5f, Theme.TXT, Theme.SANS_SB));
        guest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                app.store.login("Guest", true);
                app.toast("Welcome aboard — browsing as guest", "check");
                app.rebuild();
            }
        });
        col.addView(guest, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(Ui.space(c, 34));

        // feature cards
        String[][] feats = {
                {"trending", "Live AniList sync", "Lists & progress stay in orbit"},
                {"layers", "Extensions", "Pick sources for every episode"},
                {"sparkles", "Yours, visually", "Themes, accents, density"},
        };
        for (int i = 0; i < feats.length; i++) {
            LinearLayout card = Ui.col(c);
            card.setBackground(Ui.rounded(Theme.alpha(Theme.BG1, 128), 16, Theme.LINE, 1));
            card.setPadding(Ui.dp(14), Ui.dp(14), Ui.dp(14), Ui.dp(14));
            card.addView(new Icons(c, feats[i][0], 16, Theme.ACC), Ui.lp(Ui.dp(16), Ui.dp(16)));
            TextView ft = Ui.text(c, feats[i][1], 12.5f, Theme.TXT, Theme.SANS_SB);
            ft.setPadding(0, Ui.dp(8), 0, 0);
            card.addView(ft);
            TextView fs = Ui.text(c, feats[i][2], 11, Theme.MUT, Theme.SANS);
            fs.setPadding(0, Ui.dp(4), 0, 0);
            card.addView(fs);
            col.addView(card, Ui.lpm(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 10, 0, 0));
        }

        // filler pushes footer down
        View filler = new View(c);
        LinearLayout.LayoutParams fp = Ui.lp(1, 0);
        fp.weight = 1;
        col.addView(filler, fp);

        col.addView(Ui.space(c, 28));
        TextView foot = Ui.text(c, "Anisora demo · not affiliated with AniList", 11, Theme.MUT, Theme.SANS);
        foot.setGravity(Gravity.CENTER);
        col.addView(foot, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sc.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sc, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    /** The orbit-planet logo from src/bits.tsx, drawn natively. */
    public static View logo(Context c, final int sizeDp, boolean withText) {
        LinearLayout row = Ui.row(c);
        View mark = new View(c) {
            protected void onDraw(android.graphics.Canvas cv) {
                float s = getWidth() / 32f;
                android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                // orbit ring
                p.setStyle(android.graphics.Paint.Style.STROKE);
                p.setStrokeWidth(1.5f * s);
                p.setColor(Theme.alpha(Theme.ACC, 230));
                cv.save();
                cv.rotate(-22, 16 * s, 16 * s);
                cv.drawOval(new android.graphics.RectF(2 * s, 10.6f * s, 30 * s, 21.4f * s), p);
                cv.restore();
                // core
                p.setStyle(android.graphics.Paint.Style.FILL);
                p.setShader(new LinearGradient(9 * s, 9 * s, 23 * s, 23 * s,
                        Theme.ACC, Ui.mix(Theme.ACC, 0xFFFFFFFF, 0.65f), Shader.TileMode.CLAMP));
                cv.drawCircle(16 * s, 16 * s, 6.5f * s, p);
                p.setShader(null);
                // moon
                p.setColor(Theme.ACC);
                cv.drawCircle(27 * s, 11 * s, 1.9f * s, p);
            }
        };
        row.addView(mark, Ui.lp(Ui.dp(sizeDp), Ui.dp(sizeDp)));
        if (withText) {
            row.addView(Ui.hspace(c, 10));
            TextView a = Ui.text(c, "Ani", 19, Theme.TXT, Theme.DISP_BOLD);
            TextView b = Ui.text(c, "sora", 19, Theme.ACC, Theme.DISP_BOLD);
            row.addView(a);
            row.addView(b);
        }
        return row;
    }
}
