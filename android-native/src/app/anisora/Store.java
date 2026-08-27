package app.anisora;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Local persistence mirroring src/store.tsx (session, settings, library). */
public class Store {

    private final SharedPreferences prefs;
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    public Store(Context ctx) {
        prefs = ctx.getSharedPreferences("anisora", Context.MODE_PRIVATE);
    }

    public SharedPreferences prefs() {
        return prefs;
    }

    public void listen(Runnable r) {
        listeners.add(r);
    }

    public void notifyChanged() {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).run();
    }

    /* ------------------------------- session ------------------------------- */

    public boolean hasSession() {
        return prefs.contains("session.name");
    }

    public String userName() {
        return prefs.getString("session.name", "traveller");
    }

    public boolean isGuest() {
        return prefs.getBoolean("session.guest", true);
    }

    public void login(String name, boolean guest) {
        prefs.edit().putString("session.name", name).putBoolean("session.guest", guest).apply();
        if (library().length() == 0) seedLibrary();
    }

    public void logout() {
        prefs.edit().remove("session.name").remove("session.guest").apply();
    }

    /* ------------------------------- settings ------------------------------ */

    public String getS(String k, String def) {
        return prefs.getString(k, def);
    }

    public boolean getB(String k, boolean def) {
        return prefs.getBoolean(k, def);
    }

    public int getI(String k, int def) {
        return prefs.getInt(k, def);
    }

    public void put(String k, String v) {
        prefs.edit().putString(k, v).apply();
    }

    public void put(String k, boolean v) {
        prefs.edit().putBoolean(k, v).apply();
    }

    public void put(String k, int v) {
        prefs.edit().putInt(k, v).apply();
    }

    /* ------------------------------- library ------------------------------- */

    public JSONObject library() {
        try {
            return new JSONObject(prefs.getString("library", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void saveLibrary(JSONObject lib) {
        prefs.edit().putString("library", lib.toString()).apply();
    }

    public JSONObject entry(int id) {
        JSONObject lib = library();
        return lib.optJSONObject(String.valueOf(id));
    }

    public void upsert(JSONObject e) {
        try {
            JSONObject lib = library();
            e.put("updatedAt", System.currentTimeMillis());
            lib.put(String.valueOf(e.getInt("id")), e);
            saveLibrary(lib);
            notifyChanged();
        } catch (Exception ignored) {
        }
    }

    /** Upsert without firing listeners (used when echoing server responses back). */
    public void upsertQuiet(JSONObject e) {
        try {
            JSONObject lib = library();
            lib.put(String.valueOf(e.getInt("id")), e);
            saveLibrary(lib);
        } catch (Exception ignored) {
        }
    }

    /** Replace the entire library (AniList sync). */
    public void replaceLibrary(JSONObject lib) {
        saveLibrary(lib);
        notifyChanged();
    }

    public void remove(int id) {
        JSONObject lib = library();
        lib.remove(String.valueOf(id));
        saveLibrary(lib);
        notifyChanged();
    }

    /** +1 progress; auto-complete & auto-start like the web store. */
    public JSONObject bump(int id) {
        try {
            JSONObject lib = library();
            JSONObject e = lib.optJSONObject(String.valueOf(id));
            if (e == null) return null;
            int next = e.optInt("progress", 0) + 1;
            int total = e.isNull("total") ? -1 : e.optInt("total", -1);
            boolean done = total > 0 && next >= total;
            e.put("progress", done ? total : next);
            String st = e.optString("status", "CURRENT");
            if (done) e.put("status", "COMPLETED");
            else if ("PLANNING".equals(st) || "PAUSED".equals(st)) e.put("status", "CURRENT");
            e.put("updatedAt", System.currentTimeMillis());
            lib.put(String.valueOf(id), e);
            saveLibrary(lib);
            notifyChanged();
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    public List<JSONObject> entriesOf(String type) {
        List<JSONObject> out = new ArrayList<JSONObject>();
        JSONObject lib = library();
        JSONArray names = lib.names();
        if (names == null) return out;
        for (int i = 0; i < names.length(); i++) {
            JSONObject e = lib.optJSONObject(names.optString(i));
            if (e != null && type.equals(e.optString("type"))) out.add(e);
        }
        Collections.sort(out, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                long d = b.optLong("updatedAt") - a.optLong("updatedAt");
                return d > 0 ? 1 : d < 0 ? -1 : 0;
            }
        });
        return out;
    }

    public int countInProgress() {
        int n = 0;
        JSONObject lib = library();
        JSONArray names = lib.names();
        if (names == null) return 0;
        for (int i = 0; i < names.length(); i++) {
            JSONObject e = lib.optJSONObject(names.optString(i));
            if (e != null && "CURRENT".equals(e.optString("status"))) n++;
        }
        return n;
    }

    /* ------------------------ seed demo library ------------------------ */

    private void seed(JSONObject lib, int id, String type, String title, String status,
                      int progress, int total, String color, int score) {
        try {
            JSONObject e = new JSONObject();
            e.put("id", id);
            e.put("type", type);
            e.put("title", title);
            e.put("status", status);
            e.put("progress", progress);
            if (total > 0) e.put("total", total);
            e.put("color", color);
            if (score > 0) e.put("score", score);
            e.put("updatedAt", System.currentTimeMillis() - (long) (Math.random() * 86400000));
            lib.put(String.valueOf(id), e);
        } catch (Exception ignored) {
        }
    }

    public void seedLibrary() {
        JSONObject lib = new JSONObject();
        seed(lib, 154587, "ANIME", "Sousou no Frieren", "CURRENT", 11, 28, "#5DA2D5", 64);
        seed(lib, 113415, "ANIME", "Jujutsu Kaisen", "CURRENT", 17, 23, "#6C5CE7", 71);
        seed(lib, 151807, "ANIME", "Solo Leveling", "PLANNING", 0, 12, "#8E44AD", 0);
        seed(lib, 16498, "ANIME", "Attack on Titan", "COMPLETED", 25, 25, "#D64550", 92);
        seed(lib, 21, "ANIME", "One Piece", "PAUSED", 112, -1, "#F4A261", 84);
        seed(lib, 30013, "MANGA", "One Piece", "CURRENT", 1098, -1, "#F4A261", 88);
        seed(lib, 2, "MANGA", "Berserk", "PAUSED", 122, 364, "#A26769", 0);
        seed(lib, 105778, "MANGA", "Chainsaw Man", "CURRENT", 132, -1, "#E85D04", 80);
        seed(lib, 656, "MANGA", "Vagabond", "PLANNING", 0, 327, "#606C38", 0);
        saveLibrary(lib);
    }
}
