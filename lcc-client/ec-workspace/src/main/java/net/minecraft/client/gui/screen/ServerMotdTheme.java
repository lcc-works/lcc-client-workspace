package net.minecraft.client.gui.screen;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.lax1dude.eaglercraft.internal.buffer.ByteBuffer;

/**
 * Optional per-server visual theme, requested by the server itself through a
 * hidden marker field on the MOTD chat component.
 *
 * <p>The marker is an {@code eagler_theme} JSON object attached to the MOTD.
 * Vanilla clients parse chat components with a closed schema and ignore unknown
 * keys, so the marker is invisible to every client that does not implement this.
 * See {@code make_theme/} for the generator.</p>
 */
public class ServerMotdTheme {

    /** Bumped if the on-the-wire schema changes. */
    public static final int VERSION = 1;

    private static final int DEFAULT_ACCENT = 0x4CAF50;
    private static final int DEFAULT_ACCENT_DARK = 0x2E7D32;
    private static final int DEFAULT_BG = 0x101014;

    public final int accent;
    public final int accentDark;
    public final int bg;
    public final boolean gradient;
    /** Gradient direction in degrees, 0 = left-to-right, 90 = top-to-bottom. */
    public final int angle;
    public final int radius;
    public final boolean showLogo;
    public final boolean showMotd;

    /** Base64 PNG exactly as the vanilla favicon field carries it, or null. */
    private final String logoData;

    private ServerMotdTheme(String logoData, int accent, int accentDark, int bg,
                            boolean gradient, int angle, int radius,
                            boolean showLogo, boolean showMotd) {
        this.logoData = logoData;
        this.accent = accent;
        this.accentDark = accentDark;
        this.bg = bg;
        this.gradient = gradient;
        // Keep it in [0, 360) so the cache key stays small.
        this.angle = ((angle % 360) + 360) % 360;
        this.radius = Math.max(0, Math.min(16, radius));
        this.showLogo = showLogo;
        this.showMotd = showMotd;
    }

    /**
     * Identity of the rendered gradient, so a cached texture can be reused
     * while the theme is unchanged.
     */
    public int getGradientKey() {
        if (!this.gradient) {
            return 0;
        }
        int h = 0;
        h = h * 31 + this.accentDark;
        h = h * 31 + this.bg;
        h = h * 31 + this.angle;
        return h == 0 ? 1 : h;
    }

    public String getLogoData() {
        return this.logoData;
    }

    /**
     * Extracts the theme from a MOTD string, if the server sent one.
     *
     * <p>The MOTD may be plain text (no theme) or a JSON chat component. Only
     * the object form can carry a marker, so plain text returns null.</p>
     *
     * <p>A MOTD is not always a single JSON object: the query response can hold
     * up to two MOTD lines, which {@code ServerData.setMOTDFromQuery} joins with
     * a {@code \n}. Feeding that joined string to a JSON parser fails, so each
     * line is tried on its own as well.</p>
     *
     * @return the parsed theme, or null if this server has no theme
     */
    public static ServerMotdTheme parse(String motd) {
        if (motd == null) {
            return null;
        }

        // Whole string first: covers the common single-object case.
        ServerMotdTheme theme = parseOne(motd);
        if (theme != null) {
            return theme;
        }

        // Then line by line, for MOTDs the query joined together.
        int start = 0;
        while (start <= motd.length()) {
            int nl = motd.indexOf('\n', start);
            String line = nl < 0 ? motd.substring(start) : motd.substring(start, nl);
            theme = parseOne(line);
            if (theme != null) {
                return theme;
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }

        return null;
    }

    private static ServerMotdTheme parseOne(String motd) {
        String trimmed = motd.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{') {
            return null;
        }

        try {
            JsonElement root = new JsonParser().parse(trimmed);
            if (!root.isJsonObject()) {
                return null;
            }

            JsonObject obj = root.getAsJsonObject();
            JsonElement marker = obj.get("eagler_theme");
            if (marker == null || !marker.isJsonObject()) {
                return null;
            }

            JsonObject t = marker.getAsJsonObject();

            // Reject a marker we do not understand rather than guessing.
            if (!t.has("v") || t.get("v").getAsInt() != VERSION) {
                return null;
            }

            String logo = null;
            if (t.has("logo") && t.get("logo").isJsonPrimitive()) {
                logo = t.get("logo").getAsString();
                if (logo.length() > 262144) {
                    // Guard against a hostile server stuffing a huge blob.
                    logo = null;
                }
            }

            return new ServerMotdTheme(
                    logo,
                    optColor(t, "accent", DEFAULT_ACCENT),
                    optColor(t, "accentDark", DEFAULT_ACCENT_DARK),
                    optColor(t, "bg", DEFAULT_BG),
                    optBool(t, "gradient", true),
                    optInt(t, "angle", 100),
                    t.has("radius") ? t.get("radius").getAsInt() : 6,
                    optBool(t, "showLogo", true),
                    optBool(t, "showMotd", true)
            );
        } catch (Throwable t) {
            // A malformed marker must never break the server list.
            return null;
        }
    }

    private static int optColor(JsonObject o, String key, int fallback) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try {
                int v = o.get(key).getAsInt() & 0xFFFFFF;
                if (v != 0) {
                    return v;
                }
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private static int optInt(JsonObject o, String key, int fallback) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try {
                return o.get(key).getAsInt();
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private static boolean optBool(JsonObject o, String key, boolean fallback) {
        if (o.has(key) && o.get(key).isJsonPrimitive()) {
            try {
                return o.get(key).getAsBoolean();
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }
}