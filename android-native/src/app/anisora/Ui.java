package app.anisora;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small view-building helpers shared by every screen. */
public class Ui {

    public static float density = 2f;

    public static void init(Context ctx) {
        density = ctx.getResources().getDisplayMetrics().density;
    }

    public static int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    /* ------------------------------ drawables ------------------------------ */

    public static GradientDrawable rounded(int fill, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) d.setStroke(Math.max(1, dp(strokeDp)), strokeColor);
        return d;
    }

    public static GradientDrawable circle(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        return d;
    }

    public static GradientDrawable gradient(int from, int to, GradientDrawable.Orientation o, float radiusDp) {
        GradientDrawable d = new GradientDrawable(o, new int[]{from, to});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    public static RippleDrawable ripple(GradientDrawable content, int rippleColor) {
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(18)); // getCornerRadius() needs API 24; close enough for a mask
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static void bg(View v, int fill, float radiusDp, int strokeColor, float strokeDp) {
        v.setBackground(ripple(rounded(fill, radiusDp, strokeColor, strokeDp), Theme.alpha(Theme.TXT, 26)));
    }

    /* -------------------------------- text -------------------------------- */

    public static TextView text(Context c, String s, float sizeSp, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView mono(Context c, String s, float sizeSp, int color) {
        TextView t = text(c, s == null ? "" : s.toUpperCase(), sizeSp, color, Theme.MONO_BOLD);
        t.setLetterSpacing(0.22f);
        return t;
    }

    public static TextView oneLine(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    /* ------------------------------- layouts ------------------------------- */

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    public static View space(Context c, int hDp) {
        View v = new View(c);
        v.setLayoutParams(new ViewGroup.LayoutParams(1, dp(hDp)));
        return v;
    }

    public static View hspace(Context c, int wDp) {
        View v = new View(c);
        v.setLayoutParams(new ViewGroup.LayoutParams(dp(wDp), 1));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Theme.LINE);
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        return v;
    }

    /* -------------------------------- motion -------------------------------- */

    /** Press-scale feedback (whileTap in the web UI). Returns false so clicks still fire. */
    public static void press(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, android.view.MotionEvent ev) {
                if (Theme.REDUCE_MOTION) return false;
                int a = ev.getAction();
                if (a == android.view.MotionEvent.ACTION_DOWN) {
                    view.animate().scaleX(0.965f).scaleY(0.965f).setDuration(90).start();
                } else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                }
                return false;
            }
        });
    }

    /** Fade + rise entrance (initial/animate pairs in the web UI). */
    public static void appear(View v, long delayMs) {
        if (Theme.REDUCE_MOTION) return;
        v.setAlpha(0f);
        v.setTranslationY(dp(10));
        v.animate().alpha(1f).translationY(0).setStartDelay(delayMs).setDuration(240).start();
    }

    /* -------------------------------- misc -------------------------------- */

    public static int mix(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a), aa = Color.alpha(a);
        int br = Color.red(b), bg2 = Color.green(b), bb = Color.blue(b), ba = Color.alpha(b);
        return Color.argb((int) (aa + (ba - aa) * t), (int) (ar + (br - ar) * t),
                (int) (ag + (bg2 - ag) * t), (int) (ab + (bb - ab) * t));
    }

    /** Deterministic hue from a string — mirrors hashHue() in src/api.ts. */
    public static int hashHue(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) h = (h * 31 + s.charAt(i)) % 360;
        return h;
    }

    public static int hsl(float h, float s, float l) {
        return Color.HSVToColor(new float[]{h, s, l});
    }
}
