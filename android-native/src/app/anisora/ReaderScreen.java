package app.anisora;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Demo manga reader — the Read-tab counterpart of PlayerScreen. */
public class ReaderScreen {

    private static final int PAGES = 18; // simulated pages per chapter

    /** Real pages from an extension (image URLs). */
    public static void openPages(final Context c, final MainActivity app, final String head,
                                 final int chNum, final String chTitle, final java.util.List<String> urls,
                                 final PlayerScreen.OnDone onDone) {
        if (urls == null || urls.isEmpty()) {
            open(c, app, head, chNum, chTitle, null, onDone);
            return;
        }
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0xF2000000);
        overlay.setClickable(true);
        final int[] page = {1};
        final int P = urls.size();
        final boolean[] closed = {false};

        final FrameLayout pageBox = new FrameLayout(c);
        pageBox.setBackgroundColor(0xFF060708);
        final ImageView img = new ImageView(c);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageBox.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = Ui.row(c);
        top.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(12), Ui.dp(10));
        top.setBackgroundColor(0xB3000000);
        LinearLayout tcol = Ui.col(c);
        tcol.addView(Ui.oneLine(Ui.text(c, "Ch. " + chNum + " · " + chTitle, 13, 0xFFFFFFFF, Theme.SANS_BOLD)));
        TextView t2 = Ui.oneLine(Ui.mono(c, head, 9, 0x80FFFFFF));
        t2.setPadding(0, Ui.dp(2), 0, 0);
        tcol.addView(t2);
        LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.weight = 1;
        top.addView(tcol, tp);
        FrameLayout close = new FrameLayout(c);
        close.setBackground(Ui.rounded(0x66000000, 999, 0x26FFFFFF, 1));
        Icons xi = new Icons(c, "x", 13, 0xFFFFFFFF);
        FrameLayout.LayoutParams xip = new FrameLayout.LayoutParams(Ui.dp(13), Ui.dp(13));
        xip.gravity = Gravity.CENTER;
        close.addView(xi, xip);
        top.addView(close, Ui.lp(Ui.dp(30), Ui.dp(30)));

        LinearLayout bottom = Ui.col(c);
        bottom.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(14));
        bottom.setBackgroundColor(0xB3000000);
        final FrameLayout barBg = new FrameLayout(c);
        barBg.setBackground(Ui.rounded(0x33FFFFFF, 999, 0, 0));
        final View bar = new View(c);
        bar.setBackground(Ui.rounded(Theme.ACC, 999, 0, 0));
        barBg.addView(bar, new FrameLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        bottom.addView(barBg, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(5)));
        final TextView foot = Ui.mono(c, "page 1 / " + P, 9, 0x99FFFFFF);
        foot.setPadding(0, Ui.dp(8), 0, 0);
        bottom.addView(foot);

        final Runnable update = new Runnable() {
            public void run() {
                foot.setText(("page " + page[0] + " / " + P).toUpperCase());
                int w = barBg.getWidth();
                if (w > 0) {
                    ViewGroup.LayoutParams blp = bar.getLayoutParams();
                    blp.width = Math.max(1, w * page[0] / P);
                    bar.setLayoutParams(blp);
                }
                String u = urls.get(page[0] - 1);
                Images.load(u, img, 1200);
            }
        };
        final Runnable closeIt = new Runnable() {
            public void run() {
                if (closed[0]) return;
                closed[0] = true;
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
            }
        };
        pageBox.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() != MotionEvent.ACTION_UP) return true;
                boolean fwd = ev.getX() > v.getWidth() / 2f;
                if (fwd) {
                    if (page[0] >= P) { closeIt.run(); onDone.done(chNum); return true; }
                    page[0]++;
                } else if (page[0] > 1) page[0]--;
                update.run();
                return true;
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeIt.run(); }
        });
        barBg.post(update);

        LinearLayout root = Ui.col(c);
        root.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams pb = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        pb.weight = 1;
        root.addView(pageBox, pb);
        root.addView(bottom, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        overlay.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public static void open(final Context c, final MainActivity app, final String head,
                            final int chNum, final String chTitle, final String cover,
                            final PlayerScreen.OnDone onDone) {
        final FrameLayout overlay = new FrameLayout(c);
        overlay.setBackgroundColor(0xF2000000);
        overlay.setClickable(true);

        final int[] page = {1};
        final boolean[] closed = {false};

        /* ---------------- page area ---------------- */
        final FrameLayout pageBox = new FrameLayout(c);
        pageBox.setBackgroundColor(0xFF060708);

        ImageView bg = new ImageView(c);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.35f);
        pageBox.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (cover != null && !"null".equals(cover)) Images.load(cover, bg, 700);
        else bg.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Theme.alpha(Theme.ACC, 50), 0xFF0A0C11}));

        // simulated page card
        LinearLayout pageCard = Ui.col(c);
        pageCard.setGravity(Gravity.CENTER);
        pageCard.setBackground(Ui.rounded(0xCC0E1116, 18, 0x26FFFFFF, 1));
        pageCard.setPadding(Ui.dp(30), Ui.dp(38), Ui.dp(30), Ui.dp(38));
        final TextView bigPage = Ui.text(c, "1", 44, Theme.TXT, Theme.DISP_BOLD);
        pageCard.addView(bigPage);
        final TextView pageLabel = Ui.mono(c, "page 1 / " + PAGES, 10, Theme.MUT);
        pageLabel.setPadding(0, Ui.dp(8), 0, 0);
        pageCard.addView(pageLabel);
        TextView hint = Ui.text(c, "Tap right to turn · left to go back", 11, Theme.MUT, Theme.SANS);
        hint.setPadding(0, Ui.dp(18), 0, 0);
        pageCard.addView(hint);
        FrameLayout.LayoutParams pcp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pcp.gravity = Gravity.CENTER;
        pageBox.addView(pageCard, pcp);

        /* ---------------- top bar ---------------- */
        LinearLayout top = Ui.row(c);
        top.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(12), Ui.dp(10));
        top.setBackgroundColor(0xB3000000);
        LinearLayout tcol = Ui.col(c);
        tcol.addView(Ui.oneLine(Ui.text(c, "Ch. " + chNum + " · " + chTitle, 13, 0xFFFFFFFF, Theme.SANS_BOLD)));
        TextView t2 = Ui.oneLine(Ui.mono(c, head, 9, 0x80FFFFFF));
        t2.setPadding(0, Ui.dp(2), 0, 0);
        tcol.addView(t2);
        LinearLayout.LayoutParams tp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.weight = 1;
        top.addView(tcol, tp);

        LinearLayout srv = Ui.row(c);
        srv.setBackground(Ui.rounded(0x1AFFFFFF, 999, 0x26FFFFFF, 1));
        srv.setPadding(Ui.dp(10), Ui.dp(4), Ui.dp(10), Ui.dp(4));
        srv.addView(Ui.text(c, app.store.getS("readExt", "MangaDex") + " · EN", 10, 0xCCFFFFFF, Theme.SANS_SB));
        top.addView(srv);
        top.addView(Ui.hspace(c, 8));

        FrameLayout close = new FrameLayout(c);
        close.setBackground(Ui.rounded(0x66000000, 999, 0x26FFFFFF, 1));
        Icons xi = new Icons(c, "x", 13, 0xFFFFFFFF);
        FrameLayout.LayoutParams xip = new FrameLayout.LayoutParams(Ui.dp(13), Ui.dp(13));
        xip.gravity = Gravity.CENTER;
        close.addView(xi, xip);
        top.addView(close, Ui.lp(Ui.dp(30), Ui.dp(30)));

        /* ---------------- bottom: progress bar ---------------- */
        LinearLayout bottom = Ui.col(c);
        bottom.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(14));
        bottom.setBackgroundColor(0xB3000000);
        final FrameLayout barBg = new FrameLayout(c);
        barBg.setBackground(Ui.rounded(0x33FFFFFF, 999, 0, 0));
        final View bar = new View(c);
        bar.setBackground(Ui.rounded(Theme.ACC, 999, 0, 0));
        barBg.addView(bar, new FrameLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        bottom.addView(barBg, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(5)));
        final TextView foot = Ui.mono(c, "demo reader · " + app.store.getS("readExt", "MangaDex"), 9, 0x99FFFFFF);
        foot.setPadding(0, Ui.dp(8), 0, 0);
        bottom.addView(foot);

        /* ---------------- behaviour ---------------- */
        final Runnable update = new Runnable() {
            public void run() {
                bigPage.setText(String.valueOf(page[0]));
                pageLabel.setText(("page " + page[0] + " / " + PAGES).toUpperCase());
                int w = barBg.getWidth();
                if (w > 0) {
                    ViewGroup.LayoutParams blp = bar.getLayoutParams();
                    blp.width = Math.max(1, w * page[0] / PAGES);
                    bar.setLayoutParams(blp);
                }
            }
        };
        final Runnable closeIt = new Runnable() {
            public void run() {
                if (closed[0]) return;
                closed[0] = true;
                ViewGroup p = (ViewGroup) overlay.getParent();
                if (p != null) p.removeView(overlay);
            }
        };
        pageBox.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() != MotionEvent.ACTION_UP) return true;
                boolean fwd = ev.getX() > v.getWidth() / 2f;
                if (fwd) {
                    if (page[0] >= PAGES) {
                        closeIt.run();
                        onDone.done(chNum);
                        return true;
                    }
                    page[0]++;
                } else if (page[0] > 1) {
                    page[0]--;
                }
                update.run();
                return true;
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeIt.run();
            }
        });
        barBg.post(update);

        /* ---------------- assemble ---------------- */
        LinearLayout root = Ui.col(c);
        root.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams pb = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        pb.weight = 1;
        root.addView(pageBox, pb);
        root.addView(bottom, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        overlay.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        app.overlayRoot().addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (!Theme.REDUCE_MOTION) {
            overlay.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(180).start();
        }
    }
}
