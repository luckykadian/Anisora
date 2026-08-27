package app.anisora;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

/** Design tokens mirroring src/index.css (dark / amoled / light themes + accent). */
public class Theme {

    public static int BG0, BG1, BG2, LINE, TXT, MUT, ACC, ACC_SOFT, ACC_LINE, ACC_INK;
    public static boolean LIGHT;
    public static boolean REDUCE_MOTION;
    public static int RADIUS; // poster radius in dp

    public static final int GREEN = 0xFF34D399;   // emerald-400
    public static final int SKY = 0xFF38BDF8;     // sky-400
    public static final int AMBER = 0xFFFBBF24;   // amber-400
    public static final int ROSE = 0xFFFB7185;    // rose-400
    public static final int ORANGE = 0xFFFB923C;  // orange-400
    public static final int STAR = 0xFFFACC15;    // amber for score stars

    public static Typeface DISP_BOLD, DISP_MED, SANS, SANS_MED, SANS_SB, SANS_BOLD, MONO_BOLD, MONO_MED;

    /** Accent palette from src/api.ts ACCENTS ("r g b"). */
    public static final String[][] ACCENTS = {
            {"AniList Blue", "61 180 242"},
            {"Iris", "129 140 248"},
            {"Violet", "167 139 250"},
            {"Sakura", "244 114 182"},
            {"Matcha", "74 222 128"},
            {"Amber", "251 191 36"},
            {"Crimson", "248 113 113"},
            {"Mint", "45 212 191"},
    };

    public static int parseAccent(String rgb) {
        try {
            String[] p = rgb.trim().split(" ");
            return 0xFF000000 | (Integer.parseInt(p[0]) << 16) | (Integer.parseInt(p[1]) << 8) | Integer.parseInt(p[2]);
        } catch (Exception e) {
            return 0xFF3DB4F2;
        }
    }

    public static void load(Context ctx, SharedPreferences prefs) {
        String theme = prefs.getString("theme", "dark");
        String accent = prefs.getString("accent", "61 180 242");
        RADIUS = prefs.getInt("posterRadius", 16);
        REDUCE_MOTION = prefs.getBoolean("reduceMotion", false);

        ACC = parseAccent(accent);
        ACC_SOFT = (ACC & 0x00FFFFFF) | 0x24000000; // ~14%
        ACC_LINE = (ACC & 0x00FFFFFF) | 0x61000000; // ~38%

        if ("light".equals(theme)) {
            LIGHT = true;
            BG0 = 0xFFEEF1F7; BG1 = 0xFFFFFFFF; BG2 = 0xFFF0F2F8;
            LINE = 0x1A0D1421; TXT = 0xFF0D1421; MUT = 0xFF5A6478; ACC_INK = 0xFFFFFFFF;
        } else if ("amoled".equals(theme)) {
            LIGHT = false;
            BG0 = 0xFF000000; BG1 = 0xFF0A0A0C; BG2 = 0xFF131316;
            LINE = 0x1AFFFFFF; TXT = 0xFFF2F4F8; MUT = 0xFF8B94A7; ACC_INK = 0xFF06131D;
        } else {
            LIGHT = false;
            BG0 = 0xFF0A0C11; BG1 = 0xFF10141C; BG2 = 0xFF181E29;
            LINE = 0x14FFFFFF; TXT = 0xFFEDF1F7; MUT = 0xFF8B94A7; ACC_INK = 0xFF06131D;
        }

        if (DISP_BOLD == null) {
            DISP_BOLD = tf(ctx, "fonts/SpaceGrotesk-Bold.ttf");
            DISP_MED = tf(ctx, "fonts/SpaceGrotesk-Medium.ttf");
            SANS = tf(ctx, "fonts/Inter-Regular.ttf");
            SANS_MED = tf(ctx, "fonts/Inter-Medium.ttf");
            SANS_SB = tf(ctx, "fonts/Inter-SemiBold.ttf");
            SANS_BOLD = tf(ctx, "fonts/Inter-Bold.ttf");
            MONO_BOLD = tf(ctx, "fonts/JetBrainsMono-Bold.ttf");
            MONO_MED = tf(ctx, "fonts/JetBrainsMono-Medium.ttf");
        }
    }

    private static Typeface tf(Context ctx, String path) {
        try {
            return Typeface.createFromAsset(ctx.getAssets(), path);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public static int alpha(int color, int a255) {
        return (color & 0x00FFFFFF) | (a255 << 24);
    }
}
