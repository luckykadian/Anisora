package app.anisora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny async image loader: memory LRU + disk cache + fade-in. */
public class Images {

    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static LruCache<String, Bitmap> mem;
    private static File dir;

    public static void init(Context ctx) {
        if (mem != null) return;
        int kb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 6);
        mem = new LruCache<String, Bitmap>(kb) {
            protected int sizeOf(String k, Bitmap b) {
                return b.getByteCount() / 1024;
            }
        };
        dir = new File(ctx.getCacheDir(), "img");
        dir.mkdirs();
    }

    public static void load(final String url, final ImageView into, final int reqWidth) {
        if (url == null || url.length() == 0) return;
        Bitmap hit = mem.get(url);
        if (hit != null) {
            into.setImageBitmap(hit);
            return;
        }
        into.setTag(url);
        pool.execute(new Runnable() {
            public void run() {
                try {
                    File f = new File(dir, md5(url));
                    if (!f.exists()) download(url, f);
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                    int sample = 1;
                    while (o.outWidth / (sample * 2) >= reqWidth) sample *= 2;
                    o = new BitmapFactory.Options();
                    o.inSampleSize = sample;
                    final Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                    if (b == null) return;
                    mem.put(url, b);
                    main.post(new Runnable() {
                        public void run() {
                            if (url.equals(into.getTag())) {
                                into.setAlpha(0f);
                                into.setImageBitmap(b);
                                into.animate().alpha(1f).setDuration(220).start();
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static void download(String url, File out) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        InputStream in = c.getInputStream();
        File tmp = new File(out.getAbsolutePath() + ".tmp");
        OutputStream os = new FileOutputStream(tmp);
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        os.close();
        in.close();
        tmp.renameTo(out);
    }

    private static String md5(String s) {
        try {
            MessageDigest d = MessageDigest.getInstance("MD5");
            byte[] b = d.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.length; i++) {
                String h = Integer.toHexString(b[i] & 0xFF);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
