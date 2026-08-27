package app.anisora;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Anisora demo shell.
 *
 * Renders the untouched Anisora web UI (single-file Vite build shipped in
 * assets/www) inside a fullscreen WebView. Assets are served from a virtual
 * https origin so the app gets a secure context, working localStorage
 * persistence and clean CORS against the AniList GraphQL API.
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.anisora.app";
    private static final String START_URL = "https://" + HOST + "/index.html";
    private static final int BG = 0xFF0A0C11; // matches the UI's --bg0

    private WebView web;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(BG);
        win.setNavigationBarColor(BG);

        web = new WebView(this);
        web.setBackgroundColor(BG);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebViewClient(new WebViewClient() {
            // API >= 21 path
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return serveAsset(request.getUrl());
            }

            // legacy path
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return serveAsset(Uri.parse(url));
            }

            // keep the SPA inside the app, push external links to the browser
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri u = Uri.parse(url);
                if (u != null && HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        setContentView(web);

        if (savedInstanceState == null) {
            web.loadUrl(START_URL);
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    private WebResourceResponse serveAsset(Uri uri) {
        if (uri == null || !HOST.equals(uri.getHost())) return null; // let network requests through
        String path = uri.getPath();
        if (path == null || path.length() == 0 || "/".equals(path)) path = "/index.html";
        String asset = "www" + path;
        try {
            InputStream in = getAssets().open(asset);
            return new WebResourceResponse(mimeFor(asset), "utf-8", in);
        } catch (Exception e) {
            return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream(new byte[0]));
        }
    }

    private static String mimeFor(String name) {
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".woff")) return "font/woff";
        return "application/octet-stream";
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
