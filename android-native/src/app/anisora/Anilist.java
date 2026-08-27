package app.anisora;

import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Real AniList integration: OAuth (implicit grant) + library sync + mutations.
 *
 *   client id     49241
 *   redirect URL  anisora://anilist-auth
 */
public class Anilist {

    public static final String CLIENT_ID = "49241";
    public static final String AUTH_URL =
            "https://anilist.co/api/v2/oauth/authorize?client_id=" + CLIENT_ID + "&response_type=token";

    /** Launch the AniList authorize page in the browser. */
    public static boolean startLogin(MainActivity app) {
        try {
            app.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Extract #access_token=… from the anisora://anilist-auth redirect. */
    public static String tokenFromRedirect(Uri uri) {
        if (uri == null) return null;
        String frag = uri.getFragment();
        if (frag == null) frag = uri.getEncodedQuery(); // some browsers convert # to ?
        if (frag == null) return null;
        String[] parts = frag.split("&");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("access_token=")) return parts[i].substring("access_token=".length());
        }
        return null;
    }

    /** After receiving a token: identify the viewer, then pull both libraries. */
    public static void completeLogin(final MainActivity app, String token) {
        Api.token = token;
        app.store.put("anilist.token", token);
        app.toast("Connecting to AniList…", "sync");
        Api.fetchViewer(new Api.Cb() {
            public void ok(JSONObject d) {
                JSONObject viewer = d.optJSONObject("Viewer");
                if (viewer == null) {
                    fail(new Exception("no viewer"));
                    return;
                }
                int userId = viewer.optInt("id");
                String name = viewer.optString("name", "AniList user");
                app.store.put("anilist.userId", userId);
                app.store.login(name, false);
                app.toast("Connected as " + name + " — syncing library…", "check");
                syncLibrary(app, userId);
            }

            public void fail(Exception e) {
                Api.token = null;
                app.store.put("anilist.token", "");
                app.toast("AniList login failed — try again", "info");
            }
        });
    }

    /** Restore a persisted session on app start. */
    public static void restore(MainActivity app) {
        String t = app.store.getS("anilist.token", "");
        if (t != null && t.length() > 0) Api.token = t;
    }

    public static boolean authed() {
        return Api.token != null;
    }

    /* ------------------------------ library sync ------------------------------ */

    public static void syncLibrary(final MainActivity app, final int userId) {
        final JSONObject lib = new JSONObject();
        Api.fetchLists(userId, "ANIME", new Api.Cb() {
            public void ok(JSONObject d) {
                mergeLists(lib, d);
                Api.fetchLists(userId, "MANGA", new Api.Cb() {
                    public void ok(JSONObject d2) {
                        mergeLists(lib, d2);
                        app.store.replaceLibrary(lib);
                        app.rebuild();
                        app.toast("Library synced — " + lib.length() + " titles from AniList", "check");
                    }

                    public void fail(Exception e) {
                        app.store.replaceLibrary(lib);
                        app.rebuild();
                        app.toast("Synced anime list (manga failed)", "info");
                    }
                });
            }

            public void fail(Exception e) {
                app.toast("Library sync failed — pull down later from Settings", "info");
                app.rebuild();
            }
        });
    }

    private static void mergeLists(JSONObject lib, JSONObject data) {
        try {
            JSONObject coll = data.optJSONObject("MediaListCollection");
            if (coll == null) return;
            JSONArray lists = coll.optJSONArray("lists");
            if (lists == null) return;
            for (int i = 0; i < lists.length(); i++) {
                JSONObject list = lists.optJSONObject(i);
                if (list == null) continue;
                JSONArray entries = list.optJSONArray("entries");
                if (entries == null) continue;
                for (int j = 0; j < entries.length(); j++) {
                    JSONObject le = entries.optJSONObject(j);
                    if (le == null) continue;
                    JSONObject media = le.optJSONObject("media");
                    if (media == null) continue;
                    JSONObject e = new JSONObject();
                    e.put("id", media.optInt("id"));
                    e.put("listId", le.optInt("id"));
                    e.put("type", media.optString("type"));
                    e.put("title", Api.titleOf(media, "romaji"));
                    JSONObject cov = media.optJSONObject("coverImage");
                    if (cov != null) {
                        e.put("cover", cov.optString("large", null));
                        e.put("color", cov.optString("color", null));
                    }
                    e.put("status", le.optString("status", "PLANNING"));
                    e.put("progress", le.optInt("progress", 0));
                    int total = "MANGA".equals(media.optString("type"))
                            ? media.optInt("chapters", -1) : media.optInt("episodes", -1);
                    if (total > 0) e.put("total", total);
                    int score = (int) Math.round(le.optDouble("score", 0));
                    if (score > 0) e.put("score", score);
                    e.put("updatedAt", System.currentTimeMillis());
                    lib.put(String.valueOf(media.optInt("id")), e);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /* ------------------------------- mutations ------------------------------- */

    /** Push a local entry's status/progress/score to AniList (no-op for guests). */
    public static void push(final MainActivity app, final JSONObject entry) {
        if (!authed() || entry == null) return;
        int scoreRaw = entry.has("score") ? entry.optInt("score", -1) : -1;
        Api.saveEntry(entry.optInt("id"), entry.optString("status", "PLANNING"),
                entry.optInt("progress", 0), scoreRaw, new Api.Cb() {
                    public void ok(JSONObject d) {
                        JSONObject saved = d.optJSONObject("SaveMediaListEntry");
                        if (saved != null) {
                            try {
                                entry.put("listId", saved.optInt("id"));
                                app.store.upsertQuiet(entry);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    public void fail(Exception e) {
                        app.toast("AniList sync failed — change kept locally", "info");
                    }
                });
    }

    /** Delete the entry on AniList (needs the listId captured during sync/push). */
    public static void delete(final MainActivity app, JSONObject entry) {
        if (!authed() || entry == null) return;
        int listId = entry.optInt("listId", -1);
        if (listId <= 0) return;
        Api.deleteEntry(listId, new Api.Cb() {
            public void ok(JSONObject d) {
            }

            public void fail(Exception e) {
                app.toast("AniList delete failed — removed locally", "info");
            }
        });
    }

    public static void logout(MainActivity app) {
        Api.token = null;
        app.store.put("anilist.token", "");
        app.store.put("anilist.userId", 0);
    }
}
