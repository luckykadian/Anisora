package app.anisora;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Shared widgets: Seg control, Toggle, Chip, SectionHead, EmptyState, Skeleton. */
public class Widgets {

    /* ------------------------------ rounded clip ------------------------------ */

    public static void clipRounded(View v, final float radiusDp) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Ui.dp(radiusDp));
            }
        });
        v.setClipToOutline(true);
    }

    /* ---------------------------------- chip ---------------------------------- */

    public static LinearLayout chip(Context c, String label, Icons icon, boolean accent) {
        LinearLayout l = Ui.row(c);
        l.setPadding(Ui.dp(11), Ui.dp(6), Ui.dp(11), Ui.dp(6));
        if (accent) l.setBackground(Ui.rounded(Theme.ACC_SOFT, 999, Theme.ACC_LINE, 1));
        else l.setBackground(Ui.rounded(Theme.BG1, 999, Theme.LINE, 1));
        if (icon != null) {
            l.addView(icon);
            l.addView(Ui.hspace(c, 6));
        }
        TextView t = Ui.text(c, label, 11, accent ? Theme.ACC : Theme.MUT, Theme.SANS_SB);
        l.addView(t);
        return l;
    }

    /* ------------------------------ section head ------------------------------ */

    public static LinearLayout sectionHead(Context c, String iconName, String title, String sub) {
        LinearLayout head = Ui.row(c);
        head.setPadding(0, 0, 0, Ui.dp(12));

        if (iconName != null) {
            FrameLayout ic = new FrameLayout(c);
            ic.setBackground(Ui.rounded(Theme.ACC_SOFT, 10, Theme.ACC_LINE, 1));
            Icons i = new Icons(c, iconName, 15, Theme.ACC);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(15), Ui.dp(15));
            ip.gravity = Gravity.CENTER;
            ic.addView(i, ip);
            head.addView(ic, Ui.lpm(Ui.dp(30), Ui.dp(30), 0, 0, 10, 0));
        }

        LinearLayout col = Ui.col(c);
        TextView t = Ui.text(c, title, 16.5f, Theme.TXT, Theme.DISP_BOLD);
        col.addView(t);
        if (sub != null) {
            TextView s = Ui.text(c, sub, 11, Theme.MUT, Theme.SANS_MED);
            s.setPadding(0, Ui.dp(2), 0, 0);
            col.addView(s);
        }
        LinearLayout.LayoutParams cp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.weight = 1;
        head.addView(col, cp);
        return head;
    }

    /* ----------------------------------- seg ----------------------------------- */

    public interface OnSeg {
        void pick(String id);
    }

    public static LinearLayout seg(Context c, String[][] options, String value, final OnSeg cb) {
        final LinearLayout wrap = Ui.row(c);
        wrap.setBackground(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1));
        wrap.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        for (int i = 0; i < options.length; i++) {
            final String id = options[i][0];
            String label = options[i][1];
            boolean active = id.equals(value);
            final TextView t = Ui.text(c, label, 12, active ? Theme.ACC_INK : Theme.MUT, Theme.SANS_SB);
            t.setPadding(Ui.dp(13), Ui.dp(7), Ui.dp(13), Ui.dp(7));
            t.setGravity(Gravity.CENTER);
            if (active) t.setBackground(Ui.rounded(Theme.ACC, 10, 0, 0));
            else t.setBackground(Ui.rounded(0x00000000, 10, 0, 0));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cb.pick(id);
                }
            });
            wrap.addView(t);
        }
        return wrap;
    }

    /* ---------------------------------- toggle --------------------------------- */

    public interface OnToggle {
        void toggled(boolean on);
    }

    public static FrameLayout toggle(final Context c, boolean initial, final OnToggle cb) {
        final FrameLayout track = new FrameLayout(c);
        final boolean[] state = {initial};
        final View thumb = new View(c);

        track.setLayoutParams(new ViewGroup.LayoutParams(Ui.dp(44), Ui.dp(26)));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(Ui.dp(20), Ui.dp(20));
        tp.topMargin = Ui.dp(3);
        tp.leftMargin = Ui.dp(3);
        thumb.setLayoutParams(tp);
        thumb.setBackground(Ui.circle(0xFFFFFFFF));
        track.addView(thumb);

        final Runnable apply = new Runnable() {
            public void run() {
                track.setBackground(Ui.rounded(state[0] ? Theme.ACC : Theme.BG2, 999, state[0] ? 0 : Theme.LINE, 1));
                thumb.animate().translationX(state[0] ? Ui.dp(18) : 0).setDuration(160).start();
            }
        };
        apply.run();
        thumb.setTranslationX(state[0] ? Ui.dp(18) : 0);

        track.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state[0] = !state[0];
                apply.run();
                cb.toggled(state[0]);
            }
        });
        return track;
    }

    /* -------------------------------- empty state ------------------------------- */

    public static LinearLayout emptyState(Context c, String iconName, String title, String sub) {
        LinearLayout l = Ui.col(c);
        l.setGravity(Gravity.CENTER);
        l.setPadding(Ui.dp(24), Ui.dp(40), Ui.dp(24), Ui.dp(40));
        l.setBackground(Ui.rounded(Theme.BG1, 18, Theme.LINE, 1));

        FrameLayout ic = new FrameLayout(c);
        ic.setBackground(Ui.circle(Theme.BG2));
        Icons i = new Icons(c, iconName, 22, Theme.MUT);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(22), Ui.dp(22));
        ip.gravity = Gravity.CENTER;
        ic.addView(i, ip);
        l.addView(ic, Ui.lp(Ui.dp(52), Ui.dp(52)));

        TextView t = Ui.text(c, title, 14.5f, Theme.TXT, Theme.SANS_SB);
        t.setPadding(0, Ui.dp(14), 0, 0);
        t.setGravity(Gravity.CENTER);
        l.addView(t);
        if (sub != null) {
            TextView s = Ui.text(c, sub, 12, Theme.MUT, Theme.SANS);
            s.setPadding(0, Ui.dp(5), 0, 0);
            s.setGravity(Gravity.CENTER);
            l.addView(s);
        }
        return l;
    }

    /* --------------------------------- skeleton --------------------------------- */

    public static View skel(Context c, float radiusDp) {
        final View v = new View(c);
        v.setBackground(Ui.rounded(Theme.alpha(Theme.LIGHT ? 0xFF0D1421 : 0xFFFFFFFF, 14), radiusDp, 0, 0));
        if (!Theme.REDUCE_MOTION) {
            android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofFloat(v, "alpha", 1f, 0.45f);
            a.setDuration(750);
            a.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            a.start();
        }
        return v;
    }

    public static LinearLayout skeletonRail(Context c, String iconName, String title, int cardW) {
        LinearLayout section = Ui.col(c);
        section.addView(sectionHead(c, iconName, title, null));
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.row(c);
        for (int i = 0; i < 6; i++) {
            LinearLayout card = Ui.col(c);
            View poster = skel(c, Theme.RADIUS);
            card.addView(poster, Ui.lp(Ui.dp(cardW), Ui.dp((int) (cardW * 1.5f))));
            View l1 = skel(c, 4);
            card.addView(l1, Ui.lpm(Ui.dp((int) (cardW * 0.8f)), Ui.dp(10), 0, 8, 0, 0));
            View l2 = skel(c, 4);
            card.addView(l2, Ui.lpm(Ui.dp((int) (cardW * 0.55f)), Ui.dp(8), 0, 6, 0, 0));
            row.addView(card, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 14, 0));
        }
        hs.addView(row);
        section.addView(hs);
        return section;
    }

    /* ------------------------------- score pill -------------------------------- */

    public static LinearLayout scorePill(Context c, int score) {
        LinearLayout l = Ui.row(c);
        l.setBackground(Ui.rounded(0xB3000000, 999, 0x26FFFFFF, 1));
        l.setPadding(Ui.dp(7), Ui.dp(3), Ui.dp(8), Ui.dp(3));
        Icons star = new Icons(c, "star", 9, Theme.STAR);
        l.addView(star, Ui.lp(Ui.dp(9), Ui.dp(9)));
        l.addView(Ui.hspace(c, 4));
        TextView t = Ui.text(c, String.valueOf(score), 10, 0xFFFFFFFF, Theme.MONO_BOLD);
        l.addView(t);
        return l;
    }

    /* ----------------------- horizontal wrap (genre chips) ---------------------- */

    public static LinearLayout wrapChips(Context c, List<String> labels, int maxPerRow) {
        LinearLayout col = Ui.col(c);
        LinearLayout row = null;
        int inRow = 0;
        for (int i = 0; i < labels.size(); i++) {
            if (row == null || inRow >= maxPerRow) {
                row = Ui.row(c);
                col.addView(row, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, i == 0 ? 0 : 8, 0, 0));
                inRow = 0;
            }
            LinearLayout ch = chip(c, labels.get(i), null, false);
            row.addView(ch, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, inRow == 0 ? 0 : 8, 0, 0, 0));
            inRow++;
        }
        return col;
    }

    /* ------------------------- poster gradient fallback ------------------------- */

    public static GradientDrawable posterFallback(String color, String title) {
        int from, to;
        if (color != null && color.length() == 7 && color.charAt(0) == '#') {
            try {
                int base = (int) Long.parseLong(color.substring(1), 16) | 0xFF000000;
                from = Ui.mix(base, 0xFF000000, 0.25f);
                to = Ui.mix(base, 0xFFFFFFFF, 0.15f);
            } catch (Exception e) {
                from = 0xFF25304A;
                to = 0xFF3D4E77;
            }
        } else {
            float h = Ui.hashHue(title == null ? "x" : title);
            from = Ui.hsl(h, 0.55f, 0.35f);
            to = Ui.hsl((h + 50) % 360, 0.6f, 0.55f);
        }
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{from, to});
        d.setCornerRadius(0);
        return d;
    }
}
