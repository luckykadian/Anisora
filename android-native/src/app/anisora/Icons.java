package app.anisora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Hand-drawn lucide-style stroked icons (24x24 grid, round caps/joins),
 * matching the icon set used across the web UI.
 */
public class Icons extends View {

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String name;
    private int sizePx;

    public Icons(Context c, String name, float sizeDp, int color) {
        this(c, name, sizeDp, color, 2f);
    }

    public Icons(Context c, String name, float sizeDp, int color, float sw) {
        super(c);
        this.name = name;
        this.sizePx = Ui.dp(sizeDp);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(sw);
        stroke.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
        setLayoutParams(new android.view.ViewGroup.LayoutParams(sizePx, sizePx));
    }

    public void setColor(int color) {
        stroke.setColor(color);
        fill.setColor(color);
        invalidate();
    }

    public void setIcon(String n) {
        this.name = n;
        invalidate();
    }

    protected void onMeasure(int w, int h) {
        setMeasuredDimension(sizePx, sizePx);
    }

    protected void onDraw(Canvas cv) {
        float s = sizePx / 24f;
        cv.save();
        cv.scale(s, s);
        Paint p = stroke;
        String n = name;
        if ("search".equals(n)) {
            cv.drawCircle(11, 11, 7, p);
            cv.drawLine(16.2f, 16.2f, 21, 21, p);
        } else if ("bell".equals(n)) {
            Path a = new Path();
            a.moveTo(6, 10);
            a.cubicTo(6, 5.5f, 8.5f, 3.5f, 12, 3.5f);
            a.cubicTo(15.5f, 3.5f, 18, 5.5f, 18, 10);
            a.cubicTo(18, 15, 20, 16.2f, 20, 16.2f);
            a.lineTo(4, 16.2f);
            a.cubicTo(4, 16.2f, 6, 15, 6, 10);
            a.close();
            cv.drawPath(a, p);
            cv.drawArc(new RectF(10.3f, 18.2f, 13.7f, 21.2f), 20, 140, false, p);
        } else if ("film".equals(n)) {
            cv.drawRoundRect(new RectF(3, 3, 21, 21), 2.5f, 2.5f, p);
            cv.drawLine(7, 3.4f, 7, 20.6f, p);
            cv.drawLine(17, 3.4f, 17, 20.6f, p);
            cv.drawLine(3.4f, 12, 7, 12, p);
            cv.drawLine(17, 12, 20.6f, 12, p);
            cv.drawLine(3.4f, 7.5f, 7, 7.5f, p);
            cv.drawLine(3.4f, 16.5f, 7, 16.5f, p);
            cv.drawLine(17, 7.5f, 20.6f, 7.5f, p);
            cv.drawLine(17, 16.5f, 20.6f, 16.5f, p);
        } else if ("book".equals(n)) {
            Path a = new Path();
            a.moveTo(12, 6.5f);
            a.cubicTo(10.5f, 4.8f, 8, 4, 2.8f, 4.3f);
            a.lineTo(2.8f, 18.6f);
            a.cubicTo(8, 18.3f, 10.5f, 19, 12, 20.6f);
            a.cubicTo(13.5f, 19, 16, 18.3f, 21.2f, 18.6f);
            a.lineTo(21.2f, 4.3f);
            a.cubicTo(16, 4, 13.5f, 4.8f, 12, 6.5f);
            a.close();
            cv.drawPath(a, p);
            cv.drawLine(12, 6.5f, 12, 20.2f, p);
        } else if ("compass".equals(n)) {
            cv.drawCircle(12, 12, 9, p);
            Path a = new Path();
            a.moveTo(16.2f, 7.8f);
            a.lineTo(14.1f, 14.1f);
            a.lineTo(7.8f, 16.2f);
            a.lineTo(9.9f, 9.9f);
            a.close();
            cv.drawPath(a, p);
        } else if ("settings".equals(n)) {
            cv.drawCircle(12, 12, 3.2f, p);
            for (int i = 0; i < 8; i++) {
                double ang = Math.PI / 4 * i + Math.PI / 8;
                float c1 = (float) Math.cos(ang), s1 = (float) Math.sin(ang);
                cv.drawLine(12 + c1 * 6.4f, 12 + s1 * 6.4f, 12 + c1 * 9.2f, 12 + s1 * 9.2f, p);
            }
            cv.drawCircle(12, 12, 6.4f, p);
        } else if ("flame".equals(n)) {
            Path a = new Path();
            a.moveTo(12, 2.8f);
            a.cubicTo(10.5f, 6.5f, 6.8f, 8.5f, 6.8f, 13.2f);
            a.cubicTo(6.8f, 17.2f, 9.2f, 20.6f, 12, 20.6f);
            a.cubicTo(14.8f, 20.6f, 17.2f, 17.2f, 17.2f, 13.2f);
            a.cubicTo(17.2f, 10.6f, 16, 8.8f, 14.6f, 7);
            a.cubicTo(13.8f, 6, 12.8f, 4.6f, 12, 2.8f);
            a.close();
            cv.drawPath(a, p);
            cv.drawArc(new RectF(9.6f, 12.5f, 14.4f, 20.4f), 30, 220, false, p);
        } else if ("trending".equals(n)) {
            Path a = new Path();
            a.moveTo(2.5f, 16.5f);
            a.lineTo(8.8f, 10.2f);
            a.lineTo(13.4f, 14.8f);
            a.lineTo(21.5f, 6.8f);
            cv.drawPath(a, p);
            Path b = new Path();
            b.moveTo(15.5f, 6.8f);
            b.lineTo(21.5f, 6.8f);
            b.lineTo(21.5f, 12.8f);
            cv.drawPath(b, p);
        } else if ("trophy".equals(n)) {
            Path a = new Path();
            a.moveTo(7, 3);
            a.lineTo(17, 3);
            a.lineTo(17, 9.5f);
            a.cubicTo(17, 12.5f, 15, 14.5f, 12, 14.5f);
            a.cubicTo(9, 14.5f, 7, 12.5f, 7, 9.5f);
            a.close();
            cv.drawPath(a, p);
            cv.drawArc(new RectF(2.6f, 4.5f, 7.2f, 9.5f), 90, 180, false, p);
            cv.drawArc(new RectF(16.8f, 4.5f, 21.4f, 9.5f), 270, 180, false, p);
            cv.drawLine(12, 14.8f, 12, 18, p);
            cv.drawLine(7.5f, 21, 16.5f, 21, p);
            cv.drawLine(9, 18, 15, 18, p);
        } else if ("heart".equals(n)) {
            cv.drawPath(heartPath(), p);
        } else if ("heart-fill".equals(n)) {
            cv.drawPath(heartPath(), fill);
        } else if ("grid".equals(n)) {
            cv.drawRoundRect(new RectF(3, 3, 10.4f, 10.4f), 1.6f, 1.6f, p);
            cv.drawRoundRect(new RectF(13.6f, 3, 21, 10.4f), 1.6f, 1.6f, p);
            cv.drawRoundRect(new RectF(3, 13.6f, 10.4f, 21), 1.6f, 1.6f, p);
            cv.drawRoundRect(new RectF(13.6f, 13.6f, 21, 21), 1.6f, 1.6f, p);
        } else if ("clock".equals(n)) {
            cv.drawCircle(12, 12, 8.6f, p);
            cv.drawLine(12, 7, 12, 12.2f, p);
            cv.drawLine(12, 12.2f, 15.4f, 13.8f, p);
        } else if ("wifi".equals(n)) {
            cv.drawArc(new RectF(2, 8, 22, 28), 218, 104, false, p);
            cv.drawArc(new RectF(5.6f, 11.6f, 18.4f, 24.4f), 214, 112, false, p);
            cv.drawCircle(12, 18.6f, 1.15f, fill);
        } else if ("cloud-off".equals(n)) {
            cv.drawArc(new RectF(3, 8, 13, 18), 90, 200, false, p);
            cv.drawArc(new RectF(8, 5, 18.6f, 15.6f), 190, 145, false, p);
            cv.drawLine(6.5f, 17.9f, 17.5f, 17.9f, p);
            cv.drawLine(4, 4, 20, 20, p);
        } else if ("plus".equals(n)) {
            cv.drawLine(12, 5.4f, 12, 18.6f, p);
            cv.drawLine(5.4f, 12, 18.6f, 12, p);
        } else if ("star".equals(n)) {
            cv.drawPath(starPath(), fill);
        } else if ("check".equals(n)) {
            Path a = new Path();
            a.moveTo(4.4f, 12.4f);
            a.lineTo(9.4f, 17.4f);
            a.lineTo(19.6f, 6.6f);
            cv.drawPath(a, p);
        } else if ("x".equals(n)) {
            cv.drawLine(6.4f, 6.4f, 17.6f, 17.6f, p);
            cv.drawLine(17.6f, 6.4f, 6.4f, 17.6f, p);
        } else if ("chev-left".equals(n)) {
            Path a = new Path();
            a.moveTo(14.6f, 5.6f);
            a.lineTo(8.2f, 12);
            a.lineTo(14.6f, 18.4f);
            cv.drawPath(a, p);
        } else if ("chev-right".equals(n)) {
            Path a = new Path();
            a.moveTo(9.4f, 5.6f);
            a.lineTo(15.8f, 12);
            a.lineTo(9.4f, 18.4f);
            cv.drawPath(a, p);
        } else if ("chev-down".equals(n)) {
            Path a = new Path();
            a.moveTo(5.6f, 9.4f);
            a.lineTo(12, 15.8f);
            a.lineTo(18.4f, 9.4f);
            cv.drawPath(a, p);
        } else if ("arrow-left".equals(n)) {
            cv.drawLine(19.4f, 12, 5.4f, 12, p);
            Path a = new Path();
            a.moveTo(11.6f, 5.8f);
            a.lineTo(5.4f, 12);
            a.lineTo(11.6f, 18.2f);
            cv.drawPath(a, p);
        } else if ("play".equals(n)) {
            Path a = new Path();
            a.moveTo(8.4f, 5.2f);
            a.lineTo(19.2f, 12);
            a.lineTo(8.4f, 18.8f);
            a.close();
            cv.drawPath(a, fill);
        } else if ("bookmark".equals(n)) {
            Path a = new Path();
            a.moveTo(6.4f, 3.6f);
            a.lineTo(17.6f, 3.6f);
            a.lineTo(17.6f, 20.4f);
            a.lineTo(12, 16.2f);
            a.lineTo(6.4f, 20.4f);
            a.close();
            cv.drawPath(a, p);
        } else if ("pause".equals(n)) {
            cv.drawLine(9, 5.5f, 9, 18.5f, p);
            cv.drawLine(15, 5.5f, 15, 18.5f, p);
        } else if ("rotate".equals(n)) {
            cv.drawArc(new RectF(4, 4.6f, 19.4f, 20), -55, 275, false, p);
            Path a = new Path();
            a.moveTo(3.2f, 3.4f);
            a.lineTo(3.6f, 8.6f);
            a.lineTo(8.8f, 8.2f);
            cv.drawPath(a, p);
        } else if ("user".equals(n)) {
            cv.drawCircle(12, 8, 4.2f, p);
            cv.drawArc(new RectF(4.4f, 15, 19.6f, 28), 200, 140, false, p);
        } else if ("sparkles".equals(n)) {
            Path a = new Path();
            a.moveTo(11, 4);
            a.lineTo(12.7f, 9.3f);
            a.lineTo(18, 11);
            a.lineTo(12.7f, 12.7f);
            a.lineTo(11, 18);
            a.lineTo(9.3f, 12.7f);
            a.lineTo(4, 11);
            a.lineTo(9.3f, 9.3f);
            a.close();
            cv.drawPath(a, fill);
            cv.drawLine(18.6f, 15.5f, 18.6f, 19.5f, p);
            cv.drawLine(16.6f, 17.5f, 20.6f, 17.5f, p);
        } else if ("shield".equals(n)) {
            Path a = new Path();
            a.moveTo(12, 2.8f);
            a.lineTo(19.4f, 6);
            a.lineTo(19.4f, 11.4f);
            a.cubicTo(19.4f, 16.4f, 16.4f, 19.4f, 12, 21.2f);
            a.cubicTo(7.6f, 19.4f, 4.6f, 16.4f, 4.6f, 11.4f);
            a.lineTo(4.6f, 6);
            a.close();
            cv.drawPath(a, p);
            Path b = new Path();
            b.moveTo(9, 11.8f);
            b.lineTo(11.2f, 14);
            b.lineTo(15.2f, 9.6f);
            cv.drawPath(b, p);
        } else if ("lock".equals(n)) {
            cv.drawRoundRect(new RectF(5, 11, 19, 21), 2.4f, 2.4f, p);
            cv.drawArc(new RectF(8, 3, 16, 14), 180, 180, false, p);
        } else if ("logout".equals(n)) {
            Path a = new Path();
            a.moveTo(15, 4);
            a.lineTo(6.4f, 4);
            a.lineTo(6.4f, 20);
            a.lineTo(15, 20);
            cv.drawPath(a, p);
            cv.drawLine(10.6f, 12, 21, 12, p);
            Path b = new Path();
            b.moveTo(17.4f, 8.4f);
            b.lineTo(21, 12);
            b.lineTo(17.4f, 15.6f);
            cv.drawPath(b, p);
        } else if ("moon".equals(n)) {
            Path a = new Path();
            a.moveTo(20.4f, 13.6f);
            a.cubicTo(19.2f, 14.2f, 17.8f, 14.5f, 16.4f, 14.5f);
            a.cubicTo(11.6f, 14.5f, 7.7f, 10.6f, 7.7f, 5.8f);
            a.cubicTo(7.7f, 5, 7.8f, 4.3f, 8, 3.6f);
            a.cubicTo(5, 5, 3, 8.1f, 3, 11.6f);
            a.cubicTo(3, 16.6f, 7, 20.6f, 12, 20.6f);
            a.cubicTo(15.8f, 20.6f, 19.1f, 17.7f, 20.4f, 13.6f);
            a.close();
            cv.drawPath(a, p);
        } else if ("sun".equals(n)) {
            cv.drawCircle(12, 12, 4.2f, p);
            for (int i = 0; i < 8; i++) {
                double ang = Math.PI / 4 * i;
                float c1 = (float) Math.cos(ang), s1 = (float) Math.sin(ang);
                cv.drawLine(12 + c1 * 7f, 12 + s1 * 7f, 12 + c1 * 9.4f, 12 + s1 * 9.4f, p);
            }
        } else if ("palette".equals(n)) {
            cv.drawArc(new RectF(3, 3, 21, 21), 40, 300, false, p);
            cv.drawCircle(15.4f, 8.2f, 1.15f, fill);
            cv.drawCircle(9.4f, 7.6f, 1.15f, fill);
            cv.drawCircle(6.6f, 12.2f, 1.15f, fill);
            cv.drawCircle(16.2f, 17, 2.1f, p);
        } else if ("type".equals(n)) {
            Path a = new Path();
            a.moveTo(4.6f, 7);
            a.lineTo(4.6f, 4.2f);
            a.lineTo(19.4f, 4.2f);
            a.lineTo(19.4f, 7);
            cv.drawPath(a, p);
            cv.drawLine(12, 4.6f, 12, 19.8f, p);
            cv.drawLine(9, 19.8f, 15, 19.8f, p);
        } else if ("layers".equals(n)) {
            Path a = new Path();
            a.moveTo(12, 2.8f);
            a.lineTo(21.2f, 7.6f);
            a.lineTo(12, 12.4f);
            a.lineTo(2.8f, 7.6f);
            a.close();
            cv.drawPath(a, p);
            Path b = new Path();
            b.moveTo(2.8f, 12.2f);
            b.lineTo(12, 17);
            b.lineTo(21.2f, 12.2f);
            cv.drawPath(b, p);
            Path c2 = new Path();
            c2.moveTo(2.8f, 16.6f);
            c2.lineTo(12, 21.4f);
            c2.lineTo(21.2f, 16.6f);
            cv.drawPath(c2, p);
        } else if ("refresh".equals(n)) {
            cv.drawArc(new RectF(4.4f, 4.4f, 19.6f, 19.6f), -30, 240, false, p);
            Path a = new Path();
            a.moveTo(21.2f, 3.6f);
            a.lineTo(20.7f, 9);
            a.lineTo(15.3f, 8.5f);
            cv.drawPath(a, p);
        } else if ("download".equals(n)) {
            cv.drawLine(12, 4, 12, 14.6f, p);
            Path a = new Path();
            a.moveTo(7.4f, 10.4f);
            a.lineTo(12, 15);
            a.lineTo(16.6f, 10.4f);
            cv.drawPath(a, p);
            Path b = new Path();
            b.moveTo(4, 15.6f);
            b.lineTo(4, 19.6f);
            b.lineTo(20, 19.6f);
            b.lineTo(20, 15.6f);
            cv.drawPath(b, p);
        } else if ("info".equals(n)) {
            cv.drawCircle(12, 12, 8.6f, p);
            cv.drawLine(12, 11, 12, 16, p);
            cv.drawCircle(12, 8, 0.6f, fill);
        } else if ("calendar".equals(n)) {
            cv.drawRoundRect(new RectF(4, 5.4f, 20, 20.6f), 2.4f, 2.4f, p);
            cv.drawLine(4.4f, 9.8f, 19.6f, 9.8f, p);
            cv.drawLine(8.4f, 3.4f, 8.4f, 7, p);
            cv.drawLine(15.6f, 3.4f, 15.6f, 7, p);
        } else if ("tv".equals(n)) {
            cv.drawRoundRect(new RectF(3, 6.4f, 21, 18.6f), 2.4f, 2.4f, p);
            cv.drawLine(8.6f, 21.2f, 15.4f, 21.2f, p);
        } else if ("mic".equals(n)) {
            cv.drawRoundRect(new RectF(9.4f, 2.8f, 14.6f, 13), 2.6f, 2.6f, p);
            cv.drawArc(new RectF(6, 6.5f, 18, 18.5f), 20, 140, false, p);
            cv.drawLine(12, 18.5f, 12, 21.2f, p);
        } else if ("skip".equals(n)) {
            Path a = new Path();
            a.moveTo(5.4f, 5.4f);
            a.lineTo(14.6f, 12);
            a.lineTo(5.4f, 18.6f);
            a.close();
            cv.drawPath(a, fill);
            cv.drawLine(18.6f, 5.4f, 18.6f, 18.6f, p);
        } else if ("volume".equals(n)) {
            Path a = new Path();
            a.moveTo(4, 9.4f);
            a.lineTo(7.6f, 9.4f);
            a.lineTo(12, 5.4f);
            a.lineTo(12, 18.6f);
            a.lineTo(7.6f, 14.6f);
            a.lineTo(4, 14.6f);
            a.close();
            cv.drawPath(a, p);
            cv.drawArc(new RectF(11, 8, 20, 16), -60, 120, false, p);
        } else if ("captions".equals(n)) {
            cv.drawRoundRect(new RectF(3, 5.4f, 21, 18.6f), 2.4f, 2.4f, p);
            cv.drawLine(6.4f, 12, 9.4f, 12, p);
            cv.drawLine(11.6f, 12, 17.6f, 12, p);
            cv.drawLine(6.4f, 15.2f, 12.4f, 15.2f, p);
            cv.drawLine(14.6f, 15.2f, 17.6f, 15.2f, p);
        } else if ("users".equals(n)) {
            cv.drawCircle(9, 8.4f, 3.6f, p);
            cv.drawArc(new RectF(2.4f, 14.4f, 15.6f, 27), 200, 140, false, p);
            cv.drawArc(new RectF(13.4f, 5.2f, 19.8f, 11.6f), -90, 180, false, p);
            cv.drawArc(new RectF(15, 14.6f, 22.4f, 26), 250, 90, false, p);
        }
        cv.restore();
    }

