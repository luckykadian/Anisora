package app.anisora;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Demo player overlay — replica of Player in src/components/WatchTab.tsx. */
public class PlayerScreen {

    public interface OnDone {
        void done(int epNumber);
    }

    private static final int DUR = 60; // seconds of simulated playback (like the web demo)

    public static void open(final Context c, final MainActivity app, final String head,
                            final int epNum, final String epTitle, final String thumb,
                            final OnDone onDone) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0xD9000000);
        overlay.setClickable(true);

        /* ---------------- video card (16:9) ---------------- */
        final FrameLayout card = new FrameLayout(c);
        card.setBackgroundColor(0xFF000000);
        Widgets.clipRounded(card, 20);

        ImageView bg = new ImageView(c);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.7f);
        card.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (thumb != null && thumb.length() > 0 && !"null".equals(thumb)) Images.load(thumb, bg, 900);
        else bg.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.alpha(Theme.ACC, 64), Theme.BG0}));

        View scrim = new View(c);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xE6000000, 0x33000000, 0x99000000}));
        card.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        /* ---------------- state ---------------- */
        final float[] t = {0f};
        final boolean[] playing = {true};
        final boolean[] closed = {false};
        final Handler h = new Handler();

        /* ---------------- top bar ---------------- */
        LinearLayout top = Ui.row(c);
        top.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(12), 0);
        LinearLayout tcol = Ui.col(c);
        TextView t1 = Ui.oneLine(Ui.text(c, "E" + epNum + " · " + epTitle, 13, 0xFFFFFFFF, Theme.SANS_BOLD));
        tcol.addView(t1);
        TextView t2 = Ui.oneLine(Ui.mono(c, head, 9, 0x80FFFFFF));
        t2.setPadding(0, Ui.dp(2), 0, 0);
        tcol.addView(t2);
        LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.weight = 1;
        top.addView(tcol, tp);

        LinearLayout srv = Ui.row(c);
        srv.setBackground(Ui.rounded(0x1AFFFFFF, 999, 0x26FFFFFF, 1));
        srv.setPadding(Ui.dp(10), Ui.dp(4), Ui.dp(10), Ui.dp(4));
        srv.addView(Ui.text(c, "AniWatch · HD-1", 10, 0xCCFFFFFF, Theme.SANS_SB));
        top.addView(srv);
        top.addView(Ui.hspace(c, 8));

        FrameLayout close = new FrameLayout(c);
        close.setBackground(Ui.rounded(0x80000000, 999, 0x26FFFFFF, 1));
        Icons xi = new Icons(c, "x", 13, 0xFFFFFFFF);
        FrameLayout.LayoutParams xip = new FrameLayout.LayoutParams(Ui.dp(13), Ui.dp(13));
        xip.gravity = Gravity.CENTER;
        close.addView(xi, xip);
        top.addView(close, Ui.lp(Ui.dp(30), Ui.dp(30)));
        FrameLayout.LayoutParams topP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.addView(top, topP);

        /* ---------------- center play/pause ---------------- */
        final FrameLayout mid = new FrameLayout(c);
        mid.setBackground(Ui.rounded(0x80000000, 999, 0x40FFFFFF, 1));
        final Icons midIcon = new Icons(c, "pause", 26, 0xFFFFFFFF, 2.6f);
        FrameLayout.LayoutParams mip = new FrameLayout.LayoutParams(Ui.dp(26), Ui.dp(26));
        mip.gravity = Gravity.CENTER;
        mid.addView(midIcon, mip);
        FrameLayout.LayoutParams midP = new FrameLayout.LayoutParams(Ui.dp(66), Ui.dp(66));
        midP.gravity = Gravity.CENTER;
        card.addView(mid, midP);

        /* ---------------- skip intro ---------------- */
        final LinearLayout skip = Ui.row(c);
        skip.setBackground(Ui.rounded(0xB3000000, 12, 0x33FFFFFF, 1));
        skip.setPadding(Ui.dp(14), Ui.dp(9), Ui.dp(14), Ui.dp(9));
        skip.addView(new Icons(c, "skip", 13, 0xFFFFFFFF), Ui.lp(Ui.dp(13), Ui.dp(13)));
        skip.addView(Ui.hspace(c, 7));
        skip.addView(Ui.text(c, "Skip intro", 12, 0xFFFFFFFF, Theme.SANS_BOLD));
        skip.setVisibility(View.GONE);
        FrameLayout.LayoutParams skp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        skp.setMargins(0, 0, Ui.dp(14), Ui.dp(76));
        card.addView(skip, skp);

        /* ---------------- bottom controls ---------------- */
        LinearLayout bottom = Ui.col(c);
        bottom.setPadding(Ui.dp(14), Ui.dp(16), Ui.dp(14), Ui.dp(12));

        final FrameLayout barBg = new FrameLayout(c);
        barBg.setBackground(Ui.rounded(0x33FFFFFF, 999, 0, 0));
        final View bar = new View(c);
        bar.setBackground(Ui.rounded(Theme.ACC, 999, 0, 0));
        barBg.addView(bar, new FrameLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        bottom.addView(barBg, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(5)));

        LinearLayout ctl = Ui.row(c);
        ctl.setPadding(0, Ui.dp(10), 0, 0);
        final Icons small = new Icons(c, "pause", 16, 0xFFFFFFFF, 2.4f);
        ctl.addView(small, Ui.lp(Ui.dp(16), Ui.dp(16)));
        ctl.addView(Ui.hspace(c, 10));
        final TextView time = Ui.text(c, "0:00 / 1:00", 11, 0xCCFFFFFF, Theme.MONO_MED);
        ctl.addView(time);
        ctl.addView(Ui.hspace(c, 8));
        TextView demo = Ui.mono(c, "demo playback", 9, 0x99FFFFFF);
        ctl.addView(demo);
        View spring = new View(c);
        LinearLayout.LayoutParams sp2 = Ui.lp(0, 1);
        sp2.weight = 1;
        ctl.addView(spring, sp2);
        ctl.addView(new Icons(c, "captions", 14, 0xB3FFFFFF), Ui.lp(Ui.dp(14), Ui.dp(14)));
        ctl.addView(Ui.hspace(c, 10));
        ctl.addView(new Icons(c, "volume", 14, 0xB3FFFFFF), Ui.lp(Ui.dp(14), Ui.dp(14)));
        ctl.addView(Ui.hspace(c, 10));
        TextView q = Ui.text(c, app.store.getS("quality", "1080p"), 9.5f, 0xFFFFFFFF, Theme.MONO_BOLD);
        q.setBackground(Ui.rounded(0x1AFFFFFF, 6, 0x33FFFFFF, 1));
        q.setPadding(Ui.dp(6), Ui.dp(2), Ui.dp(6), Ui.dp(2));
        ctl.addView(q);
        bottom.addView(ctl);

        FrameLayout.LayoutParams botP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        botP.gravity = Gravity.BOTTOM;
        card.addView(bottom, botP);

        /* ---------------- behaviour ---------------- */
        final Runnable[] tick = new Runnable[1];
        final Runnable closeIt = new Runnable() {
            public void run() {
                if (closed[0]) return;
                closed[0] = true;
                h.removeCallbacks(tick[0]);
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
            }
        };
        tick[0] = new Runnable() {
            public void run() {
                if (closed[0]) return;
                if (playing[0]) t[0] = Math.min(DUR, t[0] + 0.5f);
                int w = barBg.getWidth();
                if (w > 0) {
                    ViewGroup.LayoutParams blp = bar.getLayoutParams();
                    blp.width = Math.max(1, (int) (w * t[0] / DUR));
                    bar.setLayoutParams(blp);
                }
                int sec = (int) t[0];
                time.setText((sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60) + " / 1:00");
                boolean showSkip = app.store.getB("skipIntro", true) && t[0] >= 3 && t[0] < 12;
                skip.setVisibility(showSkip ? View.VISIBLE : View.GONE);
                if (t[0] >= DUR) {
                    closeIt.run();
                    onDone.done(epNum);
                    return;
                }
                h.postDelayed(this, 500);
            }
        };
        h.postDelayed(tick[0], 500);

        View.OnClickListener togglePlay = new View.OnClickListener() {
            public void onClick(View v) {
                playing[0] = !playing[0];
                midIcon.setIcon(playing[0] ? "pause" : "play");
                small.setIcon(playing[0] ? "pause" : "play");
            }
        };
        mid.setOnClickListener(togglePlay);
        small.setOnClickListener(togglePlay);
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t[0] = 16;
            }
        });
        barBg.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) {
                    float f = Math.max(0, Math.min(1, ev.getX() / Math.max(1, v.getWidth())));
                    t[0] = f * DUR;
                    return true;
                }
                return false;
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeIt.run();
            }
        });

        // 16:9 sizing
        int screenW = c.getResources().getDisplayMetrics().widthPixels;
        int cardW = screenW - Ui.dp(24);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(cardW, cardW * 9 / 16);
        cp.gravity = Gravity.CENTER;
        overlay.addView(card, cp);

        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!Theme.REDUCE_MOTION) {
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(180).start();
            card.setScaleX(0.94f);
            card.setScaleY(0.94f);
            card.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
        }
    }
}
