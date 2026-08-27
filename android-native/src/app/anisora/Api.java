package app.anisora;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** AniList GraphQL client + label helpers, mirroring src/api.ts. */
public class Api {

    public interface Cb {
        void ok(JSONObject data);

        void fail(Exception e);
    }

    private static final String ENDPOINT = "https://graphql.anilist.co";
    private static final ExecutorService pool = Executors.newFixedThreadPool(3);
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Map<String, JSONObject> cache = new HashMap<String, JSONObject>();

    /** AniList OAuth bearer token (null = guest). */
    public static String token = null;

    public static void clearCache() {
        cache.clear();
    }

    public static void gql(final String query, final JSONObject variables, final Cb cb) {
        gql(query, variables, true, cb);
    }

    public static void gql(final String query, final JSONObject variables, final boolean useCache, final Cb cb) {
        final String key = query + variables.toString();
        if (useCache) {
            final JSONObject hit = cache.get(key);
            if (hit != null) {
                main.post(new Runnable() {
                    public void run() {
                        cb.ok(hit);
                    }
                });
                return;
            }
        }
        pool.execute(new Runnable() {
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("query", query);
                    body.put("variables", variables);
                    HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                    c.setConnectTimeout(12000);
                    c.setReadTimeout(20000);
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setRequestProperty("Accept", "application/json");
                    if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
                    c.setDoOutput(true);
                    OutputStream os = c.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();
                    int code = c.getResponseCode();
                    BufferedReader r = new BufferedReader(new InputStreamReader(
                            code >= 400 ? c.getErrorStream() : c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    if (code >= 400) throw new Exception("AniList " + code);
                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("errors")) throw new Exception("GraphQL error");
                    final JSONObject data = json.getJSONObject("data");
                    if (useCache) cache.put(key, data);
                    main.post(new Runnable() {
                        public void run() {
                            cb.ok(data);
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

    private static final String CARD = " id type title { romaji english native } coverImage { large medium color }"
            + " bannerImage format status season seasonYear averageScore popularity episodes chapters genres"
            + " nextAiringEpisode { episode timeUntilAiring } ";

    public static String seasonNow() {
        int m = Calendar.getInstance().get(Calendar.MONTH);
        return m <= 2 ? "WINTER" : m <= 5 ? "SPRING" : m <= 8 ? "SUMMER" : "FALL";
    }

    public static int yearNow() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static void fetchHome(String type, Cb cb) {
        String q = "query ($type: MediaType, $season: MediaSeason, $seasonYear: Int) {"
                + " trending: Page(page: 1, perPage: 14) { media(type: $type, sort: [TRENDING_DESC, POPULARITY_DESC], isAdult: false) {" + CARD + "} }"
                + " seasonal: Page(page: 1, perPage: 14) { media(type: $type, sort: [POPULARITY_DESC], isAdult: false, season: $season, seasonYear: $seasonYear) {" + CARD + "} }"
                + " top: Page(page: 1, perPage: 14) { media(type: $type, sort: [SCORE_DESC], isAdult: false) {" + CARD + "} }"
                + " loved: Page(page: 1, perPage: 14) { media(type: $type, sort: [FAVOURITES_DESC], isAdult: false) {" + CARD + "} } }";
        JSONObject v = new JSONObject();
        try {
            v.put("type", type);
            if ("ANIME".equals(type)) {
                v.put("season", seasonNow());
                v.put("seasonYear", yearNow());
            }
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    public static void search(String qstr, String type, boolean nsfw, boolean adultOnly, Cb cb) {
        String q = "query ($q: String, $type: MediaType, $nsfw: Boolean) {"
                + " Page(page: 1, perPage: 30) { media(search: $q, type: $type, isAdult: $nsfw, sort: [SEARCH_MATCH, POPULARITY_DESC]) {" + CARD + "} } }";
        JSONObject v = new JSONObject();
        try {
            v.put("q", qstr);
            if (type != null) v.put("type", type);
            if (adultOnly) v.put("nsfw", true);
            else if (!nsfw) v.put("nsfw", false);
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    /** Browse the AniList catalog (empty-query state on the Search page). */
    public static void browse(String type, boolean adultOnly, Cb cb) {
        String q = "query ($type: MediaType, $nsfw: Boolean) {"
                + " Page(page: 1, perPage: 24) { media(type: $type, isAdult: $nsfw, sort: [TRENDING_DESC, POPULARITY_DESC]) {" + CARD + "} } }";
        JSONObject v = new JSONObject();
        try {
            v.put("type", type);
            v.put("nsfw", adultOnly);
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    public static void fetchDetail(int id, Cb cb) {
        String q = "query ($id: Int) { Media(id: $id) { id type"
                + " title { romaji english native }"
                + " coverImage { extraLarge large medium color } bannerImage"
                + " description(asHtml: false)"
                + " format status season seasonYear episodes chapters volumes duration"
                + " averageScore meanScore popularity favourites source synonyms"
                + " startDate { year month day } endDate { year month day } genres"
                + " studios(isMain: true) { nodes { name } }"
                + " nextAiringEpisode { episode timeUntilAiring }"
                + " streamingEpisodes { title thumbnail site }"
                + " relations { edges { relationType(version: 2) node { id type title { romaji english native } coverImage { large color } format status averageScore } } }"
                + " characters(sort: [ROLE, RELEVANCE, ID], perPage: 12) { edges { role node { id name { full } image { large } }"
                + "   voiceActors(language: JAPANESE, sort: [RELEVANCE, ID]) { id name { full } image { medium } } } }"
                + " staff(sort: [RELEVANCE, ID], perPage: 10) { edges { role node { id name { full } image { large } } } }"
                + " recommendations(sort: [RATING_DESC, ID], perPage: 10) { nodes { rating mediaRecommendation { id type title { romaji english native } coverImage { large color } averageScore format } } }"
                + " } }";
        JSONObject v = new JSONObject();
        try {
            v.put("id", id);
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    /* -------------------------------- persons -------------------------------- */

    public static void fetchCharacter(int id, Cb cb) {
        String q = "query ($id: Int) { Character(id: $id) { id"
                + " name { full native alternative } image { large } description(asHtml: false)"
                + " gender age bloodType dateOfBirth { year month day } favourites"
                + " media(perPage: 12, sort: [POPULARITY_DESC, FAVOURITES_DESC]) {"
                + "   nodes { id type title { romaji english native } format coverImage { large medium color } averageScore } } } }";
        JSONObject v = new JSONObject();
        try {
            v.put("id", id);
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    public static void fetchStaff(int id, Cb cb) {
        String q = "query ($id: Int) { Staff(id: $id) { id"
                + " name { full native } image { large } description(asHtml: false)"
                + " gender age homeTown languageV2 dateOfBirth { year month day } favourites"
                + " staffMedia(perPage: 12, sort: [POPULARITY_DESC, FAVOURITES_DESC]) {"
                + "   edges { staffRole node { id type title { romaji english native } format coverImage { large medium color } averageScore } } } } }";
        JSONObject v = new JSONObject();
        try {
            v.put("id", id);
        } catch (Exception ignored) {
        }
        gql(q, v, cb);
    }

    /* --------------------------- authenticated AniList --------------------------- */

    public static void fetchViewer(Cb cb) {
        gql("query { Viewer { id name avatar { medium } mediaListOptions { scoreFormat } } }",
                new JSONObject(), false, cb);
    }

    public static void fetchLists(int userId, String type, Cb cb) {
        String q = "query ($userId: Int, $type: MediaType) {"
                + " MediaListCollection(userId: $userId, type: $type) { lists { name entries {"
                + "   id status progress score(format: POINT_100)"
                + "   media { id type title { romaji english native } coverImage { large color }"
                + "     episodes chapters format seasonYear status averageScore } } } } }";
        JSONObject v = new JSONObject();
        try {
            v.put("userId", userId);
            v.put("type", type);
        } catch (Exception ignored) {
        }
        gql(q, v, false, cb);
    }

    /** Create/update a list entry on AniList. scoreRaw 0-100, -1 = don't touch. */
    public static void saveEntry(int mediaId, String status, int progress, int scoreRaw, Cb cb) {
        StringBuilder q = new StringBuilder("mutation ($mediaId: Int, $status: MediaListStatus, $progress: Int");
        if (scoreRaw >= 0) q.append(", $scoreRaw: Int");
        q.append(") { SaveMediaListEntry(mediaId: $mediaId, status: $status, progress: $progress");
        if (scoreRaw >= 0) q.append(", scoreRaw: $scoreRaw");
        q.append(") { id status progress score(format: POINT_100) } }");
        JSONObject v = new JSONObject();
        try {
            v.put("mediaId", mediaId);
            v.put("status", status);
            v.put("progress", progress);
            if (scoreRaw >= 0) v.put("scoreRaw", scoreRaw);
        } catch (Exception ignored) {
        }
        gql(q.toString(), v, false, cb);
    }

    public static void deleteEntry(int listId, Cb cb) {
        JSONObject v = new JSONObject();
        try {
            v.put("id", listId);
        } catch (Exception ignored) {
        }
        gql("mutation ($id: Int) { DeleteMediaListEntry(id: $id) { deleted } }", v, false, cb);
    }

    /* -------------------------------- labels -------------------------------- */

    public static String formatLabel(String f) {
        if (f == null) return null;
        if ("TV".equals(f)) return "TV";
        if ("TV_SHORT".equals(f)) return "TV Short";
        if ("MOVIE".equals(f)) return "Movie";
        if ("SPECIAL".equals(f)) return "Special";
        if ("OVA".equals(f)) return "OVA";
        if ("ONA".equals(f)) return "ONA";
        if ("MUSIC".equals(f)) return "Music";
        if ("MANGA".equals(f)) return "Manga";
        if ("NOVEL".equals(f)) return "Light Novel";
        if ("ONE_SHOT".equals(f)) return "One Shot";
        return f;
    }

    public static String statusLabel(String s) {
        if (s == null) return null;
        if ("RELEASING".equals(s)) return "Releasing";
        if ("FINISHED".equals(s)) return "Finished";
        if ("NOT_YET_RELEASED".equals(s)) return "Not yet released";
        if ("CANCELLED".equals(s)) return "Cancelled";
        if ("HIATUS".equals(s)) return "Hiatus";
        return s;
    }

    public static String seasonLabel(String s) {
        if ("WINTER".equals(s)) return "Winter";
        if ("SPRING".equals(s)) return "Spring";
        if ("SUMMER".equals(s)) return "Summer";
        if ("FALL".equals(s)) return "Fall";
        return s;
    }

    public static String relationLabel(String r) {
        if ("SEQUEL".equals(r)) return "Sequel";
        if ("PREQUEL".equals(r)) return "Prequel";
        if ("SIDE_STORY".equals(r)) return "Side Story";
        if ("SPIN_OFF".equals(r)) return "Spin-off";
        if ("ALTERNATIVE".equals(r)) return "Alternative";
        if ("ADAPTATION".equals(r)) return "Adaptation";
        if ("SOURCE".equals(r)) return "Source";
        if ("PARENT".equals(r)) return "Parent";
        if ("SUMMARY".equals(r)) return "Summary";
        if ("CHARACTER".equals(r)) return "Shared Universe";
        return "Related";
    }

    public static String statusShort(String s) {
        if ("CURRENT".equals(s)) return "Watching";
        if ("COMPLETED".equals(s)) return "Completed";
        if ("PLANNING".equals(s)) return "Planning";
        if ("PAUSED".equals(s)) return "Paused";
        if ("DROPPED".equals(s)) return "Dropped";
        if ("REPEATING".equals(s)) return "Rewatching";
        return s;
    }

    public static int statusDot(String s) {
        if ("RELEASING".equals(s)) return Theme.GREEN;
        if ("FINISHED".equals(s)) return Theme.SKY;
        if ("NOT_YET_RELEASED".equals(s)) return Theme.AMBER;
        if ("CANCELLED".equals(s)) return Theme.ROSE;
        if ("HIATUS".equals(s)) return Theme.ORANGE;
        return Theme.MUT;
    }

    /** Title in the user's preferred language with fallbacks (titleOf in api.ts). */
    public static String titleOf(JSONObject m, String lang) {
        if (m == null) return "Untitled";
        JSONObject t = m.optJSONObject("title");
        if (t == null) return m.optString("title", "Untitled");
        String[] order;
        if ("english".equals(lang)) order = new String[]{"english", "romaji", "native"};
        else if ("native".equals(lang)) order = new String[]{"native", "romaji", "english"};
        else order = new String[]{"romaji", "english", "native"};
        for (int i = 0; i < order.length; i++) {
            String v = t.optString(order[i], null);
            if (v != null && v.length() > 0 && !"null".equals(v)) return v;
        }
        return "Untitled";
    }

    public static String fmt(long n) {
        if (n >= 1000000) {
            String s = String.valueOf(Math.round(n / 100000.0) / 10.0);
            return (s.endsWith(".0") ? s.substring(0, s.length() - 2) : s) + "M";
        }
        if (n >= 1000) {
            String s = String.valueOf(Math.round(n / 100.0) / 10.0);
            return (s.endsWith(".0") ? s.substring(0, s.length() - 2) : s) + "k";
        }
        return String.valueOf(n);
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public static String fmtDate(JSONObject d) {
        if (d == null || d.isNull("year")) return "—";
        int mo = d.optInt("month", 0);
        int day = d.optInt("day", 0);
        String s = mo >= 1 && mo <= 12 ? MONTHS[mo - 1] : "";
        if (day > 0) s = day + " " + s;
        return (s + " " + d.optInt("year")).trim();
    }

    public static String fmtCountdown(long sec) {
        long d = sec / 86400, h = (sec % 86400) / 3600, m = (sec % 3600) / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }

    public static String sourceLabel(String s) {
        if (s == null) return "—";
        String t = s.replace('_', ' ').toLowerCase();
        return t.length() > 0 ? Character.toUpperCase(t.charAt(0)) + t.substring(1) : "—";
    }
}
