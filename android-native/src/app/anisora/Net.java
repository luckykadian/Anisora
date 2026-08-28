package app.anisora;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Plain HTTP helpers for extension repos (GET text + file download). */
public class Net {

    public interface Text {
        void ok(String body);

        void fail(Exception e);
    }

    public interface FileCb {
        void ok(File f);

        void fail(Exception e);
    }

    private static final ExecutorService pool = Executors.newFixedThreadPool(3);
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static void getText(final String url, final Text cb) {
        pool.execute(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setInstanceFollowRedirects(true);
                    c.setRequestProperty("User-Agent", "Anisora/0.6 (Android)");
                    int code = c.getResponseCode();
                    if (code >= 400) throw new Exception("HTTP " + code);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[8192];
                    int n;
                    while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
                    r.close();
                    final String body = sb.toString();
                    main.post(new Runnable() {
                        public void run() {
                            cb.ok(body);
                        }
                    });
                } catch (final Exception e) {
                    main.post(new Runnable() {
                        public void run() {
                            cb.fail(e);
                        }
                    });
                }
            }
        });
    }

    public static void download(final String url, final File out, final FileCb cb) {
        pool.execute(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(60000);
                    c.setInstanceFollowRedirects(true);
                    c.setRequestProperty("User-Agent", "Anisora/0.6 (Android)");
                    int code = c.getResponseCode();
                    if (code >= 400) throw new Exception("HTTP " + code);
                    InputStream in = c.getInputStream();
                    File tmp = new File(out.getAbsolutePath() + ".tmp");
                    tmp.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(tmp);
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                    os.close();
                    in.close();
                    // sanity: an APK is a zip — check PK magic before accepting
                    java.io.FileInputStream fi = new java.io.FileInputStream(tmp);
                    byte[] magic = new byte[2];
                    int got = fi.read(magic);
                    fi.close();
                    if (got != 2 || magic[0] != 'P' || magic[1] != 'K') {
                        tmp.delete();
                        throw new Exception("not an APK (bad magic)");
                    }
                    tmp.renameTo(out);
                    main.post(new Runnable() {
                        public void run() {
                            cb.ok(out);
                        }
                    });
                } catch (final Exception e) {
                    main.post(new Runnable() {
                        public void run() {
                            cb.fail(e);
                        }
                    });
                }
            }
        });
    }
}