    private Path heartPath() {
        Path a = new Path();
        a.moveTo(12, 20.2f);
        a.cubicTo(7.2f, 16.4f, 3.2f, 13.2f, 3.2f, 9.2f);
        a.cubicTo(3.2f, 6.4f, 5.4f, 4.4f, 8, 4.4f);
        a.cubicTo(9.6f, 4.4f, 11.1f, 5.2f, 12, 6.6f);
        a.cubicTo(12.9f, 5.2f, 14.4f, 4.4f, 16, 4.4f);
        a.cubicTo(18.6f, 4.4f, 20.8f, 6.4f, 20.8f, 9.2f);
        a.cubicTo(20.8f, 13.2f, 16.8f, 16.4f, 12, 20.2f);
        a.close();
        return a;
    }

    private Path starPath() {
        Path a = new Path();
        for (int i = 0; i < 5; i++) {
            double outer = -Math.PI / 2 + i * 2 * Math.PI / 5;
            double inner = outer + Math.PI / 5;
            float ox = 12 + (float) Math.cos(outer) * 9f, oy = 12 + (float) Math.sin(outer) * 9f;
            float ix = 12 + (float) Math.cos(inner) * 4.1f, iy = 12 + (float) Math.sin(inner) * 4.1f;
            if (i == 0) a.moveTo(ox, oy); else a.lineTo(ox, oy);
            a.lineTo(ix, iy);
        }
        a.close();
        return a;
    }
}
