package app.anisora;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anisora — native replica of the web UI (src/App.tsx shell):
 * header with search, four routes, detail overlay stack, toasts.
 */
public class MainActivity extends Activity {

    public Store store;
    public String route = "anime";
    public String searchQuery = "";
    public String searchTab = "ALL";
    public String settingsSection = "appearance";

    private final Map<String, String> filters = new HashMap<String, String>();
    private FrameLayout root;        // whole window
    private LinearLayout shell;      // header + content + nav
    private FrameLayout content;     // route container
    private FrameLayout overlays;    // detail stack + sheets
    private LinearLayout toasts;     // toast stack
    private EditText searchInput;
    private FrameLayout headerBtn;
    private Icons headerIcon;
    private View headerDot;
    private final List<int[]> detailStack = new ArrayList<int[]>();
    private final Handler handler = new Handler();
    private Runnable searchDebounce;
    /** Set while the Extensions screen is open; run on resume to reflect installer results. */
    public Runnable extScreenRefresh;

    protected void onResume() {
        super.onResume();
        try { ExtBridge.reload(); } catch (Throwable ignored) {}
        if (extScreenRefresh != null) extScreenRefresh.run();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        Images.init(this);
        store = new Store(this);
        Theme.load(this, store.prefs());
        Anilist.restore(this);
        try { ExtBridge.reload(); } catch (Throwable ignored) {}

        root = new FrameLayout(this);
        setContentView(root);
        rebuild();
        handleAuthIntent(getIntent());
    }

    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        handleAuthIntent(intent);
    }

    /** anisora://anilist-auth#access_token=… (AniList OAuth redirect). */
    private void handleAuthIntent(android.content.Intent intent) {
        if (intent == null || intent.getData() == null) return;
        android.net.Uri uri = intent.getData();
        if (!"anisora".equalsIgnoreCase(uri.getScheme())) return;
        String token = Anilist.tokenFromRedirect(uri);
        if (token != null && token.length() > 0) {
            Anilist.completeLogin(this, token);
        } else {
            toast("AniList didn't return a token — try again", "info");
        }
    }

    public void startAniListLogin() {
        if (!Anilist.startLogin(this)) toast("No browser available for AniList login", "info");
        else toast("Authorize Anisora in the browser…", "info");
    }

    public String getFilter(String type) {
        String f = filters.get(type);
        return f == null ? "ALL" : f;
    }

    public void setFilter(String type, String f) {
        filters.put(type, f);
    }

    public FrameLayout overlayRoot() {
        return overlays;
    }

    /* ------------------------------ full rebuild ------------------------------ */

    public void applyTheme() {
        Theme.load(this, store.prefs());
        rebuild();
    }

    public void rebuild() {
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(Theme.BG0);
        win.setNavigationBarColor(Theme.BG0);
        if (Theme.LIGHT) {
            // SYSTEM_UI_FLAG_LIGHT_STATUS_BAR (API 23 constant; no-op below 23)
            win.getDecorView().setSystemUiVisibility(0x00002000);
        } else {
            win.getDecorView().setSystemUiVisibility(0);
        }

        root.removeAllViews();
        root.setBackgroundColor(Theme.BG0);
        detailStack.clear();

        if (!store.hasSession()) {
            root.addView(OnboardingScreen.build(this, this),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            overlays = new FrameLayout(this);
            root.addView(overlays, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            toasts = buildToastHolder();
            root.addView(toasts);
            return;
        }

        shell = Ui.col(this);
        shell.setBackgroundColor(Theme.BG0);

        shell.addView(buildHeader());

        content = new FrameLayout(this);
        LinearLayout.LayoutParams cp = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        cp.weight = 1;
        shell.addView(content, cp);

        shell.addView(buildBottomNav());

        root.addView(shell, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlays = new FrameLayout(this);
        root.addView(overlays, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        toasts = buildToastHolder();
        root.addView(toasts);

        rebuildContent();
    }

    private LinearLayout buildToastHolder() {
        LinearLayout t = Ui.col(this);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.BOTTOM;
        tp.setMargins(Ui.dp(16), 0, Ui.dp(16), Ui.dp(84));
        t.setLayoutParams(tp);
        return t;
    }

    /* --------------------------------- header --------------------------------- */

    private View buildHeader() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Theme.alpha(Theme.BG0, 245));
        bar.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10));

        bar.addView(OnboardingScreen.logo(this, 26, false));
        bar.addView(Ui.hspace(this, 10));

        // search input
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1));
        Icons si = new Icons(this, "search", 14, Theme.MUT);
        FrameLayout.LayoutParams sip = new FrameLayout.LayoutParams(Ui.dp(14), Ui.dp(14));
        sip.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        sip.leftMargin = Ui.dp(12);
        box.addView(si, sip);

        searchInput = new EditText(this);
        searchInput.setText(searchQuery);
        searchInput.setHint("Search anime, manga…");
        searchInput.setHintTextColor(Theme.alpha(Theme.MUT, 160));
        searchInput.setTextColor(Theme.TXT);
        searchInput.setTextSize(13);
        searchInput.setTypeface(Theme.SANS_MED);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(Ui.dp(34), Ui.dp(9), Ui.dp(12), Ui.dp(9));
        searchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c2) {
            }

            public void afterTextChanged(Editable s) {
                final String q = s.toString();
                if (q.equals(searchQuery)) return;
                searchQuery = q;
                if (!"search".equals(route)) {
                    route = "search";
                    refreshNav();
                }
                if (searchDebounce != null) handler.removeCallbacks(searchDebounce);
                searchDebounce = new Runnable() {
                    public void run() {
                        if ("search".equals(route)) rebuildContent();
                    }
                };
                handler.postDelayed(searchDebounce, 420);
            }
        });
        box.addView(searchInput, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams bp = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.weight = 1;
        bar.addView(box, bp);

        bar.addView(Ui.hspace(this, 10));

        // action button: bell normally, catalog-sort on the Search route
        headerBtn = new FrameLayout(this);
        headerBtn.setBackground(Ui.ripple(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1), Theme.alpha(Theme.TXT, 26)));
        headerIcon = new Icons(this, "bell", 15, Theme.MUT);
        FrameLayout.LayoutParams bip = new FrameLayout.LayoutParams(Ui.dp(15), Ui.dp(15));
        bip.gravity = Gravity.CENTER;
        headerBtn.addView(headerIcon, bip);
        headerDot = new View(this);
        headerDot.setBackground(Ui.circle(Theme.ACC));
        FrameLayout.LayoutParams dp2 = new FrameLayout.LayoutParams(Ui.dp(6), Ui.dp(6));
        dp2.gravity = Gravity.TOP | Gravity.RIGHT;
        dp2.setMargins(0, Ui.dp(9), Ui.dp(9), 0);
        headerBtn.addView(headerDot, dp2);
        headerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ("search".equals(route)) SearchScreen.showCatalogSortSheet(MainActivity.this);
                else toast("You're all caught up — no new notifications", "info");
            }
        });
        updateHeaderAction();
        bar.addView(headerBtn, Ui.lp(Ui.dp(38), Ui.dp(38)));

        LinearLayout wrap = Ui.col(this);
        wrap.addView(bar);
        wrap.addView(Ui.divider(this));
        return wrap;
    }

    /* -------------------------------- bottom nav ------------------------------- */

    private LinearLayout navBar;

    private View buildBottomNav() {
        LinearLayout wrap = Ui.col(this);
        wrap.addView(Ui.divider(this));
        navBar = Ui.row(this);
        navBar.setBackgroundColor(Theme.alpha(Theme.BG0, 250));
        navBar.setPadding(0, Ui.dp(6), 0, Ui.dp(8));
        refreshNavInto(navBar);
        wrap.addView(navBar);
        return wrap;
    }

    private void refreshNav() {
        if (navBar != null) {
            navBar.removeAllViews();
            refreshNavInto(navBar);
        }
    }

    private void refreshNavInto(LinearLayout bar) {
        String[][] items = {{"anime", "Anime", "film"}, {"manga", "Manga", "book"},
                {"search", "Search", "compass"}, {"settings", "Settings", "settings"}};
        for (int i = 0; i < items.length; i++) {
            final String id = items[i][0];
            boolean active = id.equals(route);
            LinearLayout item = Ui.col(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(4), 0, Ui.dp(2));

            View pill = new View(this);
            pill.setBackground(Ui.rounded(active ? Theme.ACC : 0x00000000, 2, 0, 0));
            item.addView(pill, Ui.lp(Ui.dp(28), Ui.dp(3)));
            item.addView(Ui.space(this, 5));

            item.addView(new Icons(this, items[i][2], 19, active ? Theme.ACC : Theme.MUT), Ui.lp(Ui.dp(19), Ui.dp(19)));
            TextView label = Ui.text(this, items[i][1].toUpperCase(), 8.5f, active ? Theme.ACC : Theme.MUT, Theme.SANS_BOLD);
            label.setLetterSpacing(0.08f);
            label.setPadding(0, Ui.dp(4), 0, 0);
            label.setGravity(Gravity.CENTER_HORIZONTAL); // labels default to full width — keep text centered under the icon
            item.addView(label, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!id.equals(route)) {
                        route = id;
                        refreshNav();
                        rebuildContent();
                    }
                    if ("search".equals(id)) {
                        searchInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(searchInput, 0);
                    }
                }
            });
            LinearLayout.LayoutParams ip = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            ip.weight = 1;
            bar.addView(item, ip);
        }
    }

    /* ------------------------------ route content ------------------------------ */

    /** Header action reflects the route: sort popup on Search, bell elsewhere. */
    private void updateHeaderAction() {
        if (headerIcon == null) return;
        boolean search = "search".equals(route);
        headerIcon.setIcon(search ? "sort" : "bell");
        if (headerDot != null) headerDot.setVisibility(search ? View.GONE : View.VISIBLE);
    }

    public void rebuildContent() {
        if (content == null) return;
        content.removeAllViews();
        View v;
        if ("anime".equals(route)) v = HomeScreen.build(this, this, "ANIME");
        else if ("manga".equals(route)) v = HomeScreen.build(this, this, "MANGA");
        else if ("search".equals(route)) v = SearchScreen.build(this, this, searchQuery);
        else v = SettingsScreen.build(this, this);
        content.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Ui.appear(v, 0); // route transition like AnimatePresence on the web
        refreshNav();
        updateHeaderAction();
    }

    /* ------------------------------ detail overlay ----------------------------- */

    public void openDetail(int id, JSONObject seed) {
        View page = DetailScreen.build(this, this, id, seed);
        overlays.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        page.setTranslationY(Ui.dp(26));
        page.setAlpha(0f);
        page.animate().translationY(0).alpha(1f).setDuration(220).start();
        detailStack.add(new int[]{id});
    }

    public void closeDetail() {
        int n = overlays.getChildCount();
        if (n > 0) {
            final View top = overlays.getChildAt(n - 1);
            top.animate().translationY(Ui.dp(20)).alpha(0f).setDuration(160).withEndAction(new Runnable() {
                public void run() {
                    overlays.removeView(top);
                }
            }).start();
        }
        if (!detailStack.isEmpty()) detailStack.remove(detailStack.size() - 1);
        // refresh underlying content so tracked changes show up
        if (overlays.getChildCount() <= 1) rebuildContent();
    }

    public void onBackPressed() {
        if (overlays != null && overlays.getChildCount() > 0) {
            closeDetail();
            return;
        }
        if (!"anime".equals(route)) {
            route = "anime";
            refreshNav();
            rebuildContent();
            return;
        }
        super.onBackPressed();
    }

    /** Open a character/staff page (PersonOverlay on the web). */
    public void openPerson(String kind, int id, String name, String image, String role) {
        PersonScreen.open(this, this, kind, id, name, image, role);
    }

    /* --------------------------------- oauth demo -------------------------------- */

    public void showOauthDialog() {
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.rounded(Theme.BG1, 22, Theme.LINE, 1));
        Widgets.clipRounded(card, 22);

        // browser chrome bar
        LinearLayout chrome = Ui.row(this);
        chrome.setBackgroundColor(Theme.BG2);
        chrome.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        int[] dots = {0xCCFB7185, 0xCCFBBF24, 0xCC34D399};
        for (int i = 0; i < 3; i++) {
            View d = new View(this);
            d.setBackground(Ui.circle(dots[i]));
            chrome.addView(d, Ui.lpm(Ui.dp(9), Ui.dp(9), i == 0 ? 0 : 5, 0, 0, 0));
        }
        chrome.addView(Ui.hspace(this, 10));
        LinearLayout url = Ui.row(this);
        url.setBackground(Ui.rounded(Theme.BG0, 8, Theme.LINE, 1));
        url.setPadding(Ui.dp(9), Ui.dp(4), Ui.dp(9), Ui.dp(4));
        url.addView(new Icons(this, "lock", 9, Theme.GREEN), Ui.lp(Ui.dp(9), Ui.dp(9)));
        url.addView(Ui.hspace(this, 6));
        TextView ut = Ui.oneLine(Ui.text(this, "anilist.co/api/v2/oauth/authorize", 9.5f, Theme.MUT, Theme.MONO_MED));
        url.addView(ut);
        LinearLayout.LayoutParams up = Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        up.weight = 1;
        chrome.addView(url, up);
        card.addView(chrome);

        final LinearLayout body = Ui.col(this);
        body.setPadding(Ui.dp(20), Ui.dp(20), Ui.dp(20), Ui.dp(20));
        card.addView(body);

        // form step
        LinearLayout hd = Ui.row(this);
        TextView a = Ui.text(this, "A", 18, 0xFFFFFFFF, Theme.SANS_BOLD);
        a.setGravity(Gravity.CENTER);
        a.setBackground(Ui.rounded(0xFF02A9FF, 14, 0, 0));
        hd.addView(a, Ui.lpm(Ui.dp(40), Ui.dp(40), 0, 0, 10, 0));
        LinearLayout hc = Ui.col(this);
        hc.addView(Ui.text(this, "AniList", 16, Theme.TXT, Theme.DISP_BOLD));
        hc.addView(Ui.text(this, "wants to connect with Anisora", 11, Theme.MUT, Theme.SANS));
        hd.addView(hc);
        body.addView(hd);
        body.addView(Ui.space(this, 16));

        body.addView(Ui.text(this, "USERNAME", 9.5f, Theme.MUT, Theme.MONO_BOLD));
        body.addView(Ui.space(this, 5));
        TextView un = Ui.text(this, "nova", 13.5f, Theme.TXT, Theme.SANS_MED);
        un.setBackground(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1));
        un.setPadding(Ui.dp(13), Ui.dp(10), Ui.dp(13), Ui.dp(10));
        body.addView(un, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(Ui.space(this, 10));
        body.addView(Ui.text(this, "PASSWORD", 9.5f, Theme.MUT, Theme.MONO_BOLD));
        body.addView(Ui.space(this, 5));
        TextView pw = Ui.text(this, "••••••••••", 13.5f, Theme.TXT, Theme.SANS_MED);
        pw.setBackground(Ui.rounded(Theme.BG2, 12, Theme.LINE, 1));
        pw.setPadding(Ui.dp(13), Ui.dp(10), Ui.dp(13), Ui.dp(10));
        body.addView(pw, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(Ui.space(this, 16));

        final LinearLayout auth = Ui.row(this);
        auth.setGravity(Gravity.CENTER);
        auth.setBackground(Ui.ripple(Ui.rounded(0xFF02A9FF, 12, 0, 0), 0x33000000));
        auth.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
        auth.addView(Ui.text(this, "Authorize Anisora", 13.5f, 0xFFFFFFFF, Theme.SANS_BOLD));
        auth.addView(Ui.hspace(this, 6));
        auth.addView(new Icons(this, "chev-right", 14, 0xFFFFFFFF), Ui.lp(Ui.dp(14), Ui.dp(14)));
        body.addView(auth, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(Ui.space(this, 10));
        TextView legal = Ui.text(this, "Read & write access to your lists. Revoke anytime at anilist.co", 10, Theme.MUT, Theme.SANS);
        legal.setGravity(Gravity.CENTER);
        body.addView(legal, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        auth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                body.removeAllViews();
                LinearLayout mid = Ui.col(MainActivity.this);
                mid.setGravity(Gravity.CENTER);
                mid.setPadding(0, Ui.dp(26), 0, Ui.dp(26));
                mid.addView(new Icons(MainActivity.this, "check", 40, Theme.GREEN, 3f), Ui.lp(Ui.dp(40), Ui.dp(40)));
                TextView t1 = Ui.text(MainActivity.this, "Connected as Nova", 14, Theme.TXT, Theme.SANS_SB);
                t1.setPadding(0, Ui.dp(12), 0, 0);
                mid.addView(t1);
                TextView t2 = Ui.text(MainActivity.this, "Pulling your lists into orbit…", 11, Theme.MUT, Theme.SANS);
                t2.setPadding(0, Ui.dp(4), 0, 0);
                mid.addView(t2);
                body.addView(mid, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                handler.postDelayed(new Runnable() {
                    public void run() {
                        store.login("Nova", false);
                        rebuild();
                        toast("Connected to AniList as Nova", "check");
                    }
                }, 900);
            }
        });

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER;
        cp.setMargins(Ui.dp(22), 0, Ui.dp(22), 0);
        overlay.addView(card, cp);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                root.removeView(overlay);
            }
        });
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /* ---------------------------------- toasts --------------------------------- */

    public void toast(String msg, String icon) {
        if (toasts == null) return;
        final LinearLayout t = Ui.row(this);
        t.setBackground(Ui.rounded(Theme.BG1, 14, Theme.LINE, 1));
        t.setElevation(Ui.dp(8));
        t.setPadding(Ui.dp(13), Ui.dp(11), Ui.dp(15), Ui.dp(11));
        String ic = "check".equals(icon) ? "check" : "sync".equals(icon) ? "refresh"
                : "trash".equals(icon) ? "x" : "play".equals(icon) ? "play" : "info";
        int color = "check".equals(icon) ? Theme.GREEN : "trash".equals(icon) ? Theme.ROSE : Theme.ACC;
        t.addView(new Icons(this, ic, 14, color), Ui.lp(Ui.dp(14), Ui.dp(14)));
        t.addView(Ui.hspace(this, 9));
        TextView m = Ui.text(this, msg, 12.5f, Theme.TXT, Theme.SANS_SB);
        t.addView(m);

        toasts.addView(t, Ui.lpm(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 8, 0, 0));
        t.setAlpha(0f);
        t.setTranslationY(Ui.dp(14));
        t.animate().alpha(1f).translationY(0).setDuration(200).start();
        handler.postDelayed(new Runnable() {
            public void run() {
                t.animate().alpha(0f).translationY(Ui.dp(10)).setDuration(220).withEndAction(new Runnable() {
                    public void run() {
                        toasts.removeView(t);
                    }
                }).start();
            }
        }, 2800);
        while (toasts.getChildCount() > 4) toasts.removeViewAt(0);
    }
}
